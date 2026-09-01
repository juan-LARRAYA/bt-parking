package com.juan.btparking;

public final class DeviceNameFilterTest {
    public static void main(String[] args) {
        expect(false, DeviceNameFilter.hasBluetoothName(null, "AA:BB:CC:DD:EE:FF"));
        expect(false, DeviceNameFilter.hasBluetoothName("  ", "AA:BB:CC:DD:EE:FF"));
        expect(false, DeviceNameFilter.hasBluetoothName("AA:BB:CC:DD:EE:FF", "AA:BB:CC:DD:EE:FF"));
        expect(false, DeviceNameFilter.hasBluetoothName("aa-bb-cc-dd-ee-ff", "11:22:33:44:55:66"));
        expect(true, DeviceNameFilter.hasBluetoothName("Auto de Juan", "AA:BB:CC:DD:EE:FF"));
    }

    private static void expect(boolean expected, boolean actual) {
        if (expected != actual) {
            throw new AssertionError("Expected " + expected + " but got " + actual);
        }
    }
}
