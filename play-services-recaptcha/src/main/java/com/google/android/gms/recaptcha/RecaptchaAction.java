/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.recaptcha;

import android.os.Bundle;

import androidx.annotation.NonNull;

import org.microg.safeparcel.AutoSafeParcelable;

/**
 * Actions (e.g., login) intended to be protected by reCAPTCHA. An instance of this object should be passed to
 * {@link RecaptchaClient#execute(RecaptchaHandle, RecaptchaAction)}.
 */
public class RecaptchaAction extends AutoSafeParcelable {
    @Field(1)
    private RecaptchaActionType action;
    @Field(2)
    private String customAction;
    @Field(3)
    private Bundle additionalArgs;
    @Field(4)
    private String verificationHistoryToken;

    private RecaptchaAction() {
    }

    /**
     * Creates a {@link RecaptchaAction} instance with a predefined reCAPTCHA action.
     */
    public RecaptchaAction(RecaptchaActionType action) {
        this(action, Bundle.EMPTY);
    }

    /**
     * Creates a {@link RecaptchaAction} instance with a predefined reCAPTCHA action and additional arguments.
     */
    public RecaptchaAction(RecaptchaActionType action, Bundle additionalArgs) {
        this.action = action;
        this.additionalArgs = additionalArgs;
    }

    /**
     * Creates a {@link RecaptchaAction} instance with a custom action String.
     */
    public RecaptchaAction(String customAction) {
        this(customAction, Bundle.EMPTY);
    }

    /**
     * Creates a {@link RecaptchaAction} instance with a custom action in the form of a String and additional arguments.
     */
    public RecaptchaAction(String customAction, Bundle additionalArgs) {
        this.action = new RecaptchaActionType(RecaptchaActionType.OTHER);
        this.customAction = customAction;
        this.additionalArgs = additionalArgs;
    }

    /**
     * Gets {@code RecaptchaActionType}.
     */
    public RecaptchaActionType getAction() {
        return action;
    }

    /**
     * Gets the additional arg map specified by this action.
     */
    public Bundle getAdditionalArgs() {
        return additionalArgs;
    }

    /**
     * Gets custom action that user inputs.
     */
    public String getCustomAction() {
        return customAction;
    }

    /**
     * Gets the verification history token specified by this action.
     */
    public String getVerificationHistoryToken() {
        return verificationHistoryToken;
    }

    /**
     * Gets the String value of {@code RecaptchaAction}.
     */
    @NonNull
    @Override
    public String toString() {
        if (RecaptchaActionType.OTHER.equals(action.name) && customAction != null) {
            return customAction;
        } else {
            return action.name;
        }
    }

    public static final Creator<RecaptchaAction> CREATOR = new AutoCreator<>(RecaptchaAction.class);
}
