/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.gms.wearable;

import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.test.AndroidTestCase;

import com.google.android.gms.wearable.MessageOptions;
import com.google.android.gms.wearable.internal.*;

import java.util.Arrays;

/** Raw Binder/Parcel contracts: public Wearable SDK 20.0.1 and companion MessageOptions extension. */
public class RpcBinderContractTest extends AndroidTestCase {
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

    private void request(Parcel data) {
        data.enforceInterface("com.google.android.gms.wearable.internal.IWearableService");
        assertNotNull(data.readStrongBinder());
        assertEquals("watch", data.readString());
        assertEquals("/rpc", data.readString());
        assertTrue(Arrays.equals(new byte[]{1, 2}, data.createByteArray()));
    }

    public void testPublicSendRequestUses58() throws RemoteException {
        final boolean[] called = {false};
        IWearableService service = IWearableService.Stub.asInterface(endpoint((code, data, flags) -> {
            called[0] = true;
            assertEquals(58, code);
            request(data);
            assertEquals(0, data.dataAvail());
        }));
        service.sendRequest(new BaseWearableCallbacks(), "watch", "/rpc", new byte[]{1, 2});
        assertTrue(called[0]);
    }

    public void testCompanionSendRequestUses60AndPriorityField2() throws RemoteException {
        final boolean[] called = {false};
        IWearableService service = IWearableService.Stub.asInterface(endpoint((code, data, flags) -> {
            called[0] = true;
            assertEquals(60, code);
            request(data);
            assertEquals(1, data.readInt());
            assertEquals(0xffff4f45, data.readInt());
            int end = data.readInt() + data.dataPosition();
            assertEquals(0x00040002, data.readInt());
            assertEquals(1, data.readInt());
            assertEquals(end, data.dataPosition());
            assertEquals(0, data.dataAvail());
        }));
        service.sendRequestWithOptions(new BaseWearableCallbacks(), "watch", "/rpc", new byte[]{1, 2}, new MessageOptions(1));
        assertTrue(called[0]);
    }

    public void testRpcCompletionUses34AndFieldsOneTwoThree() throws RemoteException {
        final boolean[] called = {false};
        IWearableCallbacks callback = IWearableCallbacks.Stub.asInterface(endpoint((code, data, flags) -> {
            called[0] = true;
            assertEquals(34, code);
            data.enforceInterface("com.google.android.gms.wearable.internal.IWearableCallbacks");
            assertEquals(1, data.readInt());
            assertEquals(0xffff4f45, data.readInt());
            int end = data.readInt() + data.dataPosition();
            int fields = 0;
            while (data.dataPosition() < end) {
                int header = data.readInt();
                int id = header & 0xffff;
                int size = (header >>> 16) == 0xffff ? data.readInt() : header >>> 16;
                int next = data.dataPosition() + size;
                if (id == 1) {
                    assertEquals(4, size);
                    assertEquals(0, data.readInt());
                    fields |= 1;
                } else if (id == 2) {
                    assertEquals(4, size);
                    assertEquals(16406, data.readInt());
                    fields |= 2;
                } else if (id == 3) {
                    assertTrue(Arrays.equals(new byte[]{9}, data.createByteArray()));
                    fields |= 4;
                } else {
                    fail("Unexpected RpcResponse field " + id);
                }
                assertEquals(next, data.dataPosition());
            }
            assertEquals(7, fields);
            assertEquals(end, data.dataPosition());
        }));
        callback.onRpcResponse(new RpcResponse(0, 16406, new byte[]{9}));
        assertTrue(called[0]);
    }

    public void testListenerRequestUses13WithResponseBinder() throws RemoteException {
        final boolean[] called = {false};
        IWearableListener listener = IWearableListener.Stub.asInterface(endpoint((code, data, flags) -> {
            called[0] = true;
            assertEquals(13, code);
            data.enforceInterface("com.google.android.gms.wearable.internal.IWearableListener");
            assertEquals(1, data.readInt());
            MessageEventParcelable event = MessageEventParcelable.CREATOR.createFromParcel(data);
            assertEquals(16406, event.requestId);
            assertEquals("watch", event.sourceNodeId);
            assertEquals("/rpc", event.path);
            assertNotNull(data.readStrongBinder());
            assertEquals(0, data.dataAvail());
        }));
        MessageEventParcelable event = new MessageEventParcelable();
        event.requestId = 16406;
        event.sourceNodeId = "watch";
        event.path = "/rpc";
        event.data = new byte[0];
        listener.onRequestReceived(event, new IRpcResponseCallback.Stub() {
            @Override public void onResponse(boolean success, byte[] data) { }
        });
        assertTrue(called[0]);
    }

    public void testResponseBinderUsesOneWay1BooleanThenBytes() throws RemoteException {
        final boolean[] called = {false};
        IRpcResponseCallback callback = IRpcResponseCallback.Stub.asInterface(endpoint((code, data, flags) -> {
            called[0] = true;
            assertEquals(1, code);
            assertEquals(IBinder.FLAG_ONEWAY, flags);
            data.enforceInterface("com.google.android.gms.wearable.internal.IRpcResponseCallback");
            assertEquals(1, data.readInt());
            assertTrue(Arrays.equals(new byte[]{42}, data.createByteArray()));
            assertEquals(0, data.dataAvail());
        }));
        callback.onResponse(true, new byte[]{42});
        assertTrue(called[0]);
    }
}
