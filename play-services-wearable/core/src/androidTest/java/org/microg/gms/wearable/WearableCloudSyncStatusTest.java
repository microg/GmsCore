/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.gms.wearable;

import android.os.Binder;
import android.os.Parcel;
import android.os.RemoteException;
import android.test.AndroidTestCase;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.wearable.internal.GetCloudSyncOptInStatusResponse;
import com.google.android.gms.wearable.internal.GetCloudSyncSettingResponse;
import com.google.android.gms.wearable.internal.IWearableCallbacks;
import com.google.android.gms.wearable.internal.IWearableService;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Checks the companion ABI independently of the generated SafeParcelable field reader. */
public class WearableCloudSyncStatusTest extends AndroidTestCase {
    private void assertFields(Parcel parcel, int status, boolean done, boolean optedIn) {
        assertEquals(0xffff4f45, parcel.readInt());
        int end = parcel.readInt() + parcel.dataPosition();
        int seen = 0;
        while (parcel.dataPosition() < end) {
            int header = parcel.readInt();
            int field = header & 0xffff;
            int size = (header >>> 16) == 0xffff ? parcel.readInt() : header >>> 16;
            int next = parcel.dataPosition() + size;
            assertTrue(next <= end);
            if (field >= 2 && field <= 4) {
                int bit = 1 << (field - 2);
                assertEquals("Fields must not be duplicated", 0, seen & bit);
                seen |= bit;
                assertEquals(4, size);
                assertEquals(field == 2 ? status : (field == 3 ? (done ? 1 : 0) : (optedIn ? 1 : 0)),
                        parcel.readInt());
            }
            parcel.setDataPosition(next);
        }
        assertEquals(7, seen);
        assertEquals(end, parcel.dataPosition());
    }

    public void testResponseWritesStatusAndIndependentBooleansInOfficialFields() {
        for (boolean done : new boolean[]{false, true}) {
            for (boolean optedIn : new boolean[]{false, true}) {
                Parcel parcel = Parcel.obtain();
                try {
                    new GetCloudSyncOptInStatusResponse(8, done, optedIn).writeToParcel(parcel, 0);
                    parcel.writeInt(0x2843);
                    parcel.setDataPosition(0);
                    assertFields(parcel, 8, done, optedIn);
                    assertEquals(0x2843, parcel.readInt());
                    assertEquals(0, parcel.dataAvail());
                } finally {
                    parcel.recycle();
                }
            }
        }
    }

    public void testCreatorReadsOfficialFieldNumbersWithoutReusingItsWriter() {
        for (boolean done : new boolean[]{false, true}) {
            for (boolean optedIn : new boolean[]{false, true}) {
                Parcel parcel = Parcel.obtain();
                try {
                    parcel.writeInt(0xffff4f45);
                    parcel.writeInt(32);
                    parcel.writeInt(0x00040001);
                    parcel.writeInt(1);
                    parcel.writeInt(0x00040002);
                    parcel.writeInt(10);
                    parcel.writeInt(0x00040003);
                    parcel.writeInt(done ? 1 : 0);
                    parcel.writeInt(0x00040004);
                    parcel.writeInt(optedIn ? 1 : 0);
                    parcel.writeInt(0x2843);
                    parcel.setDataPosition(0);
                    GetCloudSyncOptInStatusResponse result = GetCloudSyncOptInStatusResponse.CREATOR.createFromParcel(parcel);
                    assertEquals(10, result.statusCode);
                    assertEquals(done, result.optInDone);
                    assertEquals(optedIn, result.optedIn);
                    assertEquals(0x2843, parcel.readInt());
                    assertEquals(0, parcel.dataAvail());
                } finally {
                    parcel.recycle();
                }
            }
        }
    }

    public void testCallbackUsesBinder30AndOfficialResponseFields() throws RemoteException {
        AtomicInteger calls = new AtomicInteger();
        IWearableCallbacks callbacks = IWearableCallbacks.Stub.asInterface(new Binder() {
            @Override protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) {
                assertEquals(30, code);
                assertEquals(0, flags);
                data.enforceInterface("com.google.android.gms.wearable.internal.IWearableCallbacks");
                assertEquals(1, data.readInt());
                assertFields(data, 0, true, false);
                assertEquals(0, data.dataAvail());
                calls.incrementAndGet();
                reply.writeNoException();
                return true;
            }
        });
        callbacks.onGetCloudSyncOptInStatusResponse(new GetCloudSyncOptInStatusResponse(0, true, false));
        assertEquals(1, calls.get());
    }

    public void testServiceBinder52CompletesOnceWithCloudConsentUnset() throws Exception {
        assertEquals("Only the instrumentation application's isolated storage may be used",
                "org.microg.gms.wearable.core.test", getContext().getPackageName());
        getContext().deleteDatabase("node.db");
        getContext().deleteDatabase("connectionconfig.db");
        NodeDatabaseHelper nodes = new NodeDatabaseHelper(getContext());
        ConfigurationDatabaseHelper configurations = new ConfigurationDatabaseHelper(getContext());
        WearableImpl implementation = null;
        try {
            // No connection configurations: this service fixture opens no transport.
            implementation = new WearableImpl(getContext(), nodes, configurations);
            WearableServiceImpl service = new WearableServiceImpl(getContext(), implementation, getContext().getPackageName());
            AtomicInteger requests = new AtomicInteger();
            IWearableService proxy = IWearableService.Stub.asInterface(new Binder() {
                @Override protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
                    assertEquals(52, code);
                    assertEquals(0, flags);
                    requests.incrementAndGet();
                    return service.onTransact(code, data, reply, flags);
                }
            });
            AtomicInteger responses = new AtomicInteger();
            CountDownLatch completed = new CountDownLatch(1);
            IWearableCallbacks callbacks = IWearableCallbacks.Stub.asInterface(new Binder() {
                @Override protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) {
                    assertEquals(30, code);
                    assertEquals(0, flags);
                    data.enforceInterface("com.google.android.gms.wearable.internal.IWearableCallbacks");
                    assertEquals(1, data.readInt());
                    assertFields(data, 0, false, false);
                    assertEquals(0, data.dataAvail());
                    responses.incrementAndGet();
                    completed.countDown();
                    reply.writeNoException();
                    return true;
                }
            });
            proxy.getCloudSyncOptInStatus(callbacks);
            assertTrue("The caller must receive a terminal response", completed.await(3, TimeUnit.SECONDS));
            assertEquals(1, requests.get());
            assertEquals(1, responses.get());
            assertEquals(0, configurations.getAllConfigurations().length);
        } finally {
            if (implementation != null) {
                Thread network = implementation.networkHandler.getLooper().getThread();
                implementation.stop();
                network.join(3000);
                assertFalse("The test must release its network thread", network.isAlive());
            }
            configurations.close();
            nodes.close();
            getContext().deleteDatabase("connectionconfig.db");
            getContext().deleteDatabase("node.db");
        }
    }

    private IWearableCallbacks remoteCallback(BaseWearableCallbacks callback, int expectedCode) {
        return IWearableCallbacks.Stub.asInterface(new Binder() {
            @Override protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
                assertEquals(expectedCode, code);
                assertEquals(0, flags);
                return callback.onTransact(code, data, reply, flags);
            }
        });
    }

    private void cloudMutationCannotEnableCloud(int transaction) throws Exception {
        assertEquals("org.microg.gms.wearable.core.test", getContext().getPackageName());
        getContext().deleteDatabase("node.db");
        getContext().deleteDatabase("connectionconfig.db");
        NodeDatabaseHelper nodes = new NodeDatabaseHelper(getContext());
        ConfigurationDatabaseHelper configurations = new ConfigurationDatabaseHelper(getContext());
        WearableImpl implementation = null;
        try {
            implementation = new WearableImpl(getContext(), nodes, configurations);
            WearableServiceImpl service = new WearableServiceImpl(getContext(), implementation, getContext().getPackageName());
            AtomicInteger expectedTransaction = new AtomicInteger();
            AtomicInteger requestCount = new AtomicInteger();
            IWearableService proxy = IWearableService.Stub.asInterface(new Binder() {
                @Override protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
                    assertEquals(expectedTransaction.get(), code);
                    assertEquals(0, flags);
                    requestCount.incrementAndGet();
                    return service.onTransact(code, data, reply, flags);
                }
            });
            // Repeated disabling must remain harmless after an unsupported enable request.
            for (boolean enable : new boolean[]{false, true, false}) {
                AtomicInteger completions = new AtomicInteger();
                IWearableCallbacks mutation = remoteCallback(new BaseWearableCallbacks() {
                    @Override public void onStatus(Status status) {
                        assertEquals(enable ? 17 : 0, status.getStatusCode());
                        completions.incrementAndGet();
                    }
                }, 11);
                expectedTransaction.set(transaction);
                if (transaction == 48) proxy.optInCloudSync(mutation, enable);
                else proxy.setCloudSyncSetting(mutation, enable);
                assertEquals("The synchronous operation must finish exactly once", 1, completions.get());

                AtomicInteger statusReads = new AtomicInteger();
                expectedTransaction.set(52);
                proxy.getCloudSyncOptInStatus(remoteCallback(new BaseWearableCallbacks() {
                    @Override public void onGetCloudSyncOptInStatusResponse(GetCloudSyncOptInStatusResponse result) {
                        assertEquals(0, result.statusCode);
                        assertFalse(result.optInDone);
                        assertFalse(result.optedIn);
                        statusReads.incrementAndGet();
                    }
                }, 30));
                assertEquals(1, statusReads.get());

                AtomicInteger settingReads = new AtomicInteger();
                expectedTransaction.set(51);
                proxy.getCloudSyncSetting(remoteCallback(new BaseWearableCallbacks() {
                    @Override public void onGetCloudSyncSettingResponse(GetCloudSyncSettingResponse result) {
                        assertEquals(0, result.statusCode);
                        assertFalse(result.cloudSyncEnabled);
                        settingReads.incrementAndGet();
                    }
                }, 29));
                assertEquals(1, settingReads.get());
                assertEquals(1, completions.get());
            }
            assertEquals(9, requestCount.get());
            assertEquals(0, configurations.getAllConfigurations().length);
        } finally {
            if (implementation != null) {
                Thread network = implementation.networkHandler.getLooper().getThread();
                implementation.stop();
                network.join(3000);
                assertFalse(network.isAlive());
            }
            configurations.close();
            nodes.close();
            getContext().deleteDatabase("connectionconfig.db");
            getContext().deleteDatabase("node.db");
        }
    }

    public void testOptInCloudSyncBinder48RejectsEnableAndLeavesConsentUnset() throws Exception {
        cloudMutationCannotEnableCloud(48);
    }

    public void testSetCloudSyncSettingBinder50RejectsEnableAndLeavesConsentUnset() throws Exception {
        cloudMutationCannotEnableCloud(50);
    }

    public void testSyncWifiCredentialsBinder37RejectsOnceWithoutClaimingSynchronization() throws Exception {
        assertEquals("org.microg.gms.wearable.core.test", getContext().getPackageName());
        getContext().deleteDatabase("node.db");
        getContext().deleteDatabase("connectionconfig.db");
        NodeDatabaseHelper nodes = new NodeDatabaseHelper(getContext());
        ConfigurationDatabaseHelper configurations = new ConfigurationDatabaseHelper(getContext());
        WearableImpl implementation = null;
        try {
            implementation = new WearableImpl(getContext(), nodes, configurations);
            WearableServiceImpl service = new WearableServiceImpl(getContext(), implementation, getContext().getPackageName());
            AtomicInteger requests = new AtomicInteger();
            IWearableService proxy = IWearableService.Stub.asInterface(new Binder() {
                @Override protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
                    assertEquals(37, code);
                    assertEquals(0, flags);
                    requests.incrementAndGet();
                    return service.onTransact(code, data, reply, flags);
                }
            });
            AtomicInteger responses = new AtomicInteger();
            proxy.syncWifiCredentials(remoteCallback(new BaseWearableCallbacks() {
                @Override public void onStatus(Status status) {
                    assertEquals(17, status.getStatusCode());
                    assertEquals("Wi-Fi credential synchronization is not supported.", status.getStatusMessage());
                    responses.incrementAndGet();
                }
            }, 11));
            assertEquals(1, requests.get());
            assertEquals("The unavailable operation must complete rather than leave a pending Task", 1, responses.get());
            AtomicInteger cloudReads = new AtomicInteger();
            service.getCloudSyncOptInStatus(remoteCallback(new BaseWearableCallbacks() {
                @Override public void onGetCloudSyncOptInStatusResponse(GetCloudSyncOptInStatusResponse result) {
                    assertEquals(0, result.statusCode);
                    assertFalse(result.optInDone);
                    assertFalse(result.optedIn);
                    cloudReads.incrementAndGet();
                }
            }, 30));
            assertEquals(1, cloudReads.get());
            assertEquals(1, responses.get());
            assertEquals(0, configurations.getAllConfigurations().length);
        } finally {
            if (implementation != null) {
                Thread network = implementation.networkHandler.getLooper().getThread();
                implementation.stop();
                network.join(3000);
                assertFalse(network.isAlive());
            }
            configurations.close();
            nodes.close();
            getContext().deleteDatabase("connectionconfig.db");
            getContext().deleteDatabase("node.db");
        }
    }
}
