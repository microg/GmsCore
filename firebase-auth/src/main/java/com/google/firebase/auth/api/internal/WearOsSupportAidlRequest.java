/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.firebase.auth.api.internal;

import org.microg.safeparcel.AutoSafeParcelable;

/**
 * Placeholder class for WearOS support request.
 * This class can be extended in the future to add WearOS specific authentication features.
 */
public class WearOsSupportAidlRequest extends AutoSafeParcelable {
    public static final Creator<WearOsSupportAidlRequest> CREATOR = new AutoCreator<>(WearOsSupportAidlRequest.class);
}
