/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.gms.wearable;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ApplicationInfo;
import android.test.AndroidTestCase;

import com.google.android.gms.wearable.ConnectionConfiguration;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Uses only the test application's databases; no TCP or Bluetooth socket is opened. */
public class WearableEmulatorTransportPolicyTest extends AndroidTestCase {
    private Context releaseContext;
    private NodeDatabaseHelper nodes;
    private ConfigurationDatabaseHelper configurations;
    private WearableImpl wearable;

    @Override protected void setUp() throws Exception {
        super.setUp();
        assertEquals("Tests must only use the dedicated test application's storage",
                "org.microg.gms.wearable.core.test", getContext().getPackageName());
        getContext().deleteDatabase("node.db");
        getContext().deleteDatabase("connectionconfig.db");
        releaseContext = new ContextWrapper(getContext()) {
            @Override public ApplicationInfo getApplicationInfo() {
                ApplicationInfo info = new ApplicationInfo(super.getApplicationInfo());
                info.flags &= ~ApplicationInfo.FLAG_DEBUGGABLE;
                return info;
            }
        };
        nodes = new NodeDatabaseHelper(releaseContext);
        configurations = new ConfigurationDatabaseHelper(releaseContext);
    }

    @Override protected void tearDown() throws Exception {
        try {
            if (wearable != null) {
                Thread network = wearable.networkHandler.getLooper().getThread();
                wearable.stop();
                network.join(3000);
                assertFalse("The test must release its network thread", network.isAlive());
            }
        } finally {
            if (configurations != null) configurations.close();
            if (nodes != null) nodes.close();
            if (configurations != null) getContext().deleteDatabase("connectionconfig.db");
            if (nodes != null) getContext().deleteDatabase("node.db");
            super.tearDown();
        }
    }

    private void start() throws Exception {
        wearable = new WearableImpl(releaseContext, nodes, configurations);
        CountDownLatch restored = new CountDownLatch(1);
        assertTrue(wearable.networkHandler.post(restored::countDown));
        assertTrue(restored.await(3, TimeUnit.SECONDS));
    }

    private ConnectionConfiguration tcp(boolean enabled) {
        return new ConnectionConfiguration("test-server", null, 2, 2, enabled, "local-node");
    }

    private void rejected(Runnable action) {
        try { action.run(); fail("A non-debuggable application must not use TCP"); }
        catch (SecurityException expected) { }
    }

    private void noListener() throws Exception {
        Field socketThread = WearableImpl.class.getDeclaredField("sct");
        socketThread.setAccessible(true);
        assertNull("No TCP thread may be created", socketThread.get(wearable));
    }

    public void testPutRejectsBeforePersistingEnabledOrDisabledTcp() throws Exception {
        start();
        rejected(() -> wearable.createConnection(tcp(true)));
        rejected(() -> wearable.createConnection(tcp(false)));
        assertEquals(0, configurations.getAllConfigurations().length);
        noListener();
    }

    public void testUpdateRejectsBeforeChangingExistingConfiguration() throws Exception {
        start();
        // This direct database write represents configuration from an older installation.
        configurations.putManagedConfiguration(tcp(false), "original-node");
        rejected(() -> wearable.updateConnection(tcp(true)));
        ConnectionConfiguration stored = configurations.getConfiguration("test-server");
        assertFalse(stored.enabled);
        assertEquals("original-node", stored.nodeId);
        noListener();
    }

    public void testEnableRejectsBeforeChangingEnabledState() throws Exception {
        start();
        configurations.putManagedConfiguration(tcp(false), "original-node");
        rejected(() -> wearable.enableConnection("test-server"));
        assertFalse(configurations.getConfiguration("test-server").enabled);
        noListener();
    }

    public void testRestoreCannotActivatePreviouslyEnabledTcp() throws Exception {
        configurations.putManagedConfiguration(tcp(true), "original-node");
        start();
        noListener();
        // Keep the stored preference for a later permitted environment; never claim connected.
        ConnectionConfiguration[] runtime = wearable.getConfigurations();
        assertEquals(1, runtime.length);
        assertFalse(runtime[0].connected);
    }

    public void testBluetoothConfigurationRemainsAvailableWithoutDebuggableFlag() throws Exception {
        start();
        wearable.createConnection(new ConnectionConfiguration("test-watch", "AA:BB:CC:DD:EE:01", 1, 1, false));
        ConnectionConfiguration stored = configurations.getConfiguration("test-watch");
        assertNotNull(stored);
        assertEquals(1, stored.type);
        assertFalse(stored.enabled);
        noListener();
    }
}
