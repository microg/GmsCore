/*
 * Copyright (C) 2013-2017 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.microg.gms.drive.api;

import android.content.IntentSender;
import android.os.RemoteException;
import android.util.Log;

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
import com.google.android.gms.drive.internal.CreateFileIntentSenderRequest;
import com.google.android.gms.drive.internal.CreateFileRequest;
import com.google.android.gms.drive.internal.CreateFolderRequest;
import com.google.android.gms.drive.internal.DeleteResourceRequest;
import com.google.android.gms.drive.internal.DisconnectRequest;
import com.google.android.gms.drive.internal.DriveServiceResponse;
import com.google.android.gms.drive.internal.FetchThumbnailRequest;
import com.google.android.gms.drive.internal.GetChangesRequest;
import com.google.android.gms.drive.internal.GetDriveIdFromUniqueIdRequest;
import com.google.android.gms.drive.internal.GetMetadataRequest;
import com.google.android.gms.drive.internal.GetPermissionsRequest;
import com.google.android.gms.drive.internal.IDriveService;
import com.google.android.gms.drive.internal.IDriveServiceCallbacks;
import com.google.android.gms.drive.internal.IEventCallback;
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

public class DriveServiceImpl extends IDriveService.Stub {
    private static final String TAG = "GmsDriveSvcImpl";

    @Override
    public void getMetadata(GetMetadataRequest request, IDriveServiceCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "unimplemented Method: getMetadata");

    }

    @Override
    public void updateMetadata(UpdateMetadataRequest request, IDriveServiceCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "unimplemented Method: updateMetadata");

    }

    @Override
    public void createContents(CreateContentsRequest request, IDriveServiceCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "unimplemented Method: createContents");

    }

    @Override
    public void createFile(CreateFileRequest request, IDriveServiceCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "unimplemented Method: createFile");

    }

    @Override
    public void createFolder(CreateFolderRequest request, IDriveServiceCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "unimplemented Method: createFolder");

    }

    @Override
    public DriveServiceResponse openContents(OpenContentsRequest request, IDriveServiceCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "unimplemented Method: openContents");
        return null;
    }

    @Override
    public void closeContents(CloseContentsRequest request, IDriveServiceCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "unimplemented Method: closeContents");

    }

    @Override
    public void requestSync(IDriveServiceCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "unimplemented Method: requestSync");

    }

    @Override
    public IntentSender openFileIntentSender(OpenFileIntentSenderRequest request) throws RemoteException {
        Log.d(TAG, "unimplemented Method: openFileIntentSender");
        return null;
    }

    @Override
    public IntentSender createFileIntentSender(CreateFileIntentSenderRequest request) throws RemoteException {
        Log.d(TAG, "unimplemented Method: createFileIntentSender");
        return null;
    }

    @Override
    public void authorizeAccess(AuthorizeAccessRequest request, IDriveServiceCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "unimplemented Method: authorizeAccess");

    }

    @Override
    public void listParents(ListParentsRequest request, IDriveServiceCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "unimplemented Method: listParents");

    }

    @Override
    public void addEventListener(AddEventListenerRequest request, IEventCallback callback, String unused, IDriveServiceCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "unimplemented Method: addEventListener");

    }

    @Override
    public void removeEventListener(RemoveEventListenerRequest request, IEventCallback callback, String unused, IDriveServiceCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "unimplemented Method: removeEventListener");

    }

    @Override
    public void disconnect(DisconnectRequest request) throws RemoteException {
        Log.d(TAG, "unimplemented Method: disconnect");

    }

    @Override
    public void trashResource(TrashResourceRequest request, IDriveServiceCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "unimplemented Method: trashResource");

    }

    @Override
    public void closeContentsAndUpdateMetadata(CloseContentsAndUpdateMetadataRequest request, IDriveServiceCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "unimplemented Method: closeContentsAndUpdateMetadata");

    }

    @Override
    public void deleteResource(DeleteResourceRequest request, IDriveServiceCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "unimplemented Method: deleteResource");

    }

    @Override
    public void loadRealtime(LoadRealtimeRequest request, IDriveServiceCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "unimplemented Method: loadRealtime");

    }

    @Override
    public void setResourceParents(SetResourceParentsRequest request, IDriveServiceCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "unimplemented Method: setResourceParents");

    }

    @Override
    public void getDriveIdFromUniqueId(GetDriveIdFromUniqueIdRequest request, IDriveServiceCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "unimplemented Method: getDriveIdFromUniqueId");

    }

    @Override
    public void checkResourceIdsExist(CheckResourceIdsExistRequest request, IDriveServiceCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "unimplemented Method: checkResourceIdsExist");

    }

    @Override
    public void completePendingAction(IDriveServiceCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "unimplemented Method: completePendingAction");

    }

    @Override
    public void getDrivePreferences(IDriveServiceCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "unimplemented Method: getDrivePreferences");

    }

    @Override
    public void setDrivePreferences(SetDrivePreferencesRequest request, IDriveServiceCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "unimplemented Method: setDrivePreferences");

    }

    @Override
    public void realtimeDocumentSync(RealtimeDocumentSyncRequest request, IDriveServiceCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "unimplemented Method: realtimeDocumentSync");

    }

    @Override
    public void getDeviceUsagePreferences(IDriveServiceCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "unimplemented Method: getDeviceUsagePreferences");

    }

    @Override
    public void setFileUploadPreferences(SetFileUploadPreferencesRequest request, IDriveServiceCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "unimplemented Method: setFileUploadPreferences");

    }

    @Override
    public void cancelPendingActions(CancelPendingActionsRequest request, IDriveServiceCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "unimplemented Method: cancelPendingActions");

    }

    @Override
    public void untrashResource(UntrashResourceRequest request, IDriveServiceCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "unimplemented Method: untrashResource");

    }

    @Override
    public void isAutoBackupEnabled(IDriveServiceCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "unimplemented Method: isAutoBackupEnabled");

    }

    @Override
    public void fetchThumbnail(FetchThumbnailRequest request, IDriveServiceCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "unimplemented Method: fetchThumbnail");

    }

    @Override
    public void getChanges(GetChangesRequest request, IDriveServiceCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "unimplemented Method: getChanges");

    }

    @Override
    public void unsubscribeResource(UnsubscribeResourceRequest request, IDriveServiceCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "unimplemented Method: unsubscribeResource");

    }

    @Override
    public void getPermissions(GetPermissionsRequest request, IDriveServiceCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "unimplemented Method: getPermissions");

    }

    @Override
    public void addPermission(AddPermissionRequest request, IDriveServiceCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "unimplemented Method: addPermission");

    }

    @Override
    public void updatePermission(UpdatePermissionRequest request, IDriveServiceCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "unimplemented Method: updatePermission");

    }

    @Override
    public void removePermission(RemovePermissionRequest request, IDriveServiceCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "unimplemented Method: removePermission");

    }

    @Override
    public void removeQueryResultListener(IEventCallback callback, IDriveServiceCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "unimplemented Method: removeQueryResultListener");

    }

    @Override
    public void controlProgress(ControlProgressRequest request, IDriveServiceCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "unimplemented Method: controlProgress");

    }

    @Override
    public void changeResourceParents(ChangeResourceParentsRequest request, IDriveServiceCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "unimplemented Method: changeResourceParents");

    }

    @Override
    public DriveServiceResponse streamContents(StreamContentsRequest request, IDriveServiceCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "unimplemented Method: streamContents");
        return null;
    }
}
