package org.microg.gms.wearable;

import android.os.RemoteException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.internal.ConnectionInfo;
import com.google.android.gms.common.internal.GetServiceRequest;
import com.google.android.gms.common.internal.IGmsCallbacks;
import com.google.android.gms.common.internal.GmsService;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class WearableServiceTest {
    private static class TestCallbacks extends IGmsCallbacks.Stub {
        boolean called = false;
        @Override
        public void onPostInitComplete(int statusCode, android.os.IBinder service) throws RemoteException {
            // not used in this test
        }
        @Override
        public void onPostInitCompleteWithConnectionInfo(int statusCode, android.os.IBinder service, ConnectionInfo connectionInfo) throws RemoteException {
            called = true;
            assertEquals(CommonStatusCodes.SUCCESS, statusCode);
        }
    }

    private WearableService service;
    private TestCallbacks callbacks;

    @Before
    public void setUp() {
        service = new WearableService();
        // Initialise the service to set up internal WearableImpl (uses a null context which is safe for this test)
        service.onCreate();
        callbacks = new TestCallbacks();
    }

    @Test
    public void testHandleServiceRequest_nullPackageName() throws Exception {
        GetServiceRequest request = new GetServiceRequest(0);
        request.packageName = null; // simulate missing package name
        service.handleServiceRequest(callbacks, request, GmsService.WEAR);
        assertTrue("Callback should have been invoked", callbacks.called);
    }
}
