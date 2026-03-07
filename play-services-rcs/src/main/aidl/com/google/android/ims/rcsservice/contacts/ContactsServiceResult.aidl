/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.ims.rcsservice.contacts;

parcelable ContactsServiceResult {
    int code;
    String description;
    
    // Success/error codes
    const int SUCCESS = 0;
    const int ERROR_UNKNOWN = 1;
    const int ERROR_INVALID_DESTINATION = 11;
}
