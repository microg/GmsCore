/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.phone;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.api.internal.IStatusCallback;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

public class SmsRetrieverServiceTest {

    @Test
    public void testStartSmsCodeAutofillReturnsSuccess() throws Exception {
        SmsRetrieverServiceImpl service = new SmsRetrieverServiceImpl(null, "com.google.android.apps.messaging", null);
        final AtomicReference<Status> result = new AtomicReference<>();
        service.startSmsCodeAutofill(new IStatusCallback.Stub() {
            @Override
            public void onResult(Status status) {
                result.set(status);
            }
        });
        Assert.assertNotNull("Callback must be invoked", result.get());
        Assert.assertTrue("Status must be SUCCESS for Google Messages RCS autofill to proceed", result.get().isSuccess());
    }

    @Test
    public void testStartSmsCodeBrowserReturnsSuccess() throws Exception {
        SmsRetrieverServiceImpl service = new SmsRetrieverServiceImpl(null, "com.google.android.apps.messaging", null);
        final AtomicReference<Status> result = new AtomicReference<>();
        service.startSmsCodeBrowser(new IStatusCallback.Stub() {
            @Override
            public void onResult(Status status) {
                result.set(status);
            }
        });
        Assert.assertNotNull("Callback must be invoked", result.get());
        Assert.assertTrue("Status must be SUCCESS for browser SMS autofill", result.get().isSuccess());
    }
}

