/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.auth.folsom.internal;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.api.ApiMetadata;
import com.google.android.gms.auth.folsom.SecurityDomainMember;

interface ISecurityDomainMembersCallback {
    void onResult(in Status status, in SecurityDomainMember[] members, in ApiMetadata apiMetadata);
}
