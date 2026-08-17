/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.threadnetwork;

import androidx.annotation.IntDef;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks the result of {@link ThreadNetworkClient#isPreferredCredentials(ThreadNetworkCredentials)}.
 */
@Target({ElementType.TYPE_PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.SOURCE)
@IntDef({IsPreferredCredentialsResult.PREFERRED_CREDENTIALS_NOT_FOUND, IsPreferredCredentialsResult.PREFERRED_CREDENTIALS_NOT_MATCHED, IsPreferredCredentialsResult.PREFERRED_CREDENTIALS_MATCHED})
public @interface IsPreferredCredentialsResult {
    /**
     * The preferred Thread network credentials don't exist.
     */
    int PREFERRED_CREDENTIALS_NOT_FOUND = -1;
    /**
     * The preferred Thread network credentials don't match given credentials.
     */
    int PREFERRED_CREDENTIALS_NOT_MATCHED = 0;
    /**
     * The preferred Thread network credentials match given credentials.
     */
    int PREFERRED_CREDENTIALS_MATCHED = 1;
}
