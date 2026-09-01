package com.juan.btparking;

import java.util.ArrayList;
import java.util.List;

public final class DisconnectFlowSimulationTest {
    private static final long NOW = 2_000_000_000_000L;

    public static void main(String[] args) {
        Simulator simulator = new Simulator();
        simulator.connectionChanged(true, "Auto de Juan", "AA:BB:CC:DD:EE:01", NOW - 1_000L);
        simulator.connectionChanged(false, "Auto de Juan", "AA:BB:CC:DD:EE:01", NOW);
        simulator.connectionChanged(false, "AA:BB:CC:DD:EE:02", "AA:BB:CC:DD:EE:02", NOW);
        simulator.connectionChanged(false, null, "AA:BB:CC:DD:EE:03", NOW);

        expect(1, simulator.saved.size());
        expect("Auto de Juan", simulator.saved.get(0).name);
        expect(true, HistoryFilter.matches(simulator.saved.get(0).name, NOW, "juan", 1, NOW));
        expect(false, HistoryFilter.matches(simulator.saved.get(0).name, NOW, "auriculares", 1, NOW));
        expect(1, simulator.notifications);
    }

    private static final class Simulator {
        final List<SavedEvent> saved = new ArrayList<>();
        int notifications;

        void connectionChanged(boolean connected, String name, String address, long time) {
            if (connected || !DeviceNameFilter.hasBluetoothName(name, address)) return;
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
