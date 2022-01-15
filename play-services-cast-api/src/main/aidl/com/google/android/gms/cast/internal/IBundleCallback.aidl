/*
 * SPDX-FileCopyrightText: 2021, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.cast.internal;

interface IBundleCallback {
  oneway void onBundle(in Bundle bundle);
}
