/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.nearby.exposurenotification.internal;

interface IDiagnosisKeyFileSupplier {
    boolean hasNext();
    ParcelFileDescriptor next();
    boolean isAvailable();
}
