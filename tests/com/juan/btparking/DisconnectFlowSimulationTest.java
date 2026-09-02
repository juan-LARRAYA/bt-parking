package com.juan.btparking;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DisconnectFlowSimulationTest {
    private static final long NOW = 2_000_000_000_000L;

    public static void main(String[] args) {
        Simulator simulator = new Simulator();
        simulator.connected("Auto de Juan", "AA:BB:CC:DD:EE:01");
        simulator.connected("Stereo", "AA:BB:CC:DD:EE:02");
        simulator.connected("AA:BB:CC:DD:EE:03", "AA:BB:CC:DD:EE:03");
        simulator.bluetoothTurningOff(NOW);
        simulator.disconnected("Auto de Juan", "AA:BB:CC:DD:EE:01", NOW + 100L);

        expect(2, simulator.saved.size());
        expect("Auto de Juan", simulator.saved.get(0).name);
        expect("Stereo", simulator.saved.get(1).name);
        expect(true, HistoryFilter.matches(simulator.saved.get(0).name, NOW, "juan", 1, NOW));
        expect(false, HistoryFilter.matches(simulator.saved.get(0).name, NOW, "auriculares", 1, NOW));
        expect(1, simulator.notifications);
    }

    private static final class Simulator {
        final List<SavedEvent> saved = new ArrayList<>();
        final Map<String, String> connected = new LinkedHashMap<>();
        int notifications;
        long shutdownAt;

        void connected(String name, String address) {
            if (DeviceNameFilter.hasBluetoothName(name, address)) connected.put(address, name);
        }

        void bluetoothTurningOff(long time) {
            shutdownAt = time;
            for (Map.Entry<String, String> device : connected.entrySet()) {
                saved.add(new SavedEvent(device.getValue(), time));
            }
            if (!connected.isEmpty()) notifications++;
            connected.clear();
        }

        void disconnected(String name, String address, long time) {
            connected.remove(address);
            if (time - shutdownAt < 30_000L || !DeviceNameFilter.hasBluetoothName(name, address)) return;
            saved.add(new SavedEvent(name, time));
            notifications++;
        }
    }

    private static final class SavedEvent {
        final String name;
        final long time;
        SavedEvent(String name, long time) { this.name = name; this.time = time; }
    }

    private static void expect(Object expected, Object actual) {
        if (!expected.equals(actual)) throw new AssertionError("Expected " + expected + " but got " + actual);
    }
}
