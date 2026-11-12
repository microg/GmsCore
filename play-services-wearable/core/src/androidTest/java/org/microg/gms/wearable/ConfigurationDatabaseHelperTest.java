/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.gms.wearable;

import android.database.Cursor;
import android.test.AndroidTestCase;

import com.google.android.gms.wearable.ConnectionConfiguration;

import java.util.Collections;

/** Real SQLite writes, including identity preservation and updates that must not create links. */
public class ConfigurationDatabaseHelperTest extends AndroidTestCase {
    private ConfigurationDatabaseHelper helper;

    @Override protected void setUp() throws Exception {
        super.setUp();
        assertEquals("Tests must only use the dedicated test application's storage",
                "org.microg.gms.wearable.core.test", getContext().getPackageName());
        getContext().deleteDatabase("connectionconfig.db");
        helper = new ConfigurationDatabaseHelper(getContext());
    }

    @Override protected void tearDown() throws Exception {
        if (helper != null) {
            helper.close();
            getContext().deleteDatabase("connectionconfig.db");
        }
        super.tearDown();
    }

    private ConnectionConfiguration bluetooth(String name, String address, boolean enabled) {
        return new ConnectionConfiguration(name, address, 1, 1, enabled, "untrusted-node");
    }

    private ConnectionConfiguration server(boolean enabled) {
        return new ConnectionConfiguration(null, null, 2, 2, enabled, "untrusted-node");
    }

    private long rowId(String name) {
        try (Cursor cursor = helper.getReadableDatabase().query(ConfigurationDatabaseHelper.TABLE_NAME,
                new String[]{"_id"}, ConfigurationDatabaseHelper.BY_NAME, new String[]{name}, null, null, null)) {
            assertTrue(cursor.moveToFirst());
            long result = cursor.getLong(0);
            assertFalse(cursor.moveToNext());
            return result;
        }
    }

    private void rejected(Runnable action) {
        try {
            action.run();
            fail("The configuration must be rejected without a database mutation");
        } catch (IllegalArgumentException expected) {
            // The rejection is followed by assertions against the real stored rows.
        }
    }

    public void testPutUsesLocalIdentityAndHonorsDisabledState() {
        ConnectionConfiguration input = server(false);
        input.connected = true;
        input.peerNodeId = "untrusted-peer";
        ConnectionConfiguration stored = helper.putManagedConfiguration(input, "local-node");
        assertEquals("server", stored.name);
        assertEquals("local-node", stored.nodeId);
        assertFalse(stored.enabled);
        assertFalse(stored.connected);
        assertNull(stored.peerNodeId);
        assertEquals(1, helper.getAllConfigurations().length);
    }

    public void testRepeatedPutKeepsRowAndNegotiatedIdentity() {
        helper.putManagedConfiguration(server(true), "original-node");
        long originalId = rowId("server");
        ConnectionConfiguration replacement = server(false);
        replacement.nodeId = "caller-replacement";
        helper.putManagedConfiguration(replacement, "new-local-default");
        helper.putManagedConfiguration(replacement, "third-local-default");
        assertEquals(originalId, rowId("server"));
        assertEquals(1, helper.getAllConfigurations().length);
        assertEquals("original-node", helper.getConfiguration("server").nodeId);
        assertFalse(helper.getConfiguration("server").enabled);
    }

    public void testPutNameCollisionDoesNotReplaceDifferentAddress() {
        helper.putManagedConfiguration(bluetooth("watch", "AA:BB:CC:DD:EE:01", true), "local-node");
        long originalId = rowId("watch");
        rejected(() -> helper.putManagedConfiguration(bluetooth("watch", "AA:BB:CC:DD:EE:02", false), "other"));
        assertEquals(originalId, rowId("watch"));
        assertEquals("AA:BB:CC:DD:EE:01", helper.getConfiguration("watch").address);
        assertTrue(helper.getConfiguration("watch").enabled);
        assertEquals("local-node", helper.getConfiguration("watch").nodeId);
        assertEquals(1, helper.getAllConfigurations().length);
    }

    public void testPutNameCollisionDoesNotChangeTransport() {
        helper.putManagedConfiguration(server(true), "local-node");
        rejected(() -> helper.putManagedConfiguration(bluetooth("server", "AA:BB:CC:DD:EE:01", false), "other"));
        assertEquals(2, helper.getConfiguration("server").type);
        assertEquals(2, helper.getConfiguration("server").role);
        assertTrue(helper.getConfiguration("server").enabled);
        assertEquals("local-node", helper.getConfiguration("server").nodeId);
    }

    public void testPutDoesNotCreateDuplicateAddresses() {
        helper.putManagedConfiguration(bluetooth("first", "AA:BB:CC:DD:EE:01", true), "local-node");
        rejected(() -> helper.putManagedConfiguration(bluetooth("second", "AA:BB:CC:DD:EE:01", true), "local-node"));
        helper.putManagedConfiguration(server(false), "local-node");
        rejected(() -> helper.putManagedConfiguration(
                new ConnectionConfiguration("another-server", null, 2, 2, true), "local-node"));
        assertEquals(2, helper.getAllConfigurations().length);
        assertNull(helper.getConfiguration("second"));
        assertNull(helper.getConfiguration("another-server"));
        assertFalse(helper.getConfiguration("server").enabled);
    }

    private ConnectionConfiguration emulator(String name, String node, boolean enabled) {
        return new ConnectionConfiguration(name, "EmulatorAddr-" + node, 2, 2, enabled, "caller-provided-identity");
    }

    public void testSelectSecondEmulatorAndReturnPreservesBothIdentitiesAndRows() {
        helper.putManagedConfiguration(emulator("wear61", "peer61", true), "local-node");
        helper.putConfiguration(new ConnectionConfiguration("wear61", "EmulatorAddr-peer61", 2, 2, true, "peer61"), "local-node");
        long firstId = rowId("wear61");
        ConnectionConfiguration second = helper.putManagedConfiguration(emulator("wear60", "peer60", true), "local-node");
        assertEquals("local-node", second.nodeId);
        assertFalse(helper.getConfiguration("wear61").enabled);
        assertEquals("peer61", helper.getConfiguration("wear61").nodeId);
        helper.putConfiguration(new ConnectionConfiguration("wear60", "EmulatorAddr-peer60", 2, 2, true, "peer60"), "local-node");
        long secondId = rowId("wear60");

        helper.putManagedConfiguration(emulator("wear61", "peer61", true), "other-local-node");
        assertTrue(helper.getConfiguration("wear61").enabled);
        assertFalse(helper.getConfiguration("wear60").enabled);
        assertEquals("peer61", helper.getConfiguration("wear61").nodeId);
        assertEquals("peer60", helper.getConfiguration("wear60").nodeId);
        assertEquals(firstId, rowId("wear61"));
        assertEquals(secondId, rowId("wear60"));
        assertEquals(2, helper.getAllConfigurations().length);
    }

    public void testDisabledPutDoesNotSwitchActiveEmulatorAndExplicitEnableDoes() {
        helper.putManagedConfiguration(emulator("first", "peer1", true), "local-node");
        helper.putManagedConfiguration(emulator("second", "peer2", false), "local-node");
        assertTrue(helper.getConfiguration("first").enabled);
        assertFalse(helper.getConfiguration("second").enabled);
        helper.enableManagedConfiguration("second");
        assertFalse(helper.getConfiguration("first").enabled);
        assertTrue(helper.getConfiguration("second").enabled);
        helper.updateConfiguration(emulator("first", "peer1", true));
        assertTrue(helper.getConfiguration("first").enabled);
        assertFalse(helper.getConfiguration("second").enabled);
    }

    public void testFailedSelectionRollsBackNewRowAndPreviousEnabledState() {
        helper.putManagedConfiguration(emulator("first", "peer1", true), "local-node");
        // A corrupt legacy row must not be silently rewritten while selecting another emulator.
        helper.putConfiguration(new ConnectionConfiguration("legacy", "EmulatorAddr-peer-legacy", 2, 99, false, "legacy-node"));
        rejected(() -> helper.putManagedConfiguration(emulator("second", "peer2", true), "local-node"));
        assertNull(helper.getConfiguration("second"));
        assertTrue(helper.getConfiguration("first").enabled);
        assertEquals("local-node", helper.getConfiguration("first").nodeId);
        assertFalse(helper.getConfiguration("legacy").enabled);
        assertEquals(99, helper.getConfiguration("legacy").role);
    }

    public void testAmbiguousLegacyEmulatorAddressCannotSelectEitherRow() {
        helper.putConfiguration(new ConnectionConfiguration("first", "EmulatorAddr-peer", 2, 2, false, "peer"));
        helper.putConfiguration(new ConnectionConfiguration("second", "EmulatorAddr-peer", 2, 2, false, "peer"));
        rejected(() -> helper.enableManagedConfiguration("first"));
        assertFalse(helper.getConfiguration("first").enabled);
        assertFalse(helper.getConfiguration("second").enabled);
    }

    public void testConflictingSecondEmulatorCannotChangeTheActiveSelection() {
        helper.putManagedConfiguration(emulator("first", "peer1", true), "local-node");
        rejected(() -> helper.putManagedConfiguration(emulator("first", "peer2", true), "local-node"));
        rejected(() -> helper.putManagedConfiguration(emulator("second", "peer1", true), "local-node"));
        rejected(() -> helper.putManagedConfiguration(emulator("second", "", true), "local-node"));
        rejected(() -> helper.putManagedConfiguration(new ConnectionConfiguration("second", "127.0.0.1", 2, 2, true), "local-node"));
        rejected(() -> helper.putManagedConfiguration(new ConnectionConfiguration("second", "peer2", 2, 2, true), "local-node"));
        assertEquals(1, helper.getAllConfigurations().length);
        assertTrue(helper.getConfiguration("first").enabled);
        assertEquals("EmulatorAddr-peer1", helper.getConfiguration("first").address);
        assertEquals("local-node", helper.getConfiguration("first").nodeId);
    }

    public void testUpdateMissingLinkDoesNotCreateOrEnableServer() {
        assertFalse(helper.updateConfiguration(server(true)));
        assertEquals(0, helper.getAllConfigurations().length);
        assertNull(helper.getConfiguration("server"));
    }

    public void testUpdateChangesEnabledOnlyAndPreservesIdentity() {
        helper.putManagedConfiguration(server(true), "original-node");
        long originalId = rowId("server");
        ConnectionConfiguration update = server(false);
        update.nodeId = "peer-provided-node";
        update.connected = true;
        update.peerNodeId = "peer-provided-peer";
        assertTrue(helper.updateConfiguration(update));
        ConnectionConfiguration stored = helper.getConfiguration("server");
        assertFalse(stored.enabled);
        assertEquals("original-node", stored.nodeId);
        assertFalse(stored.connected);
        assertNull(stored.peerNodeId);
        assertEquals(originalId, rowId("server"));
        assertTrue(helper.updateConfiguration(server(true)));
        assertTrue(helper.getConfiguration("server").enabled);
        assertEquals(1, helper.getAllConfigurations().length);
    }

    public void testUpdateDoesNotUseSharedLocalNodeAsRowKey() {
        helper.putManagedConfiguration(server(true), "same-local-node");
        helper.putManagedConfiguration(bluetooth("watch", "AA:BB:CC:DD:EE:01", true), "same-local-node");
        assertTrue(helper.updateConfiguration(bluetooth("watch", "AA:BB:CC:DD:EE:01", false)));
        assertFalse(helper.getConfiguration("watch").enabled);
        assertTrue(helper.getConfiguration("server").enabled);
        assertEquals("same-local-node", helper.getConfiguration("watch").nodeId);
        assertEquals("same-local-node", helper.getConfiguration("server").nodeId);
        assertEquals(2, helper.getAllConfigurations().length);
    }

    public void testUpdateCannotRenameOrOverwriteOtherLink() {
        helper.putManagedConfiguration(bluetooth("first", "AA:BB:CC:DD:EE:01", true), "node-first");
        helper.putManagedConfiguration(bluetooth("second", "AA:BB:CC:DD:EE:02", true), "node-second");
        long first = rowId("first"), second = rowId("second");
        rejected(() -> helper.updateConfiguration(bluetooth("second", "AA:BB:CC:DD:EE:01", false)));
        assertEquals(first, rowId("first"));
        assertEquals(second, rowId("second"));
        assertTrue(helper.getConfiguration("first").enabled);
        assertTrue(helper.getConfiguration("second").enabled);
        assertEquals("node-first", helper.getConfiguration("first").nodeId);
        assertEquals("node-second", helper.getConfiguration("second").nodeId);
    }

    public void testAmbiguousLegacyAddressRejectsUpdateWithoutMutatingEitherRow() {
        // Legacy databases can contain distinct names with one address; update must not guess.
        helper.putConfiguration(bluetooth("first", "AA:BB:CC:DD:EE:01", true));
        helper.putConfiguration(bluetooth("second", "AA:BB:CC:DD:EE:01", true));
        rejected(() -> helper.updateConfiguration(bluetooth("first", "AA:BB:CC:DD:EE:01", false)));
        assertEquals(2, helper.getAllConfigurations().length);
        assertTrue(helper.getConfiguration("first").enabled);
        assertTrue(helper.getConfiguration("second").enabled);
    }

    public void testUnsupportedPolicyDoesNotModifyOrCreateLinks() {
        helper.putManagedConfiguration(server(true), "local-node");
        ConnectionConfiguration restricted = server(false);
        restricted.hasUnsupportedConnectionPolicies = true;
        rejected(() -> helper.updateConfiguration(restricted));
        ConnectionConfiguration migrating = bluetooth("new", "AA:BB:CC:DD:EE:01", true);
        migrating.migrating = true;
        rejected(() -> helper.putManagedConfiguration(migrating, "other"));
        ConnectionConfiguration noSync = server(false);
        noSync.dataItemSyncEnabled = false;
        rejected(() -> helper.putManagedConfiguration(noSync, "other"));
        assertEquals(1, helper.getAllConfigurations().length);
        assertTrue(helper.getConfiguration("server").enabled);
        assertEquals("local-node", helper.getConfiguration("server").nodeId);
    }

    public void testUpdateCannotTurnOffEstablishedDataSynchronization() {
        helper.putManagedConfiguration(server(true), "local-node");
        ConnectionConfiguration update = server(true);
        update.dataItemSyncEnabled = false;
        assertTrue(helper.updateConfiguration(update));
        assertTrue(helper.getConfiguration("server").dataItemSyncEnabled);
        assertTrue(helper.getConfiguration("server").enabled);
        assertEquals("local-node", helper.getConfiguration("server").nodeId);
    }

    public void testConfigurationRestrictionsCannotBeSilentlyDropped() {
        helper.putManagedConfiguration(server(true), "local-node");
        ConnectionConfiguration restricted = server(false);
        restricted.allowedConfigPackages = Collections.singletonList("only.this.package");
        rejected(() -> helper.updateConfiguration(restricted));
        assertTrue(helper.getConfiguration("server").enabled);
        assertEquals(1, helper.getAllConfigurations().length);
    }

    public void testInvalidTransportAndNullAreRejectedWithoutRows() {
        rejected(() -> helper.putManagedConfiguration(null, "local-node"));
        rejected(() -> helper.putManagedConfiguration(new ConnectionConfiguration("bad", null, 99, 1, true), "local-node"));
        rejected(() -> helper.updateConfiguration(new ConnectionConfiguration("bad", null, 1, 1, true)));
        assertEquals(0, helper.getAllConfigurations().length);
    }

    public void testMissingBluetoothNameAndReservedSentinelsDoNotBecomeStoredNulls() {
        rejected(() -> helper.putManagedConfiguration(bluetooth(null, "AA:BB:CC:DD:EE:01", true), "local-node"));
        rejected(() -> helper.putManagedConfiguration(bluetooth("NULL_STRING", "AA:BB:CC:DD:EE:01", true), "local-node"));
        rejected(() -> helper.putManagedConfiguration(bluetooth("watch", "NULL_STRING", true), "local-node"));
        assertEquals(0, helper.getAllConfigurations().length);
        assertEquals("server", helper.putManagedConfiguration(server(false), "local-node").name);
    }
}
