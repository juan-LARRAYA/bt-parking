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

    public static void save(Context ctx, String address, String name, double lat, double lon, long time) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor e = prefs.edit();
        Set<String> set = new HashSet<>(prefs.getStringSet(KEY_DEVICE_SET, new HashSet<String>()));
        set.add(address);
        e.putStringSet(KEY_DEVICE_SET, set);
        e.putString("name_" + address, name);
        e.putFloat("lat_" + address, (float) lat);
        e.putFloat("lon_" + address, (float) lon);
        e.putLong("time_" + address, time);
        e.apply();
    }

    public static List<Entry> loadAll(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        Set<String> set = prefs.getStringSet(KEY_DEVICE_SET, new HashSet<String>());
        List<Entry> list = new ArrayList<>();
        for (String addr : set) {
            if (!prefs.contains("time_" + addr)) continue;
            Entry en = new Entry();
            en.address = addr;
            en.name = prefs.getString("name_" + addr, addr);
            en.lat = prefs.getFloat("lat_" + addr, 0f);
            en.lon = prefs.getFloat("lon_" + addr, 0f);
            en.timeMillis = prefs.getLong("time_" + addr, 0L);
            list.add(en);
        }
        return list;
    }
}
