package com.juan.btparking;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LocationStore {

    public static class Entry {
        public String address;
        public String name;
        public double lat;
        public double lon;
        public long timeMillis;
    }

    private static final String PREFS = "bt_parking_prefs";
    private static final String KEY_DEVICE_SET = "device_set";
    private static final String KEY_EVENT_SET = "event_set_v2";
    private static final String KEY_MIGRATED = "history_migrated_v2";

    public static synchronized void save(Context ctx, String address, String name, double lat, double lon, long time) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        migrateLegacy(prefs);
        SharedPreferences.Editor e = prefs.edit();
        Set<String> events = new HashSet<>(prefs.getStringSet(KEY_EVENT_SET, new HashSet<String>()));
        String id = time + "_" + Integer.toHexString(address.hashCode());
        int suffix = 1;
        while (events.contains(id)) id = time + "_" + Integer.toHexString(address.hashCode()) + "_" + suffix++;
        events.add(id);
        e.putStringSet(KEY_EVENT_SET, events);
        putEntry(e, id, address, name, lat, lon, time);
        e.apply();
    }

    public static synchronized List<Entry> loadAll(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        migrateLegacy(prefs);
        Set<String> set = prefs.getStringSet(KEY_EVENT_SET, new HashSet<String>());
        List<Entry> list = new ArrayList<>();
        for (String id : set) {
            if (!prefs.contains("time_event_" + id)) continue;
            Entry en = new Entry();
            en.address = prefs.getString("address_event_" + id, "");
            en.name = prefs.getString("name_event_" + id, en.address);
            en.lat = prefs.getFloat("lat_event_" + id, 0f);
            en.lon = prefs.getFloat("lon_event_" + id, 0f);
            en.timeMillis = prefs.getLong("time_event_" + id, 0L);
            list.add(en);
        }
        return list;
    }

    private static void migrateLegacy(SharedPreferences prefs) {
        if (prefs.getBoolean(KEY_MIGRATED, false)) return;
        Set<String> events = new HashSet<>(prefs.getStringSet(KEY_EVENT_SET, new HashSet<String>()));
        SharedPreferences.Editor editor = prefs.edit();
        for (String address : prefs.getStringSet(KEY_DEVICE_SET, new HashSet<String>())) {
            if (!prefs.contains("time_" + address)) continue;
            long time = prefs.getLong("time_" + address, 0L);
            String id = "legacy_" + time + "_" + Integer.toHexString(address.hashCode());
            events.add(id);
            putEntry(editor, id, address, prefs.getString("name_" + address, address),
                    prefs.getFloat("lat_" + address, 0f), prefs.getFloat("lon_" + address, 0f), time);
        }
        editor.putStringSet(KEY_EVENT_SET, events);
        editor.putBoolean(KEY_MIGRATED, true);
        editor.commit();
    }

    private static void putEntry(SharedPreferences.Editor editor, String id, String address,
                                 String name, double lat, double lon, long time) {
        editor.putString("address_event_" + id, address);
        editor.putString("name_event_" + id, name);
        editor.putFloat("lat_event_" + id, (float) lat);
        editor.putFloat("lon_event_" + id, (float) lon);
        editor.putLong("time_event_" + id, time);
    }
}
