/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.mobstore;

import com.google.android.gms.mobstore.DeleteFileRequest;
import com.google.android.gms.mobstore.OpenFileDescriptorRequest;
import com.google.android.gms.mobstore.RenameRequest;
import com.google.android.gms.mobstore.IMobStoreFileCallbacks;

interface IMobStoreFileService {
    void openFile(in IMobStoreFileCallbacks callback, in OpenFileDescriptorRequest openFileDescriptorRequest) = 0;
    void deleteFile(in IMobStoreFileCallbacks callback, in DeleteFileRequest deleteFileRequest) = 1;
    void renameFile(in IMobStoreFileCallbacks callback, in RenameRequest renameRequest) = 2;
}