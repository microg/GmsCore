/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.gms.wearable;

import android.os.Binder;
import android.os.Parcel;
import android.os.RemoteException;
import android.test.AndroidTestCase;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.wearable.ConnectionConfiguration;
import com.google.android.gms.wearable.internal.IWearableService;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Binder contracts are checked against captured companion ABI, with real Parcel and caller denial. */
public class WearableConfigurationBinderTest extends AndroidTestCase {
    private interface Transaction {
        void inspect(int code, Parcel data, int flags);
    }

    private Binder endpoint(Transaction transaction) {
        return new Binder() {
            @Override protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) {
                transaction.inspect(code, data, flags);
                if (reply != null) reply.writeNoException();
                return true;
            }
        };
    }

    private ConnectionConfiguration config() {
        return new ConnectionConfiguration("test-server", null, 2, 2, false, "local-node");
    }

    private void configurationRequest(Parcel data) {
        data.enforceInterface("com.google.android.gms.wearable.internal.IWearableService");
        assertNotNull(data.readStrongBinder());
        assertEquals(1, data.readInt());
        ConnectionConfiguration value = ConnectionConfiguration.CREATOR.createFromParcel(data);
        assertEquals("test-server", value.name);
        assertNull(value.address);
        assertEquals(2, value.type);
        assertEquals(2, value.role);
        assertFalse(value.enabled);
        assertEquals("local-node", value.nodeId);
        assertEquals(0, data.dataAvail());
    }

    public void testPutConfigUsesBinder20() throws RemoteException {
        AtomicInteger calls = new AtomicInteger();
        IWearableService proxy = IWearableService.Stub.asInterface(endpoint((code, data, flags) -> {
            calls.incrementAndGet();
            assertEquals(20, code);
            assertEquals(0, flags);
            configurationRequest(data);
        }));
        proxy.putConfig(new BaseWearableCallbacks(), config());
        assertEquals(1, calls.get());
    }

    public void testUpdateConfigUsesBinder74() throws RemoteException {
        AtomicInteger calls = new AtomicInteger();
        IWearableService proxy = IWearableService.Stub.asInterface(endpoint((code, data, flags) -> {
            calls.incrementAndGet();
            assertEquals(74, code);
            assertEquals(0, flags);
            configurationRequest(data);
        }));
        proxy.updateConfig(new BaseWearableCallbacks(), config());
        assertEquals(1, calls.get());
    }

    public void testConfigurationParcelKeepsNodeAndPoliciesInOfficialFields() {
        ConnectionConfiguration value = config();
        value.peerNodeId = "peer-node";
        value.allowedConfigPackages = Collections.singletonList("permitted.package");
        Parcel parcel = Parcel.obtain();
        try {
            value.writeToParcel(parcel, 0);
            parcel.setDataPosition(0);
            assertEquals(0xffff4f45, parcel.readInt());
            int end = parcel.readInt() + parcel.dataPosition();
            int seen = 0;
            while (parcel.dataPosition() < end) {
                int header = parcel.readInt();
                int field = header & 0xffff;
                int size = (header >>> 16) == 0xffff ? parcel.readInt() : header >>> 16;
                int next = parcel.dataPosition() + size;
                if (field == 2) {
                    assertEquals("test-server", parcel.readString());
                    seen |= 1;
                } else if (field == 6) {
                    assertEquals(0, parcel.readInt());
                    seen |= 2;
                } else if (field == 8) {
                    assertEquals("peer-node", parcel.readString());
                    seen |= 4;
                } else if (field == 10) {
                    assertEquals("local-node", parcel.readString());
                    seen |= 8;
                } else if (field == 13) {
                    assertEquals(Collections.singletonList("permitted.package"), parcel.createStringArrayList());
                    seen |= 16;
                }
                parcel.setDataPosition(next);
            }
            assertEquals(31, seen);
            assertEquals(end, parcel.dataPosition());
        } finally {
            parcel.recycle();
        }
    }

    private ConnectionConfiguration withUnknownPolicy(int field, boolean present) {
        Parcel parcel = Parcel.obtain();
        try {
            config().writeToParcel(parcel, 0);
            parcel.writeInt(((present ? 4 : 0) << 16) | field);
            if (present) parcel.writeInt(1);
            int end = parcel.dataPosition();
            parcel.setDataPosition(4);
            parcel.writeInt(end - 8);
            parcel.setDataPosition(end);
            parcel.writeInt(0x12345678);
            parcel.setDataPosition(0);
            ConnectionConfiguration result = ConnectionConfiguration.CREATOR.createFromParcel(parcel);
            assertEquals(end, parcel.dataPosition());
            assertEquals(0x12345678, parcel.readInt());
            return result;
        } finally {
            parcel.recycle();
        }
    }

    public void testUnknownPolicy16And18AreRetainedForRejection() {
        for (int field : new int[]{16, 18}) {
            ConnectionConfiguration result = withUnknownPolicy(field, true);
            assertTrue("Unsupported policy field must survive decoding", result.hasUnsupportedConnectionPolicies);
            try {
                ConfigurationDatabaseHelper.validateManagedConfiguration(result);
                fail("An unsupported policy cannot be silently ignored");
            } catch (IllegalArgumentException expected) {
                // Positive presence is rejected before a database or connection mutation.
            }
        }
    }

    public void testNullUnknownPolicyDoesNotInventARestriction() {
        for (int field : new int[]{16, 18}) {
            ConnectionConfiguration result = withUnknownPolicy(field, false);
            assertFalse(result.hasUnsupportedConnectionPolicies);
            ConfigurationDatabaseHelper.validateManagedConfiguration(result);
        }
    }

    private static final class StatusCallback extends BaseWearableCallbacks {
        final CountDownLatch finished = new CountDownLatch(1);
        final AtomicInteger calls = new AtomicInteger();
        volatile int status = -1;

        @Override public void onStatus(Status result) {
            status = result.getStatusCode();
            calls.incrementAndGet();
            finished.countDown();
        }

        void awaitDenied() throws InterruptedException {
            assertTrue("The failed configuration operation must complete its callback", finished.await(3, TimeUnit.SECONDS));
            assertEquals(CommonStatusCodes.DEVELOPER_ERROR, status);
            assertEquals(1, calls.get());
        }
    }

    private void deniedCallerCannotMutateConfiguration(String claimedPackage) throws Exception {
        assertEquals("Tests must only use the dedicated test application's storage",
                "org.microg.gms.wearable.core.test", getContext().getPackageName());
        getContext().deleteDatabase("node.db");
        getContext().deleteDatabase("connectionconfig.db");
        NodeDatabaseHelper nodes = new NodeDatabaseHelper(getContext());
        ConfigurationDatabaseHelper configurations = new ConfigurationDatabaseHelper(getContext());
        WearableImpl implementation = null;
        try {
            implementation = new WearableImpl(getContext(), nodes, configurations);
            // Disabled fixture: the test never opens a socket or accesses a Bluetooth device.
            configurations.putManagedConfiguration(config(), "original-node");
            WearableServiceImpl service = new WearableServiceImpl(getContext(), implementation, claimedPackage);
            IWearableService proxy = IWearableService.Stub.asInterface(new Binder() {
                @Override protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
                    return service.onTransact(code, data, reply, flags);
                }
            });
            StatusCallback[] callbacks = new StatusCallback[7];
            for (int i = 0; i < callbacks.length; i++) callbacks[i] = new StatusCallback();
            // A distinct emulator is a valid configuration, but this caller cannot select it.
            proxy.putConfig(callbacks[0], new ConnectionConfiguration("new-server", "EmulatorAddr-new-node", 2, 2, true));
            proxy.updateConfig(callbacks[1], new ConnectionConfiguration("test-server", null, 2, 2, true));
            proxy.enableConfig(callbacks[2], "test-server");
            proxy.disableConfig(callbacks[3], "test-server");
            proxy.deleteConfig(callbacks[4], "test-server");
            proxy.enableConnection(callbacks[5]);
            proxy.disableConnection(callbacks[6]);
            for (StatusCallback callback : callbacks) callback.awaitDenied();
            assertEquals(1, configurations.getAllConfigurations().length);
            assertNull(configurations.getConfiguration("new-server"));
            assertEquals("original-node", configurations.getConfiguration("test-server").nodeId);
            assertFalse(configurations.getConfiguration("test-server").enabled);
            for (StatusCallback callback : callbacks) assertEquals(1, callback.calls.get());
        } finally {
            if (implementation != null) {
                Thread network = implementation.networkHandler.getLooper().getThread();
                implementation.stop();
                network.join(3000);
                assertFalse("The test must not leave a wearable network thread running", network.isAlive());
            }
            configurations.close();
            nodes.close();
            getContext().deleteDatabase("connectionconfig.db");
            getContext().deleteDatabase("node.db");
        }
    }

    public void testOrdinaryAppGetsOneTerminalErrorForEveryConfigurationMutation() throws Exception {
        deniedCallerCannotMutateConfiguration(getContext().getPackageName());
    }

    public void testBorrowedCompanionPackageDoesNotGrantCallerItsPrivileges() throws Exception {
        deniedCallerCannotMutateConfiguration("com.google.android.apps.wear.companion");
    }
}
