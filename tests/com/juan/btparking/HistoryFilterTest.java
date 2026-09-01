package com.juan.btparking;

public final class HistoryFilterTest {
    private static final long DAY = 24L * 60L * 60L * 1000L;
    private static final long NOW = 2_000_000_000_000L;

    public static void main(String[] args) {
        expect(true, HistoryFilter.matches("Auto de Juan", NOW - DAY, "auto", 7, NOW));
        expect(true, HistoryFilter.matches("Auto de Juan", NOW - DAY, "JUAN", 7, NOW));
        expect(false, HistoryFilter.matches("Auriculares", NOW - DAY, "auto", 7, NOW));
        expect(false, HistoryFilter.matches("Auto de Juan", NOW - 8 * DAY, "", 7, NOW));
        expect(true, HistoryFilter.matches("Auto de Juan", NOW - 40 * DAY, "", 0, NOW));
    }

    private static void expect(boolean expected, boolean actual) {
        if (expected != actual) throw new AssertionError("Expected " + expected + " but got " + actual);
    }
}
