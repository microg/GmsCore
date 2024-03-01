/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.mobstore;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.mobstore.OpenFileDescriptorResponse;

interface IMobStoreFileCallbacks {
    void onOpenFileResult(in Status status, in OpenFileDescriptorResponse openFileDescriptorResponse) = 0;
    void onDeleteFileResult(in Status status) = 1;
    void onRenameFileResult(in Status status) = 2;
}