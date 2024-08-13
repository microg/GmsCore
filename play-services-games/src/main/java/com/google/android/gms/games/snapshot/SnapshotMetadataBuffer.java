/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.games.snapshot;

import com.google.android.gms.common.data.AbstractDataBuffer;
import com.google.android.gms.common.data.DataHolder;

public final class SnapshotMetadataBuffer extends AbstractDataBuffer<SnapshotMetadata> {

    public SnapshotMetadataBuffer(DataHolder dataHolder) {
        super(dataHolder);
    }

    public SnapshotMetadata get(int position) {
        return new SnapshotMetadataRef(this.dataHolder, position);
    }
}
