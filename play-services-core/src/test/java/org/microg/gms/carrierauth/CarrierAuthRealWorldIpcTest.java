package org.microg.gms.carrierauth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;

import android.content.Context;
import android.os.Build;
import android.os.IBinder;
import android.os.Parcel;
import android.telephony.TelephonyManager;

import com.google.android.gms.carrierauth.EAPAKARequest;
import com.google.android.gms.carrierauth.EAPAKAResponse;
import com.google.android.gms.carrierauth.internal.ICarrierAuthCallbacks;
import com.google.android.gms.common.api.Status;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.microg.gms.common.PackageUtils;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Build.VERSION_CODES.S}, shadows = {ShadowTelephonyManager.class})
public class CarrierAuthRealWorldIpcTest {
    private static final String SERVICE_DESCRIPTOR = "com.google.android.gms.carrierauth.internal.ICarrierAuthService";
    private static final String GOOGLE_MESSAGES_PACKAGE = "com.google.android.apps.messaging";
    private static final String GOOGLE_MESSAGES_SIGNATURE = "24bb24c05e47e0aefa68a58a766179d9b613a600";
    
    @Test
    public void googleMessagesRealWorldEapAkaAuthenticationFlow() throws Exception {
        // 1. Setup the service exactly as Android would (simulating the Service lifecycle)
        CarrierAuthService service = org.robolectric.Robolectric.setupService(CarrierAuthService.class);
        
        // 2. Get the raw Binder stub that Google Messages will transact with across process boundaries
        Field field = CarrierAuthService.class.getDeclaredField("serviceStub");
        field.setAccessible(true);
        IBinder serviceBinder = (IBinder) field.get(service);
        
        // 3. Mock the hardware modem environment (TelephonyManager)
        ShadowTelephonyManager.setCarrierPrivileges(7, true); // Subscription ID 7
        ShadowTelephonyManager.setMockAuthResponse(TelephonyManager.APPTYPE_USIM, "real-world-modem-token-123456");

        try (org.mockito.MockedStatic<PackageUtils> packageUtils = mockStatic(PackageUtils.class)) {
            // Faking that the caller UID belongs to Google Messages (Bypassing Android SecurityException)
            packageUtils.when(() -> PackageUtils.checkPackageUid(
                    any(Context.class), eq(GOOGLE_MESSAGES_PACKAGE), eq(android.os.Binder.getCallingUid())
            )).thenAnswer(invocation -> null); 
            
            // Faking that the caller's signature matches the official Google Messages SHA1 hash
            packageUtils.when(() -> PackageUtils.firstSignatureDigest(
                    any(Context.class), eq(GOOGLE_MESSAGES_PACKAGE), any(Boolean.class)
            )).thenReturn(GOOGLE_MESSAGES_SIGNATURE);
            
            // 4. Create the callback stub that Google Messages registers to receive the async token
            AtomicReference<EAPAKAResponse> receivedResponse = new AtomicReference<>();
            AtomicReference<Status> receivedStatus = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(1);
            
            ICarrierAuthCallbacks.Stub callbackStub = new ICarrierAuthCallbacks.Stub() {
                @Override
                public void onEAPAKAResponse(Status status, EAPAKAResponse response, com.google.android.gms.common.api.ApiMetadata metadata) {
                    receivedStatus.set(status);
                    receivedResponse.set(response);
                    latch.countDown();
                }
                @Override
                public void onEapInfoResponse(Status status, com.google.android.gms.carrierauth.EapInfoResponse response, com.google.android.gms.common.api.ApiMetadata metadata) {}
            };
            
            // 5. Build the raw IPC Parcel EXACTLY as Android's Binder does it across apps!
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            try {
                // Write the interface token that was previously crashing due to mismatches
                data.writeInterfaceToken(SERVICE_DESCRIPTOR);
                
                // Write the Callback Binder
                data.writeStrongBinder(callbackStub.asBinder());
                
                // Write EAPAKARequest (present = 1)
                data.writeInt(1);
                // The SafeParcelReader/Writer bug used to crash here! Now it perfectly serializes.
                EAPAKARequest request = new EAPAKARequest("real-world-challenge-string", null, null, null, 7);
                request.writeToParcel(data, 0);
                
                // Write ApiMetadata (present = 0)
                data.writeInt(0);
                
                // 6. Execute the FIRST_CALL_TRANSACTION (performEAPAKA) across the Binder boundary!
                boolean transactSuccess = serviceBinder.transact(IBinder.FIRST_CALL_TRANSACTION, data, reply, 0);
                
                assertTrue("IPC Transaction should succeed", transactSuccess);
                reply.readException(); // Ensure no exception was thrown by the stub
                
            } finally {
                data.recycle();
                reply.recycle();
            }
            
            // 7. Verify the response! (Wait for the async callback from CarrierAuthServiceImpl)
            assertTrue("Callback should be invoked", latch.await(2, TimeUnit.SECONDS));
            
            assertNotNull("Status should not be null", receivedStatus.get());
            assertTrue("Status should be SUCCESS", receivedStatus.get().isSuccess());
            
            assertNotNull("Response should not be null", receivedResponse.get());
            assertEquals("The modem token should match exactly what TelephonyManager returned", 
                    "real-world-modem-token-123456", receivedResponse.get().a);
        }
    }
}
