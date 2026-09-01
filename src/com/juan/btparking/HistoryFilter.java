package com.juan.btparking;

final class HistoryFilter {
    private HistoryFilter() { }

    static boolean matches(String deviceName, long timeMillis, String nameQuery, int recentDays, long nowMillis) {
        String query = nameQuery == null ? "" : nameQuery.trim().toLowerCase(java.util.Locale.ROOT);
        String name = deviceName == null ? "" : deviceName.toLowerCase(java.util.Locale.ROOT);
        if (!query.isEmpty() && !name.contains(query)) return false;
        if (recentDays <= 0) return true;
        long earliest = nowMillis - recentDays * 24L * 60L * 60L * 1000L;
        return timeMillis >= earliest && timeMillis <= nowMillis;
    }
}
