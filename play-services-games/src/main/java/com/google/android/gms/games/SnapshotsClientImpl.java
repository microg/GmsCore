/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.games;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApi;
import com.google.android.gms.games.snapshot.Snapshot;
import com.google.android.gms.games.snapshot.SnapshotContents;
import com.google.android.gms.games.snapshot.SnapshotMetadata;
import com.google.android.gms.games.snapshot.SnapshotMetadataBuffer;
import com.google.android.gms.games.snapshot.SnapshotMetadataChange;
import com.google.android.gms.tasks.Task;

public class SnapshotsClientImpl extends GoogleApi<Games.GamesOptions> implements SnapshotsClient{

    public SnapshotsClientImpl(Context context, Games.GamesOptions options) {
        super(context, Games.API, options);
        Log.d(Games.TAG, "SnapshotsClientImpl: options: " + options);
    }

    @Override
    public Task<SnapshotMetadata> commitAndClose(Snapshot snapshot, SnapshotMetadataChange metadataChange) {
        return null;
    }

    @Override
    public Task<String> delete(SnapshotMetadata metadata) {
        return null;
    }

    @Override
    public Task<Void> discardAndClose(Snapshot snapshot) {
        return null;
    }

    @Override
    public Task<Integer> getMaxCoverImageSize() {
        return null;
    }

    @Override
    public Task<Integer> getMaxDataSize() {
        return null;
    }

    @Override
    public Task<Intent> getSelectSnapshotIntent(String title, boolean allowAddButton, boolean allowDelete, int maxSnapshots) {
        return null;
    }

    @Override
    public SnapshotMetadata getSnapshotFromBundle(Bundle extras) {
        return null;
    }

    @Override
    public Task<AnnotatedData<SnapshotMetadataBuffer>> load(boolean forceReload) {
        return null;
    }

    @Override
    public Task<DataOrConflict<Snapshot>> open(SnapshotMetadata metadata) {
        return null;
    }

    @Override
    public Task<DataOrConflict<Snapshot>> open(SnapshotMetadata metadata, int conflictPolicy) {
        return null;
    }

    @Override
    public Task<DataOrConflict<Snapshot>> open(String fileName, boolean createIfNotFound, int conflictPolicy) {
        return null;
    }

    @Override
    public Task<DataOrConflict<Snapshot>> open(String fileName, boolean createIfNotFound) {
        return null;
    }

    @Override
    public Task<DataOrConflict<Snapshot>> resolveConflict(String conflictId, String snapshotId, SnapshotMetadataChange metadataChange, SnapshotContents snapshotContents) {
        return null;
    }

    @Override
    public Task<DataOrConflict<Snapshot>> resolveConflict(String conflictId, Snapshot snapshot) {
        return null;
    }
}
