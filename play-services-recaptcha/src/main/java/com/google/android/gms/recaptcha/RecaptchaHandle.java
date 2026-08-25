/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.recaptcha;

import org.microg.gms.common.PublicApi;
import org.microg.safeparcel.AutoSafeParcelable;

import java.util.Collections;
import java.util.List;

/**
 * Information pertaining to reCAPTCHA handle, which is used to identify the initialized reCAPTCHA service.
 */
public class RecaptchaHandle extends AutoSafeParcelable {
    @Field(1)
    private String siteKey;
    @Field(2)
    private String clientPackageName;
    @Field(3)
    private List<String> acceptableAdditionalArgs;

    @PublicApi(exclude = true)
    private RecaptchaHandle() {
    }

    @PublicApi(exclude = true)
    public RecaptchaHandle(String siteKey, String clientPackageName, List<String> acceptableAdditionalArgs) {
        this.siteKey = siteKey;
        this.clientPackageName = clientPackageName;
        this.acceptableAdditionalArgs = acceptableAdditionalArgs;
    }

    /**
     * Returns a list of strings indicating the additional argument keys that reCAPTCHA server accepts.
     */
    public List<String> getAcceptableAdditionalArgs() {
        return Collections.unmodifiableList(acceptableAdditionalArgs);
    }

    /**
     * Returns the package name of the app that calls reCAPTCHA API.
     */
    public String getClientPackageName() {
        return clientPackageName;
    }

    /**
     * Returns the reCAPTCHA Site Key you registered to help protect your application.
     */
    public String getSiteKey() {
        return siteKey;
    }

    public static final Creator<RecaptchaHandle> CREATOR = new AutoCreator<>(RecaptchaHandle.class);
}
