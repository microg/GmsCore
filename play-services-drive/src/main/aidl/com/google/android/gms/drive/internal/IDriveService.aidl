package com.google.android.gms.drive.internal;

import com.google.android.gms.drive.internal.DriveServiceResponse;
import com.google.android.gms.drive.internal.IDriveServiceCallbacks;
import com.google.android.gms.drive.internal.IEventCallback;

import com.google.android.gms.drive.internal.AddEventListenerRequest;
import com.google.android.gms.drive.internal.AddPermissionRequest;
import com.google.android.gms.drive.internal.AuthorizeAccessRequest;
import com.google.android.gms.drive.internal.CancelPendingActionsRequest;
import com.google.android.gms.drive.internal.ChangeResourceParentsRequest;
import com.google.android.gms.drive.internal.CheckResourceIdsExistRequest;
import com.google.android.gms.drive.internal.CloseContentsAndUpdateMetadataRequest;
import com.google.android.gms.drive.internal.CloseContentsRequest;
import com.google.android.gms.drive.internal.ControlProgressRequest;
import com.google.android.gms.drive.internal.CreateContentsRequest;
import com.google.android.gms.drive.internal.CreateFileRequest;
import com.google.android.gms.drive.internal.CreateFileIntentSenderRequest;
import com.google.android.gms.drive.internal.CreateFolderRequest;
import com.google.android.gms.drive.internal.DeleteResourceRequest;
import com.google.android.gms.drive.internal.DisconnectRequest;
import com.google.android.gms.drive.internal.FetchThumbnailRequest;
import com.google.android.gms.drive.internal.GetChangesRequest;
import com.google.android.gms.drive.internal.GetDriveIdFromUniqueIdRequest;
import com.google.android.gms.drive.internal.GetMetadataRequest;
import com.google.android.gms.drive.internal.GetPermissionsRequest;
import com.google.android.gms.drive.internal.ListParentsRequest;
import com.google.android.gms.drive.internal.LoadRealtimeRequest;
import com.google.android.gms.drive.internal.OpenContentsRequest;
import com.google.android.gms.drive.internal.OpenFileIntentSenderRequest;
import com.google.android.gms.drive.internal.RealtimeDocumentSyncRequest;
import com.google.android.gms.drive.internal.RemoveEventListenerRequest;
import com.google.android.gms.drive.internal.RemovePermissionRequest;
import com.google.android.gms.drive.internal.SetDrivePreferencesRequest;
import com.google.android.gms.drive.internal.SetFileUploadPreferencesRequest;
import com.google.android.gms.drive.internal.SetResourceParentsRequest;
import com.google.android.gms.drive.internal.StreamContentsRequest;
import com.google.android.gms.drive.internal.TrashResourceRequest;
import com.google.android.gms.drive.internal.UnsubscribeResourceRequest;
import com.google.android.gms.drive.internal.UntrashResourceRequest;
import com.google.android.gms.drive.internal.UpdateMetadataRequest;
import com.google.android.gms.drive.internal.UpdatePermissionRequest;

interface IDriveService {
    void getMetadata(in GetMetadataRequest request, IDriveServiceCallbacks callbacks) = 0;

    void updateMetadata(in UpdateMetadataRequest request, IDriveServiceCallbacks callbacks) = 2;
    void createContents(in CreateContentsRequest request, IDriveServiceCallbacks callbacks) = 3;
    void createFile(in CreateFileRequest request, IDriveServiceCallbacks callbacks) = 4;
    void createFolder(in CreateFolderRequest request, IDriveServiceCallbacks callbacks) = 5;
    DriveServiceResponse openContents(in OpenContentsRequest request, IDriveServiceCallbacks callbacks) = 6;
    void closeContents(in CloseContentsRequest request, IDriveServiceCallbacks callbacks) = 7;
    void requestSync(IDriveServiceCallbacks callbacks) = 8;
    IntentSender openFileIntentSender(in OpenFileIntentSenderRequest request) = 9;
    IntentSender createFileIntentSender(in CreateFileIntentSenderRequest request) = 10;
    void authorizeAccess(in AuthorizeAccessRequest request, IDriveServiceCallbacks callbacks) = 11;
    void listParents(in ListParentsRequest request, IDriveServiceCallbacks callbacks) = 12;
    void addEventListener(in AddEventListenerRequest request, IEventCallback callback, String unused, IDriveServiceCallbacks callbacks) = 13;
    void removeEventListener(in RemoveEventListenerRequest request, IEventCallback callback, String unused, IDriveServiceCallbacks callbacks) = 14;
    void disconnect(in DisconnectRequest request) = 15;
    void trashResource(in TrashResourceRequest request, IDriveServiceCallbacks callbacks) = 16;
    void closeContentsAndUpdateMetadata(in CloseContentsAndUpdateMetadataRequest request, IDriveServiceCallbacks callbacks) = 17;

    void deleteResource(in DeleteResourceRequest request, IDriveServiceCallbacks callbacks) = 23;

    void loadRealtime(in LoadRealtimeRequest request, IDriveServiceCallbacks callbacks) = 26;
    void setResourceParents(in SetResourceParentsRequest request, IDriveServiceCallbacks callbacks) = 27;
    void getDriveIdFromUniqueId(in GetDriveIdFromUniqueIdRequest request, IDriveServiceCallbacks callbacks) = 28;
    void checkResourceIdsExist(in CheckResourceIdsExistRequest request, IDriveServiceCallbacks callbacks) = 29;
    void completePendingAction(IDriveServiceCallbacks callbacks) = 30;
    void getDrivePreferences(IDriveServiceCallbacks callbacks) = 31;
    void setDrivePreferences(in SetDrivePreferencesRequest request, IDriveServiceCallbacks callbacks) = 32;
    void realtimeDocumentSync(in RealtimeDocumentSyncRequest request, IDriveServiceCallbacks callbacks) = 33;
    void getDeviceUsagePreferences(IDriveServiceCallbacks callbacks) = 34;
    void setFileUploadPreferences(in SetFileUploadPreferencesRequest request, IDriveServiceCallbacks callbacks) = 35;
    void cancelPendingActions(in CancelPendingActionsRequest request, IDriveServiceCallbacks callbacks) = 36;
    void untrashResource(in UntrashResourceRequest request, IDriveServiceCallbacks callbacks) = 37;

    void isAutoBackupEnabled(IDriveServiceCallbacks callbacks) = 40;
    void fetchThumbnail(in FetchThumbnailRequest request, IDriveServiceCallbacks callbacks) = 41;

    void getChanges(in GetChangesRequest request, IDriveServiceCallbacks callbacks) = 43;

    void unsubscribeResource(in UnsubscribeResourceRequest request, IDriveServiceCallbacks callbacks) = 45;
    void getPermissions(in GetPermissionsRequest request, IDriveServiceCallbacks callbacks) = 46;
    void addPermission(in AddPermissionRequest request, IDriveServiceCallbacks callbacks) = 47;
    void updatePermission(in UpdatePermissionRequest request, IDriveServiceCallbacks callbacks) = 48;
    void removePermission(in RemovePermissionRequest request, IDriveServiceCallbacks callbacks) = 49;

    void removeQueryResultListener(IEventCallback callback, IDriveServiceCallbacks callbacks) = 51;
    void controlProgress(in ControlProgressRequest request, IDriveServiceCallbacks callbacks) = 52;

    void changeResourceParents(in ChangeResourceParentsRequest request, IDriveServiceCallbacks callbacks) = 54;
    DriveServiceResponse streamContents(in StreamContentsRequest request, IDriveServiceCallbacks callbacks) = 55;
}
