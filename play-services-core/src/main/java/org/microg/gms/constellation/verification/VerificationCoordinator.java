/*
 * Copyright (C) 2013-2026 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.microg.gms.constellation.verification;

import android.os.Bundle;
import android.os.RemoteException;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.constellation.ApiMetadata;
import com.google.android.gms.constellation.PhoneNumberInfo;
import com.google.android.gms.constellation.VerifyPhoneNumberRequest;
import com.google.android.gms.constellation.VerifyPhoneNumberResponse;
import com.google.android.gms.constellation.internal.IConstellationCallbacks;

import java.util.Collections;

/**
 * Coordinates phone number verification attempts across SMS, TS.43, carrier-ID,
 * flash-call, and other methods.
 *
 * The full orchestration is TODO_PROTOCOL_DISCOVERY; the coordinator currently
 * reports INTERNAL_ERROR so callers do not hang while the protocol is resolved.
 */
public class VerificationCoordinator {

    /**
     * Legacy v1 entry point that accepts an opaque Bundle.
     */
    public void verifyPhoneNumberV1(IConstellationCallbacks cb, Bundle params, ApiMetadata apiMetadata) throws RemoteException {
        cb.onPhoneNumberVerified(Status.INTERNAL_ERROR, Collections.<PhoneNumberInfo>emptyList(), apiMetadata);
    }

    /**
     * Single-use verification entry point that accepts an opaque Bundle.
     */
    public void verifyPhoneNumberSingleUse(IConstellationCallbacks cb, Bundle params, ApiMetadata apiMetadata) throws RemoteException {
        cb.onPhoneNumberVerificationsCompleted(Status.INTERNAL_ERROR,
                new VerifyPhoneNumberResponse(new VerifyPhoneNumberResponse.PhoneNumberVerification[0], null),
                apiMetadata);
    }

    /**
     * Structured verification entry point.
     */
    public void verifyPhoneNumber(IConstellationCallbacks cb, VerifyPhoneNumberRequest request, ApiMetadata apiMetadata) throws RemoteException {
        cb.onPhoneNumberVerificationsCompleted(Status.INTERNAL_ERROR,
                new VerifyPhoneNumberResponse(new VerifyPhoneNumberResponse.PhoneNumberVerification[0], null),
                apiMetadata);
    }
}
