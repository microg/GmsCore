/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.common;

import android.app.Activity;
import android.content.Intent;

/**
 * UserRecoverableExceptions signal errors that can be recovered with user action, such as a user login.
 */
public class UserRecoverableException extends Exception {
    private final Intent intent;

    public UserRecoverableException(String message, Intent intent) {
        super(message);
        this.intent = intent;
    }

    /**
     * Getter for an {@link Intent} that when supplied to {@link Activity#startActivityForResult(Intent, int)}, will allow user intervention.
     * @return Intent representing the ameliorating user action.
     */
    public Intent getIntent() {
        return intent;
    }
}
