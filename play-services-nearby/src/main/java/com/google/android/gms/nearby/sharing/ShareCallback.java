/*
 * SPDX-FileCopyrightText: 2023, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.nearby.sharing;

import org.microg.gms.common.PublicApi;

@PublicApi
public interface ShareCallback {
    void onShareTargetDiscovered(ShareTarget shareTarget);

    void onShareTargetLost(ShareTarget shareTarget);

    void onTransferUpdate(ShareTarget shareTarget, TransferMetadata metadata);
}
