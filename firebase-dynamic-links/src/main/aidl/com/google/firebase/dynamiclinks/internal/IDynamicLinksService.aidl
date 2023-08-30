/*
 * SPDX-FileCopyrightText: 2019, e Foundation
 * SPDX-FileCopyrightText: 2021, Google LLC
 * SPDX-FileCopyrightText: 2021, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.firebase.dynamiclinks.internal;

import com.google.firebase.dynamiclinks.internal.IDynamicLinksCallbacks;
import android.os.Bundle;

interface IDynamicLinksService {
    void getInitialLink(IDynamicLinksCallbacks callback, String link) = 0;
    void createShortDynamicLink(IDynamicLinksCallbacks callback, in Bundle extras) = 1;
}
