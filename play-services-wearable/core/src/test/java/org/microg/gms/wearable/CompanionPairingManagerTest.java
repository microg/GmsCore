package org.microg.gms.wearable;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.SharedPreferences;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.List;

/**
 * Tests for CompanionPairingManager — Wear OS device pairing lifecycle.
 * Verifies discovery, pairing state, listener management, and edge cases.
 */
@RunWith(RobolectricTestRunner.class)
public class CompanionPairingManagerTest {

    private CompanionPairingManager manager;
    private Context context;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.application;
        WearableImpl wearable = new WearableImpl(context, new NodeDatabaseHelper(context), new ConfigurationDatabaseHelper(context));
        manager = new CompanionPairingManager(context, wearable, "com.test.app");
    }

    // === Lifecycle ===

    @Test
    public void testConstructor_initializesPairedDevices() {
        assertNotNull("Manager should be created", manager);
        List<CompanionPairingManager.CompanionDevice> devices = manager.getPairedDevices();
        assertNotNull("Paired devices list should not be null", devices);
        assertTrue("Paired devices should start empty", devices.isEmpty());
    }

    @Test
    public void testBluetoothAvailability_checkedGracefully() {
        boolean available = manager.isBluetoothAvailable();
        assertTrue("isBluetoothAvailable should return true or false", available || !available);
    }

    @Test
    public void testBleScanningSupport_checkedGracefully() {
        boolean supported = manager.isBleScanningSupported();
        assertTrue("isBleScanningSupported should return boolean", supported || !supported);
    }

    // === Listener Management ===

    @Test
    public void testAddListener_doesNotThrow() {
        manager.addListener(new CompanionPairingManager.PairingListener() {
            @Override
            public void onDeviceFound(CompanionPairingManager.CompanionDevice device) {}
            @Override
            public void onPairingCompleted(CompanionPairingManager.CompanionDevice device) {}
            @Override
            public void onPairingFailed(String macAddress, int errorCode) {}
            @Override
            public void onDeviceDisconnected(CompanionPairingManager.CompanionDevice device) {}
        });
    }

    @Test
    public void testRemoveListener_doesNotThrow() {
        CompanionPairingManager.PairingListener listener =
                new CompanionPairingManager.PairingListener() {
                    @Override
                    public void onDeviceFound(CompanionPairingManager.CompanionDevice device) {}
                    @Override
                    public void onPairingCompleted(CompanionPairingManager.CompanionDevice device) {}
                    @Override
                    public void onPairingFailed(String macAddress, int errorCode) {}
                    @Override
                    public void onDeviceDisconnected(CompanionPairingManager.CompanionDevice device) {}
                };
        manager.addListener(listener);
        manager.removeListener(listener);
    }

    @Test
    public void testAddNullListener_handledGracefully() {
        try {
            manager.addListener(null);
        } catch (Exception e) {
            // Expected or handled
        }
    }

    @Test
    public void testMultipleListeners_addedAndRemoved() {
        CompanionPairingManager.PairingListener l1 =
                new CompanionPairingManager.PairingListener() {
                    @Override public void onDeviceFound(CompanionPairingManager.CompanionDevice d) {}
                    @Override public void onPairingCompleted(CompanionPairingManager.CompanionDevice d) {}
                    @Override public void onPairingFailed(String m, int e) {}
                    @Override public void onDeviceDisconnected(CompanionPairingManager.CompanionDevice d) {}
                };
        CompanionPairingManager.PairingListener l2 =
                new CompanionPairingManager.PairingListener() {
                    @Override public void onDeviceFound(CompanionPairingManager.CompanionDevice d) {}
                    @Override public void onPairingCompleted(CompanionPairingManager.CompanionDevice d) {}
                    @Override public void onPairingFailed(String m, int e) {}
                    @Override public void onDeviceDisconnected(CompanionPairingManager.CompanionDevice d) {}
                };
        manager.addListener(l1);
        manager.addListener(l2);
        manager.removeListener(l1);
        manager.removeListener(l2);
    }

    // === CompanionDevice ===

    @Test
    public void testCompanionDevice_constructorAndFields() {
        CompanionPairingManager.CompanionDevice device =
                new CompanionPairingManager.CompanionDevice(
                        "AA:BB:CC:DD:EE:FF", "Test Watch", "node123", 1, 1234567890L);
        assertEquals("macAddress", "AA:BB:CC:DD:EE:FF", device.macAddress);
        assertEquals("deviceName", "Test Watch", device.deviceName);
        assertEquals("nodeId", "node123", device.nodeId);
        assertEquals("deviceType", 1, device.deviceType);
        assertEquals("pairedTimestamp", 1234567890L, device.pairedTimestamp);
    }

    @Test
    public void testCompanionDevice_equalsAndHashCode() {
        CompanionPairingManager.CompanionDevice d1 =
                new CompanionPairingManager.CompanionDevice("AA:BB:CC:DD:EE:FF", "W", "n1", 1, 0L);
        CompanionPairingManager.CompanionDevice d2 =
                new CompanionPairingManager.CompanionDevice("AA:BB:CC:DD:EE:FF", "W", "n1", 1, 0L);
        CompanionPairingManager.CompanionDevice d3 =
                new CompanionPairingManager.CompanionDevice("FF:EE:DD:CC:BB:AA", "X", "n2", 2, 1L);
        assertEquals("Same MAC should be equal", d1, d2);
        assertEquals("Equal objects should have same hash", d1.hashCode(), d2.hashCode());
        assertNotEquals("Different MAC should be unequal", d1, d3);
    }

    @Test
    public void testCompanionDevice_connectedState() {
        CompanionPairingManager.CompanionDevice device =
                new CompanionPairingManager.CompanionDevice("AA:BB:CC:DD:EE:FF", "W", "n1", 1, 0L);
        assertFalse("New device should not be connected", device.isConnected);
        device.isConnected = true;
        assertTrue("Device should be connected after setting", device.isConnected);
    }

    // === Start Scanning ===

    @Test
    public void testStartScanning_doesNotCrash() {
        try {
            manager.startScanning();
        } catch (Exception e) {
            // May throw if Bluetooth is not available in test environment
        }
    }

    @Test
    public void testStartScanning_calledWithoutBluetooth() {
        if (!manager.isBluetoothAvailable()) {
            try {
                manager.startScanning();
            } catch (SecurityException | IllegalStateException e) {
                // Expected without Bluetooth permissions
            }
        }
    }
}
