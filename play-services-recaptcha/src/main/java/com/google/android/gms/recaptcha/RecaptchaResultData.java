/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.recaptcha;

import org.microg.safeparcel.AutoSafeParcelable;

/**
 * Information pertaining to reCAPTCHA result data.
 */
public class RecaptchaResultData extends AutoSafeParcelable {
    @Field(1)
    private String tokenResult;

    private RecaptchaResultData() {
    }

    public RecaptchaResultData(String token) {
        this.tokenResult = token;
    }

    /**
     * Returns a reCAPTCHA token.
     */
    public String getTokenResult() {
        return tokenResult;
    }

    public static final Creator<RecaptchaResultData> CREATOR = new AutoCreator<>(RecaptchaResultData.class);
}
