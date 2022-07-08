/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.fido;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.google.android.gms.fido.fido2.api.common.AuthenticatorAssertionResponse;
import com.google.android.gms.fido.fido2.api.common.AuthenticatorAttestationResponse;
import com.google.android.gms.fido.fido2.api.common.AuthenticatorErrorResponse;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredential;
import com.google.android.gms.fido.fido2.Fido2ApiClient;
import com.google.android.gms.fido.fido2.Fido2PendingIntent;
import com.google.android.gms.fido.fido2.Fido2PrivilegedApiClient;
import com.google.android.gms.fido.sourcedevice.SourceDirectTransferClient;
import com.google.android.gms.fido.u2f.U2fApiClient;
import com.google.android.gms.fido.u2f.U2fPendingIntent;

import org.microg.gms.common.PublicApi;
import org.microg.gms.fido.api.FidoConstants;

/**
 * Entry point for Fido APIs.
 * <p>
 * FIDO (Fast IDentity Online), which is the industry alliance where Security Keys are being standardized.
 */
@PublicApi
public class Fido {
    /**
     * The key used by the calling {@link Activity} to retrieve {@link PublicKeyCredential} from the Intent received by
     * {@link Activity#onActivityResult(int, int, Intent)} after launching {@link Fido2PendingIntent}.
     */
    public static final String FIDO2_KEY_CREDENTIAL_EXTRA = FidoConstants.FIDO2_KEY_CREDENTIAL_EXTRA;

    /**
     * The key used by the calling {@link Activity} to retrieve {@link AuthenticatorErrorResponse} from the Intent received by
     * {@link Activity#onActivityResult(int, int, Intent)} after launching {@link Fido2PendingIntent}.
     *
     * @deprecated use {@link #FIDO2_KEY_CREDENTIAL_EXTRA} to fetch {@link PublicKeyCredential} instead.
     * {@link PublicKeyCredential} contains an {@link AuthenticatorErrorResponse}.
     */
    @Deprecated
    public static final String FIDO2_KEY_ERROR_EXTRA = FidoConstants.FIDO2_KEY_ERROR_EXTRA;

    /**
     * The key used by the calling {@link Activity} to retrieve {@link AuthenticatorAttestationResponse} or
     * {@link AuthenticatorAssertionResponse} from the Intent received by
     * {@link Activity#onActivityResult(int, int, Intent)} after launching {@link Fido2PendingIntent}.
     *
     * @deprecated use {@link #FIDO2_KEY_CREDENTIAL_EXTRA} to fetch {@link PublicKeyCredential} instead.
     * {@link PublicKeyCredential} contains an {@link AuthenticatorAttestationResponse} or an {@link AuthenticatorAssertionResponse}.
     */
    @Deprecated
    public static final String FIDO2_KEY_RESPONSE_EXTRA = FidoConstants.FIDO2_KEY_RESPONSE_EXTRA;

    /**
     * The key used by the calling {@link Activity} to retrieve {@link ResponseData} from the Intent received by
     * {@link Activity#onActivityResult(int, int, Intent)} after launching {@link Fido2PendingIntent}.
     */
    public static final String KEY_RESPONSE_EXTRA = FidoConstants.KEY_RESPONSE_EXTRA;

    /**
     * Creates a new instance of {@link Fido2ApiClient} for use in a non-activity {@link Context}.
     */
    public static Fido2ApiClient getFido2ApiClient(Context context) {
        return new Fido2ApiClient(context);
    }

    /**
     * Creates a new instance of {@link Fido2ApiClient} for use in an {@link Activity}.
     */
    public static Fido2ApiClient getFido2ApiClient(Activity activity) {
        return new Fido2ApiClient(activity);
    }

    /**
     * Creates a new instance of {@link Fido2PrivilegedApiClient} for use in a non-activity {@link Context}.
     */
    public static Fido2PrivilegedApiClient getFido2PrivilegedApiClient(Context context) {
        throw new UnsupportedOperationException();
    }

    /**
     * Creates a new instance of {@link Fido2PrivilegedApiClient} for use in an {@link Activity}.
     */
    public static Fido2PrivilegedApiClient getFido2PrivilegedApiClient(Activity activity) {
        throw new UnsupportedOperationException();
    }

    /**
     * Creates a new instance of {@link SourceDirectTransferClient} for use in a non-activity {@link Context}.
     */
    public static final SourceDirectTransferClient getSourceDirectTransferClient(Context context) {
        throw new UnsupportedOperationException();
    }

    /**
     * Creates a new instance of {@link SourceDirectTransferClient} for use in an {@link Activity}.
     */
    public static final SourceDirectTransferClient getSourceDirectTransferClient(Activity activity) {
        throw new UnsupportedOperationException();
    }

    /**
     * Creates a new instance of {@link U2fApiClient} for use in a non-activity {@link Context}.
     */
    public static U2fApiClient getU2fApiClient(Context context) {
        return new U2fApiClient(context);
    }

    /**
     * Creates a new instance of {@link U2fApiClient} for use in an {@link Activity}.
     */
    public static U2fApiClient getU2fApiClient(Activity activity) {
        return new U2fApiClient(activity);
    }
}
