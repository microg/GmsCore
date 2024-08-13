/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.games;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.games.snapshot.Snapshot;
import com.google.android.gms.games.snapshot.SnapshotContents;
import com.google.android.gms.games.snapshot.SnapshotMetadata;
import com.google.android.gms.games.snapshot.SnapshotMetadataBuffer;
import com.google.android.gms.games.snapshot.SnapshotMetadataChange;
import com.google.android.gms.tasks.Task;

/**
 * A client to interact with Snapshots.
 */
public interface SnapshotsClient {
    /**
     * Intent extra used to pass a {@link SnapshotMetadata}.
     */
    String EXTRA_SNAPSHOT_METADATA = "com.google.android.gms.games.SNAPSHOT_METADATA";
    /**
     * Intent extra used to indicate the user wants to create a new snapshot.
     */
    String EXTRA_SNAPSHOT_NEW = "com.google.android.gms.games.SNAPSHOT_NEW";
    /**
     * Constant passed to {@link SnapshotsClient#getSelectSnapshotIntent(String, boolean, boolean, int)} indicating that the UI should not cap the number of displayed snapshots.
     */
    int DISPLAY_LIMIT_NONE = -1;
    /**
     * In the case of a conflict, the snapshot with the highest progress value will be used.
     */
    int RESOLUTION_POLICY_HIGHEST_PROGRESS = 4;
    /**
     * In the case of a conflict, the last known good version of this snapshot will be used.
     */
    int RESOLUTION_POLICY_LAST_KNOWN_GOOD = 2;
    /**
     * In the case of a conflict, the snapshot with the longest played time will be used.
     */
    int RESOLUTION_POLICY_LONGEST_PLAYTIME = 1;
    /**
     * In the case of a conflict, the result will be returned to the app for resolution.
     */
    int RESOLUTION_POLICY_MANUAL = -1;
    /**
     * In the case of a conflict, the most recently modified version of this snapshot will be used.
     */
    int RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED = 3;

    /**
     * This method fails a Task with an exception when called with a snapshot that was not opened or has already been committed/discarded.
     * <p>
     * Note that the total size of the contents of snapshot may not exceed the size provided by getMaxDataSize().
     *
     * @param snapshot       The snapshot to commit the data for.
     * @param metadataChange The set of changes to apply to the metadata for the snapshot. Use SnapshotMetadataChange.EMPTY_CHANGE to preserve the existing metadata.
     * @return Returns a Task which asynchronously commits any modifications in SnapshotMetadataChange made to the Snapshot and loads a SnapshotMetadata. The Task returned by this method is complete once the changes are synced locally and the background sync request for this data has been requested.
     */
    Task<SnapshotMetadata> commitAndClose(Snapshot snapshot, SnapshotMetadataChange metadataChange);

    /**
     * @param metadata The metadata of the snapshot to delete.
     * @return Returns a Task which asynchronously deletes the specified by SnapshotMetadata snapshot and loads the deleted snapshot ID. This will delete the data of the snapshot locally and on the server.
     */
    Task<String> delete(SnapshotMetadata metadata);

    /**
     * This method fails a Task with an exception when called with a snapshot that was not opened or has already been committed/discarded.
     *
     * @param snapshot The snapshot to discard the data for.
     * @return Returns a Task which asynchronously discards the contents of the Snapshot and closes it. This will discard all changes made to the file, and close the snapshot to future changes until it is re-opened. The file will not be modified on the server.
     */
    Task<Void> discardAndClose(Snapshot snapshot);

    /**
     * The returned Task can fail with a RemoteException.
     *
     * @return Returns a Task which asynchronously loads the maximum data size per snapshot cover image in bytes. Guaranteed to be at least 800 KB. May increase in the future.
     */
    Task<Integer> getMaxCoverImageSize();

    /**
     * The returned Task can fail with a RemoteException.
     *
     * @return Returns a Task which asynchronously loads the maximum data size per snapshot in bytes. Guaranteed to be at least 3 MB. May increase in the future.
     */
    Task<Integer> getMaxDataSize();

    /**
     * The returned Task can fail with a RemoteException.
     * <p>
     * If the user canceled without selecting a snapshot, the result will be Activity.RESULT_CANCELED. If the user selected a snapshot from the list, the result will be Activity.RESULT_OK and the data intent will contain the selected Snapshot as a parcelable object in EXTRA_SNAPSHOT_METADATA. If the user pressed the add button, the result will be Activity.RESULT_OK and the data intent will contain a true boolean value in EXTRA_SNAPSHOT_NEW.
     * <p>
     * Note that if you have modified an open snapshot, the changes will not appear in this UI until you call commitAndClose(Snapshot, SnapshotMetadataChange) on the snapshot.
     *
     * @param title          The title to display in the action bar of the returned Activity.
     * @param allowAddButton Whether or not to display a "create new snapshot" option in the selection UI.
     * @param allowDelete    Whether or not to provide a delete overflow menu option for each snapshot in the selection UI.
     * @param maxSnapshots   The maximum number of snapshots to display in the UI. Use DISPLAY_LIMIT_NONE to display all snapshots.
     * @return Returns a Task which asynchronously loads an Intent that will let the user select a snapshot. Note that the Intent returned from the Task must be invoked with Activity.startActivityForResult(Intent, int), so that the identity of the calling package can be established.
     */
    Task<Intent> getSelectSnapshotIntent(String title, boolean allowAddButton, boolean allowDelete, int maxSnapshots);

    /**
     * This method takes a Bundle object and extracts the Snapshot provided. If the Bundle is invalid or does not contain the correct data, this method returns null.
     *
     * @param extras The Bundle to parse for a Snapshot object.
     * @return A SnapshotMetadata object that was provided for action.
     */
    SnapshotMetadata getSnapshotFromBundle(Bundle extras);

    /**
     * AbstractDataBuffer.release() should be called to release resources after usage.
     *
     * @param forceReload If true, this call will clear any locally cached data and attempt to fetch the latest data from the server. This would commonly be used for something like a user-initiated refresh. Normally, this should be set to false to gain advantages of data caching.
     * @return Returns a Task which asynchronously loads an annotated SnapshotMetadataBuffer that represents the snapshot data for the currently signed-in player.
     */
    Task<AnnotatedData<SnapshotMetadataBuffer>> load(boolean forceReload);

    /**
     * This will open the snapshot using RESOLUTION_POLICY_MANUAL as a conflict policy. If a conflict occurred, the result's SnapshotsClient.DataOrConflict.isConflict() will return true, and the conflict will need to be resolved using resolveConflict(String, Snapshot) to continue with opening the snapshot.
     * <p>
     * If the snapshot's contents are unavailable, the Task will fail with SnapshotsClient.SnapshotContentUnavailableApiException.
     *
     * @param metadata The metadata of the existing snapshot to load.
     * @return Returns a Task which asynchronously opens a snapshot with the given SnapshotMetadata (usually returned from load(boolean). To succeed, the snapshot must exist; i.e. this call will fail if the snapshot was deleted between the load and open calls.
     */
    Task<DataOrConflict<Snapshot>> open(SnapshotMetadata metadata);

    /**
     * If a conflict occurred, the result's SnapshotsClient.DataOrConflict.isConflict() will return true, and the conflict will need to be resolved using resolveConflict(String, Snapshot) to continue with opening the snapshot.
     * <p>
     * If the snapshot's contents are unavailable, the Task will fail with SnapshotsClient.SnapshotContentUnavailableApiException.
     *
     * @param metadata       The metadata of the existing snapshot to load.
     * @param conflictPolicy The conflict resolution policy to use for this snapshot.
     * @return Returns a Task which asynchronously opens a snapshot with the given SnapshotMetadata (usually returned from load(boolean). To succeed, the snapshot must exist; i.e. this call will fail if the snapshot was deleted between the load and open calls.
     */
    Task<DataOrConflict<Snapshot>> open(SnapshotMetadata metadata, int conflictPolicy);

    /**
     * If a conflict occurred, the result's SnapshotsClient.DataOrConflict.isConflict() will return true, and the conflict will need to be resolved using resolveConflict(String, Snapshot) to continue with opening the snapshot.
     * <p>
     * If the snapshot's contents are unavailable, the Task will fail with SnapshotsClient.SnapshotContentUnavailableApiException.
     *
     * @param fileName         The name of the snapshot file to open. Must be between 1 and 100 non-URL-reserved characters (a-z, A-Z, 0-9, or the symbols "-", ".", "_", or "~").
     * @param createIfNotFound If true, the snapshot will be created if one cannot be found.
     * @param conflictPolicy   The conflict resolution policy to use for this snapshot.
     * @return Returns a Task which asynchronously opens a snapshot with the given fileName. If createIfNotFound is set to true, the specified snapshot will be created if it does not already exist.
     */
    Task<DataOrConflict<Snapshot>> open(String fileName, boolean createIfNotFound, int conflictPolicy);

    /**
     * This will open the snapshot using RESOLUTION_POLICY_MANUAL as a conflict policy. If a conflict occurred, the result's SnapshotsClient.DataOrConflict.isConflict() will return true, and the conflict will need to be resolved using resolveConflict(String, Snapshot) to continue with opening the snapshot.
     * <p>
     * If the snapshot's contents are unavailable, the Task will fail with SnapshotsClient.SnapshotContentUnavailableApiException.
     *
     * @param fileName         The name of the snapshot file to open. Must be between 1 and 100 non-URL-reserved characters (a-z, A-Z, 0-9, or the symbols "-", ".", "_", or "~").
     * @param createIfNotFound If true, the snapshot will be created if one cannot be found.
     * @return Returns a Task which asynchronously opens a snapshot with the given fileName. If createIfNotFound is set to true, the specified snapshot will be created if it does not already exist.
     */
    Task<DataOrConflict<Snapshot>> open(String fileName, boolean createIfNotFound);

    /**
     * Values which are not included in the metadata change will be resolved to the version currently on the server.
     * <p>
     * If a conflict occurred, the result's SnapshotsClient.DataOrConflict.isConflict() will return true, and the conflict will need to be resolved again using resolveConflict(String, Snapshot) to continue with opening the snapshot.
     * <p>
     * Note that the total size of contents may not exceed the size provided by getMaxDataSize().
     * <p>
     * Calling this method with a snapshot that has already been committed or that was not opened via open(SnapshotMetadata) will throw an exception.
     * <p>
     * If the resolved snapshot's contents are unavailable, the Task will fail with SnapshotsClient.SnapshotContentUnavailableApiException.
     *
     * @param conflictId       The ID of the conflict to resolve. Must come from SnapshotsClient.SnapshotConflict.getConflictId().
     * @param snapshotId       The ID of the snapshot to resolve the conflict for.
     * @param metadataChange   The set of changes to apply to the metadata for the snapshot.
     * @param snapshotContents The SnapshotContents to replace the snapshot data with.
     * @return Returns a Task which asynchronously resolves a conflict using the provided data. This will replace the data on the server with the specified SnapshotMetadataChange and SnapshotContents. Note that it is possible for this operation to result in a conflict itself, in which case resolution should be repeated.
     */
    Task<DataOrConflict<Snapshot>> resolveConflict(String conflictId, String snapshotId, SnapshotMetadataChange metadataChange, SnapshotContents snapshotContents);

    /**
     * If a conflict occurred, the result's SnapshotsClient.DataOrConflict.isConflict() will return true, and the conflict will need to be resolved again using resolveConflict(String, Snapshot) to continue with opening the snapshot.
     * <p>
     * Note that the total size of the contents of snapshot may not exceed the size provided by getMaxDataSize().
     * <p>
     * This method fails a Task with an exception when called with a snapshot that was not opened or has already been committed/discarded.
     * <p>
     * If the resolved snapshot's contents are unavailable, the Task will fail with SnapshotsClient.SnapshotContentUnavailableApiException.
     *
     * @param conflictId The ID of the conflict to resolve. Must come from SnapshotsClient.SnapshotConflict.getConflictId().
     * @param snapshot   The snapshot to use to resolve the conflict.
     * @return Returns a Task which asynchronously resolves a conflict using the data from the provided Snapshot. This will replace the data on the server with the specified Snapshot. Note that it is possible for this operation to result in a conflict itself, in which case resolution should be repeated.
     */
    Task<DataOrConflict<Snapshot>> resolveConflict(String conflictId, Snapshot snapshot);

    /**
     * Represents the result of attempting to open a snapshot or resolve a conflict from a previous attempt.
     */
    class DataOrConflict<T> {
        private final SnapshotConflict snapshotConflict;
        private final T t;

        public DataOrConflict(T t, SnapshotConflict snapshotConflict) {
            this.snapshotConflict = snapshotConflict;
            this.t = t;
        }

        public SnapshotConflict getConflict() {
            return snapshotConflict;
        }

        public boolean isConflict() {
            return snapshotConflict != null;
        }

        public T getData() {
            return t;
        }
    }

    /**
     * Result delivered when a conflict was detected during {@link SnapshotsClient#open(SnapshotMetadata)} or {@link SnapshotsClient#resolveConflict(String, Snapshot)}.
     */
    class SnapshotConflict {
        private final String conflictId;
        private final Snapshot conflictionSnapshot;
        private final SnapshotContents snapshotContents;
        private final Snapshot snapshot;

        public SnapshotConflict(String conflictId, Snapshot conflictionSnapshot, SnapshotContents snapshotContents, Snapshot snapshot) {
            this.conflictId = conflictId;
            this.conflictionSnapshot = conflictionSnapshot;
            this.snapshotContents = snapshotContents;
            this.snapshot = snapshot;
        }

        public String getConflictId() {
            return conflictId;
        }

        public Snapshot getConflictingSnapshot() {
            return conflictionSnapshot;
        }

        public SnapshotContents getResolutionSnapshotContents() {
            return snapshotContents;
        }

        public Snapshot getSnapshot() {
            return snapshot;
        }
    }

    /**
     * Indicates that the snapshot contents are unavailable at the moment, but the SnapshotMetadata is available through {@link SnapshotContentUnavailableApiException#getSnapshotMetadata()}.
     */
    class SnapshotContentUnavailableApiException extends ApiException {
        protected final SnapshotMetadata metadata;

        public SnapshotContentUnavailableApiException(Status status, SnapshotMetadata metadata) {
            super(status);
            this.metadata = metadata;
        }

        public SnapshotMetadata getSnapshotMetadata() {
            return metadata;
        }
    }
}
