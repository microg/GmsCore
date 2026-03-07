/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.ims.rcsservice.contacts;

import com.google.android.ims.rcsservice.contacts.ContactsServiceResult;
import com.google.android.ims.rcsservice.contacts.ImsCapabilities;

interface IContactsManagement {
    ContactsServiceResult forceRefreshCapabilities(String phoneNumber);
    ImsCapabilities getCachedCapabilities(String phoneNumber);
    ContactsServiceResult refreshCapabilities(String phoneNumber);
}
