package com.juan.btparking;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class ConnectedDeviceStore {
    static final class Device {
        final String address;
        final String name;

        Device(String address, String name) {
            this.address = address;
            this.name = name;
        }
    }

    private static final String PREFS = "bt_parking_connected";
    private static final String KEY_ADDRESSES = "addresses";
    private static final String KEY_SHUTDOWN_AT = "shutdown_at";

    private ConnectedDeviceStore() { }

    static synchronized void connected(Context context, String address, String name) {
        SharedPreferences prefs = prefs(context);
        Set<String> addresses = new HashSet<>(prefs.getStringSet(KEY_ADDRESSES, new HashSet<String>()));
        addresses.add(address);
        prefs.edit().putStringSet(KEY_ADDRESSES, addresses).putString("name_" + address, name).apply();
    }

    static synchronized void disconnected(Context context, String address) {
        SharedPreferences prefs = prefs(context);
        Set<String> addresses = new HashSet<>(prefs.getStringSet(KEY_ADDRESSES, new HashSet<String>()));
        addresses.remove(address);
        prefs.edit().putStringSet(KEY_ADDRESSES, addresses).remove("name_" + address).apply();
    }

    static synchronized List<Device> snapshotForShutdown(Context context, long now) {
        SharedPreferences prefs = prefs(context);
        Set<String> addresses = new HashSet<>(prefs.getStringSet(KEY_ADDRESSES, new HashSet<String>()));
        List<Device> devices = new ArrayList<>();
        SharedPreferences.Editor editor = prefs.edit().putLong(KEY_SHUTDOWN_AT, now);
        for (String address : addresses) {
            String name = prefs.getString("name_" + address, null);
            if (DeviceNameFilter.hasBluetoothName(name, address)) devices.add(new Device(address, name.trim()));
            editor.remove("name_" + address);
        }
        editor.putStringSet(KEY_ADDRESSES, new HashSet<String>()).apply();
        return devices;
    }

    static boolean isShutdownDisconnect(Context context, long now) {
        long shutdownAt = prefs(context).getLong(KEY_SHUTDOWN_AT, 0L);
        return shutdownAt > 0L && now - shutdownAt >= 0L && now - shutdownAt < 30_000L;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
