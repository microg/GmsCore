/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.gms.wearable;

import android.database.sqlite.SQLiteDatabase;
import android.os.Binder;
import android.os.Parcel;
import android.os.RemoteException;
import android.test.AndroidTestCase;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.wearable.internal.AddAccountToConsentRequest;
import com.google.android.gms.wearable.internal.ConsentResponse;
import com.google.android.gms.wearable.internal.IWearableCallbacks;
import com.google.android.gms.wearable.internal.IWearableService;
import org.microg.gms.wearable.consent.WearableConsentStore;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

/** Independent wire layout checks and real Binder rejection; no Google account is installed or used. */
public class WearableConsentBinderTest extends AndroidTestCase {
    private static final String SERVICE = "com.google.android.gms.wearable.internal.IWearableService";
    private static final String CALLBACK = "com.google.android.gms.wearable.internal.IWearableCallbacks";

    private void readResponseFields(Parcel data, int status) {
        assertEquals(0xffff4f45, data.readInt());
        int end = data.readInt() + data.dataPosition();
        int seen = 0;
        while (data.dataPosition() < end) {
            int header = data.readInt();
            int field = header & 0xffff;
            int size = (header >>> 16) == 0xffff ? data.readInt() : header >>> 16;
            int next = data.dataPosition() + size;
            assertTrue(field >= 1 && field <= 8);
            assertEquals(0, seen & (1 << (field - 1)));
            seen |= 1 << (field - 1);
            assertTrue(next <= end);
            if (field <= 5) {
                assertEquals(4, size);
                assertEquals(field == 1 ? status : (field == 2 || field == 5 ? 1 : 0), data.readInt());
            } else if (field == 6) {
                assertEquals(0, data.readInt());
            } else if (field == 7) {
                assertEquals("synthetic-node", data.readString());
            } else {
                assertEquals(8, size);
                assertEquals(1234567890123L, data.readLong());
            }
            data.setDataPosition(next);
        }
        assertEquals(255, seen);
        assertEquals(end, data.dataPosition());
    }

    private ConsentResponse response() {
        ConsentResponse result = new ConsentResponse(8, true);
        result.hasLocationConsent = true;
        result.nodeId = "synthetic-node";
        result.lastUpdateRequestedTime = 1234567890123L;
        return result;
    }

    public void testConsentResponseWritesOfficialFieldsWithoutVersionOffset() {
        Parcel data = Parcel.obtain();
        try {
            response().writeToParcel(data, 0);
            data.writeInt(0x2843);
            data.setDataPosition(0);
            readResponseFields(data, 8);
            assertEquals(0x2843, data.readInt());
            assertEquals(0, data.dataAvail());
        } finally { data.recycle(); }
    }

    public void testConsentCreatorReadsIndependentLayoutAndNullableTimestamp() {
        for (boolean nullable : new boolean[]{false, true}) {
            Parcel data = Parcel.obtain();
            try {
                data.writeInt(0xffff4f45);
                data.writeInt(0);
                for (int field = 1; field <= 5; field++) {
                    data.writeInt(0x00040000 | field);
                    data.writeInt(field == 1 ? 10 : (field == 2 ? 1 : 0));
                }
                data.writeInt(0x00040006);
                data.writeInt(0); // Empty typed account-consent list.
                if (nullable) {
                    data.writeInt(7);
                    data.writeInt(8);
                } else {
                    data.writeInt(0xffff0007);
                    int lengthOffset = data.dataPosition();
                    data.writeInt(0);
                    int start = data.dataPosition();
                    data.writeString("synthetic-node");
                    int end = data.dataPosition();
                    data.setDataPosition(lengthOffset);
                    data.writeInt(end - start);
                    data.setDataPosition(end);
                    data.writeInt(0x00080008);
                    data.writeLong(1234567890123L);
                }
                int end = data.dataPosition();
                data.setDataPosition(4);
                data.writeInt(end - 8);
                data.setDataPosition(end);
                data.writeInt(0x2843);
                data.setDataPosition(0);
                ConsentResponse result = ConsentResponse.CREATOR.createFromParcel(data);
                assertEquals(10, result.statusCode);
                assertTrue(result.hasTosConsent);
                assertFalse(result.hasLoggingConsent);
                assertFalse(result.hasCloudSyncConsent);
                assertFalse(result.hasLocationConsent);
                assertNotNull(result.accountConsentRecords);
                assertTrue(result.accountConsentRecords.isEmpty());
                assertEquals(nullable ? null : "synthetic-node", result.nodeId);
                assertEquals(nullable ? null : Long.valueOf(1234567890123L), result.lastUpdateRequestedTime);
                assertEquals(0x2843, data.readInt());
                assertEquals(0, data.dataAvail());
            } finally { data.recycle(); }
        }
    }

    public void testServiceProxyUsesBinder65And66WithOfficialRequestFields() throws RemoteException {
        AtomicInteger expected = new AtomicInteger(65);
        AtomicInteger calls = new AtomicInteger();
        IWearableService proxy = IWearableService.Stub.asInterface(new Binder() {
            @Override protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) {
                assertEquals(expected.get(), code);
                assertEquals(0, flags);
                data.enforceInterface(SERVICE);
                assertNotNull(data.readStrongBinder());
                if (code == 66) {
                    assertEquals(1, data.readInt());
                    assertEquals(0xffff4f45, data.readInt());
                    int end = data.readInt() + data.dataPosition();
                    int seen = 0;
                    while (data.dataPosition() < end) {
                        int header = data.readInt();
                        int field = header & 0xffff;
                        int size = (header >>> 16) == 0xffff ? data.readInt() : header >>> 16;
                        int next = data.dataPosition() + size;
                        if (field == 1) {
                            assertEquals("fixture@example.invalid", data.readString());
                            seen |= 1;
                        } else if (field == 2) {
                            assertEquals(1, data.readInt());
                            seen |= 2;
                        } else fail("Unexpected account request field");
                        data.setDataPosition(next);
                    }
                    assertEquals(3, seen);
                }
                assertEquals(0, data.dataAvail());
                calls.incrementAndGet();
                reply.writeNoException();
                return true;
            }
        });
        proxy.getConsent(new BaseWearableCallbacks());
        expected.set(66);
        proxy.addAccountToConsent(new BaseWearableCallbacks(), new AddAccountToConsentRequest("fixture@example.invalid", true));
        assertEquals(2, calls.get());
    }

    public void testConsentCallbackUsesBinder38() throws RemoteException {
        AtomicInteger calls = new AtomicInteger();
        IWearableCallbacks proxy = IWearableCallbacks.Stub.asInterface(new Binder() {
            @Override protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) {
                assertEquals(38, code);
                assertEquals(0, flags);
                data.enforceInterface(CALLBACK);
                assertEquals(1, data.readInt());
                readResponseFields(data, 8);
                assertEquals(0, data.dataAvail());
                calls.incrementAndGet();
                reply.writeNoException();
                return true;
            }
        });
        proxy.onConsentResponse(response());
        assertEquals(1, calls.get());
    }

    private void deniedCallerCannotReadOrAssociateConsent(String packageName) throws Exception {
        assertEquals("org.microg.gms.wearable.core.test", getContext().getPackageName());
        getContext().deleteDatabase("node.db");
        getContext().deleteDatabase("connectionconfig.db");
        File consentDatabase = new File(getContext().getNoBackupFilesDir(), "wearable-consent.db");
        SQLiteDatabase.deleteDatabase(consentDatabase);
        NodeDatabaseHelper nodes = new NodeDatabaseHelper(getContext());
        ConfigurationDatabaseHelper configurations = new ConfigurationDatabaseHelper(getContext());
        WearableImpl implementation = null;
        try (WearableConsentStore consent = new WearableConsentStore(getContext())) {
            assertNull(consent.read());
            implementation = new WearableImpl(getContext(), nodes, configurations);
            WearableServiceImpl service = new WearableServiceImpl(getContext(), implementation, packageName);
            AtomicInteger expected = new AtomicInteger(65);
            IWearableService proxy = IWearableService.Stub.asInterface(new Binder() {
                @Override protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
                    assertEquals(expected.get(), code);
                    return service.onTransact(code, data, reply, flags);
                }
            });
            AtomicInteger readCalls = new AtomicInteger();
            proxy.getConsent(new BaseWearableCallbacks() {
                @Override public void onConsentResponse(ConsentResponse result) {
                    assertEquals(10, result.statusCode);
                    assertFalse(result.hasTosConsent);
                    assertFalse(result.hasLoggingConsent);
                    assertFalse(result.hasCloudSyncConsent);
                    assertFalse(result.hasLocationConsent);
                    assertNotNull(result.accountConsentRecords);
                    assertTrue(result.accountConsentRecords.isEmpty());
                    assertNull(result.nodeId);
                    assertNull(result.lastUpdateRequestedTime);
                    readCalls.incrementAndGet();
                }
            });
            assertEquals("Denial must complete, not leave the SDK Task pending", 1, readCalls.get());
            expected.set(66);
            AtomicInteger writeCalls = new AtomicInteger();
            for (boolean hasConsent : new boolean[]{false, true}) {
                proxy.addAccountToConsent(new BaseWearableCallbacks() {
                    @Override public void onStatus(Status result) {
                        assertEquals(10, result.getStatusCode());
                        writeCalls.incrementAndGet();
                    }
                }, new AddAccountToConsentRequest("fixture@example.invalid", hasConsent));
            }
            assertEquals(2, writeCalls.get());
            assertEquals(1, readCalls.get());
            assertNull(consent.read());
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
            SQLiteDatabase.deleteDatabase(consentDatabase);
        }
    }

    public void testOrdinaryCallerCannotReadOrAssociateConsent() throws Exception {
        deniedCallerCannotReadOrAssociateConsent(getContext().getPackageName());
    }

    public void testBorrowedCompanionPackageCannotReadOrAssociateConsent() throws Exception {
        deniedCallerCannotReadOrAssociateConsent("com.google.android.apps.wear.companion");
    }
}
