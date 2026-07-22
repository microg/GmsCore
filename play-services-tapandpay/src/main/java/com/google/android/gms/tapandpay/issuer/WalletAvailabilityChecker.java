/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.tapandpay.issuer;

import androidx.annotation.NonNull;

public interface WalletAvailabilityChecker {
    boolean isAvailable(@NonNull String walletId);
}
