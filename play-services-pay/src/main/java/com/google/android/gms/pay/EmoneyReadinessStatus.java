/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.pay;

import androidx.annotation.IntDef;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * E-money readiness status based on service provider and account.
 */
@Target({ElementType.TYPE_USE})
@Retention(RetentionPolicy.SOURCE)
@IntDef({EmoneyReadinessStatus.FEATURE_NOT_SUPPORTED, EmoneyReadinessStatus.READY, EmoneyReadinessStatus.APP_NOT_INSTALLED, EmoneyReadinessStatus.APP_UPGRADE_NEEDED, EmoneyReadinessStatus.NO_ACTIVE_ACCOUNT, EmoneyReadinessStatus.ACCOUNT_MISMATCH})
public @interface EmoneyReadinessStatus {
    /**
     * Indicates that the e-money feature is not supported yet or still waiting for launch.
     */
    int FEATURE_NOT_SUPPORTED = 0;
    /**
     * Indicates that the e-money feature is available and ready to be used.
     */
    int READY = 1;
    /**
     * Indicates that the Google Wallet app is not installed.
     */
    int APP_NOT_INSTALLED = 2;
    /**
     * Indicates that the current Google Wallet app version or the Google Play services version needs to be upgraded.
     */
    int APP_UPGRADE_NEEDED = 3;
    /**
     * Indicates that there is no active account currently in Google Wallet.
     */
    int NO_ACTIVE_ACCOUNT = 4;
    /**
     * Indicates that the provided account does not match the active account currently in Google Wallet.
     */
    int ACCOUNT_MISMATCH = 5;
}
