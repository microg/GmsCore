/*
 * SPDX-FileCopyrightText: 2019, e Foundation
 * SPDX-FileCopyrightText: 2021, Google LLC
 * SPDX-FileCopyrightText: 2021, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.firebase.dynamiclinks.internal;

import com.google.android.gms.common.api.Status;
import com.google.firebase.dynamiclinks.internal.DynamicLinkData;
import com.google.firebase.dynamiclinks.internal.ShortDynamicLinkImpl;

interface IDynamicLinksCallbacks {
    void onStatusDynamicLinkData(in Status status, in DynamicLinkData dldata) = 0;
    void onStatusShortDynamicLink(in Status status, in ShortDynamicLinkImpl sdlink) = 1;
}
