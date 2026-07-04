package com.juan.btparking;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

public class BluetoothDisconnectReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "bt_parking_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        if (device == null) return;

        String address;
        String name;
        try {
            address = device.getAddress();
        } catch (SecurityException se) {
            return;
        }
        try {
            name = device.getName();
        } catch (SecurityException se) {
            name = address;
        }
        if (name == null) name = address;
        final String finalName = name;
        final String finalAddress = address;

        final PendingResult pending = goAsync();
        final Context appContext = context.getApplicationContext();
        final LocationManager lm = (LocationManager) appContext.getSystemService(Context.LOCATION_SERVICE);
        final Handler mainHandler = new Handler(Looper.getMainLooper());

        Location best = getBestLastKnown(lm, appContext);
        if (best != null && System.currentTimeMillis() - best.getTime() < 5 * 60 * 1000) {
            finish(appContext, pending, finalAddress, finalName, best);
            return;
        }

        final boolean[] done = {false};
        final LocationListener listener = new LocationListener() {
            @Override public void onLocationChanged(Location location) {
                if (done[0]) return;
                done[0] = true;
                lm.removeUpdates(this);
                finish(appContext, pending, finalAddress, finalName, location);
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
                Location fallback = getBestLastKnown(lm, appContext);
                if (fallback != null) {
                    finish(appContext, pending, finalAddress, finalName, fallback);
                } else {
                    pending.finish();
                }
            }
        }, 20000);
    }

    private Location getBestLastKnown(LocationManager lm, Context ctx) {
        Location best = null;
        try {
            for (String provider : new String[]{LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER}) {
                if (lm.getProvider(provider) == null) continue;
                Location loc = lm.getLastKnownLocation(provider);
                if (loc != null && (best == null || loc.getTime() > best.getTime())) {
                    best = loc;
                }
            }
        } catch (SecurityException se) {
            return null;
        }
        return best;
    }

    private void finish(Context ctx, PendingResult pending, String address, String name, Location loc) {
        LocationStore.save(ctx, address, name, loc.getLatitude(), loc.getLongitude(), System.currentTimeMillis());
        notify(ctx, name, loc);
        pending.finish();
    }

    private void notify(Context ctx, String name, Location loc) {
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "BT Parking", NotificationManager.IMPORTANCE_DEFAULT);
            nm.createNotificationChannel(channel);
        }
        Intent openIntent = new Intent(ctx, MainActivity.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, openIntent, flags);
        android.app.Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new android.app.Notification.Builder(ctx, CHANNEL_ID)
                : new android.app.Notification.Builder(ctx);
        android.app.Notification n = builder
                .setContentTitle("Ubicacion guardada")
                .setContentText(name + " -> " + loc.getLatitude() + "," + loc.getLongitude())
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build();
        try {
            nm.notify(name.hashCode(), n);
        } catch (SecurityException ignored) { }
    }
}
