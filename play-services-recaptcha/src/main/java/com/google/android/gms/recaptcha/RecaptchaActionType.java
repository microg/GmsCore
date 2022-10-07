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
 * Collection of predefined actions used by RecaptchaHandle.
 */
public class RecaptchaActionType extends AutoSafeParcelable {
    @Field(1)
    String name;

    private RecaptchaActionType() {}

    public RecaptchaActionType(String action) {
        this.name = action;
    }

    public static final Creator<RecaptchaActionType> CREATOR = new AutoCreator<>(RecaptchaActionType.class);

    /**
     * User interaction that needs to be verified while the user is performing the workflow you would like to protect.
     */
    public @interface Action {}

    /**
     * Indicates that the protected action is a login workflow.
     */
    public static final String LOGIN = "login";
    /**
     * When a custom action is specified, reCAPTCHA uses this value automatically.
     */
    public static final String OTHER = "other";
    /**
     * Indicates that the protected action is a signup workflow.
     */
    public static final String SIGNUP = "signup";
}
