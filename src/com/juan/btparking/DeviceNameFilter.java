package com.juan.btparking;

final class DeviceNameFilter {
    private DeviceNameFilter() { }

    static boolean hasBluetoothName(String name, String address) {
        if (name == null) return false;
        String candidate = name.trim();
        if (candidate.length() == 0) return false;
        if (address != null && candidate.equalsIgnoreCase(address.trim())) return false;
        return !candidate.matches("(?i)^[0-9a-f]{2}([:-][0-9a-f]{2}){5}$");
    }
}
