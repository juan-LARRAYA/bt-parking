package com.juan.btparking;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BluetoothDisconnectReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "bt_parking_saved_locations_v2";
    private static final int NOTIFICATION_ID = 7401;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            if (state == BluetoothAdapter.STATE_TURNING_OFF) handleBluetoothTurningOff(context);
            return;
        }

        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        if (device == null) return;
        ConnectedDeviceStore.Device tracked = readDevice(device);
        if (tracked == null) return;

        if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
            ConnectedDeviceStore.connected(context, tracked.address, tracked.name);
            return;
        }
        if (!BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) return;

        ConnectedDeviceStore.disconnected(context, tracked.address);
        if (ConnectedDeviceStore.isShutdownDisconnect(context, System.currentTimeMillis())) return;
        List<ConnectedDeviceStore.Device> devices = new ArrayList<>();
        devices.add(tracked);
        saveWithBestLocation(context, devices);
    }

    private void handleBluetoothTurningOff(Context context) {
        List<ConnectedDeviceStore.Device> devices = ConnectedDeviceStore.snapshotForShutdown(
                context, System.currentTimeMillis());
        if (!devices.isEmpty()) saveWithBestLocation(context, devices);
    }

    private ConnectedDeviceStore.Device readDevice(BluetoothDevice device) {
        try {
            String address = device.getAddress();
            String name = device.getName();
            if (!DeviceNameFilter.hasBluetoothName(name, address)) return null;
            return new ConnectedDeviceStore.Device(address, name.trim());
        } catch (SecurityException se) {
            return null;
        }
    }

    private void saveWithBestLocation(Context context, final List<ConnectedDeviceStore.Device> devices) {
        final PendingResult pending = goAsync();
        final Context appContext = context.getApplicationContext();
        final LocationManager lm = (LocationManager) appContext.getSystemService(Context.LOCATION_SERVICE);
        final Handler mainHandler = new Handler(Looper.getMainLooper());

        Location best = getBestLastKnown(lm);
        if (best != null && System.currentTimeMillis() - best.getTime() < 5 * 60 * 1000) {
            finish(appContext, pending, devices, best);
            return;
        }

        final boolean[] done = {false};
        final LocationListener listener = new LocationListener() {
            @Override public void onLocationChanged(Location location) {
                if (done[0]) return;
                done[0] = true;
                lm.removeUpdates(this);
                finish(appContext, pending, devices, location);
            }
            @Override public void onStatusChanged(String p, int s, Bundle b) { }
            @Override public void onProviderEnabled(String p) { }
            @Override public void onProviderDisabled(String p) { }
        };

        try {
            if (lm.getProvider(LocationManager.GPS_PROVIDER) != null) {
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, listener, Looper.getMainLooper());
            }
            if (lm.getProvider(LocationManager.NETWORK_PROVIDER) != null) {
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, listener, Looper.getMainLooper());
            }
        } catch (SecurityException se) {
            pending.finish();
            return;
        }

        mainHandler.postDelayed(new Runnable() {
            @Override public void run() {
                if (done[0]) return;
                done[0] = true;
                lm.removeUpdates(listener);
                Location fallback = getBestLastKnown(lm);
                if (fallback != null) finish(appContext, pending, devices, fallback);
                else pending.finish();
            }
        }, 20_000L);
    }

    private Location getBestLastKnown(LocationManager lm) {
        Location best = null;
        try {
            for (String provider : new String[]{LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER}) {
                if (lm.getProvider(provider) == null) continue;
                Location loc = lm.getLastKnownLocation(provider);
                if (loc != null && (best == null || loc.getTime() > best.getTime())) best = loc;
            }
        } catch (SecurityException se) {
            return null;
        }
        return best;
    }

    private void finish(Context context, PendingResult pending, List<ConnectedDeviceStore.Device> devices, Location location) {
        long now = System.currentTimeMillis();
        for (ConnectedDeviceStore.Device device : devices) {
            LocationStore.save(context, device.address, device.name,
                    location.getLatitude(), location.getLongitude(), now);
        }
        notifySaved(context, devices, location, now);
        pending.finish();
    }

    private void notifySaved(Context context, List<ConnectedDeviceStore.Device> devices, Location location, long time) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Ubicaciones guardadas", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Avisos visuales cuando BT Parking guarda una ubicacion");
            channel.enableLights(true);
            channel.setLightColor(Color.parseColor("#76D275"));
            manager.createNotificationChannel(channel);
        }

        Intent openIntent = new Intent(context, MainActivity.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT
                | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);
        PendingIntent openApp = PendingIntent.getActivity(context, 0, openIntent, flags);

        StringBuilder names = new StringBuilder();
        for (ConnectedDeviceStore.Device device : devices) {
            if (names.length() > 0) names.append(", ");
            names.append(device.name);
        }
        String timeText = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(time));
        String title = devices.size() == 1 ? "Estacionamiento guardado" : devices.size() + " ubicaciones guardadas";
        String detail = names + "\n" + String.format(Locale.getDefault(), "%.5f, %.5f · %s",
                location.getLatitude(), location.getLongitude(), timeText);

        android.app.Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new android.app.Notification.Builder(context, CHANNEL_ID)
                : new android.app.Notification.Builder(context);
        android.app.Notification notification = builder
                .setContentTitle(title)
                .setContentText(names.toString())
                .setSubText("BT Parking · " + timeText)
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher))
                .setColor(Color.parseColor("#2E7D32"))
                .setStyle(new android.app.Notification.BigTextStyle().bigText(detail))
                .setContentIntent(openApp)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .build();
        try {
            manager.notify(NOTIFICATION_ID, notification);
        } catch (SecurityException ignored) { }
    }
}
