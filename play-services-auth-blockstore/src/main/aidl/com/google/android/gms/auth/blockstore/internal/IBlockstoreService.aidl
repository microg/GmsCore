/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.auth.blockstore.internal;

import com.google.android.gms.common.api.Status;
import android.os.Bundle;
import com.google.android.gms.auth.blockstore.AppRestoreInfo;
import com.google.android.gms.auth.blockstore.DeleteBytesRequest;
import com.google.android.gms.auth.blockstore.RetrieveBytesRequest;
import com.google.android.gms.auth.blockstore.StoreBytesData;
import com.google.android.gms.common.api.internal.IStatusCallback;
import com.google.android.gms.auth.blockstore.internal.IRetrieveBytesCallback;
import com.google.android.gms.auth.blockstore.internal.ISetBlockstoreDataCallback;
import com.google.android.gms.auth.blockstore.internal.IGetBlockstoreDataCallback;
import com.google.android.gms.auth.blockstore.internal.IGetAccessForPackageCallback;
import com.google.android.gms.auth.blockstore.internal.IStoreBytesCallback;
import com.google.android.gms.auth.blockstore.internal.IIsEndToEndEncryptionAvailableCallback;
import com.google.android.gms.auth.blockstore.internal.IDeleteBytesCallback;

interface IBlockstoreService {
    void retrieveBytes(IRetrieveBytesCallback callback) = 1;
    void setBlockstoreData(ISetBlockstoreDataCallback callback, in byte[] data) = 2;
    void getBlockstoreData(IGetBlockstoreDataCallback callback) = 3;
    void getAccessForPackage(IGetAccessForPackageCallback callback, String packageName) = 4;
    void setFlagWithPackage(IStatusCallback callback, String packageName, int flag) = 5;
    void clearFlagForPackage(IStatusCallback callback, String packageName) = 6;
    void updateFlagForPackage(IStatusCallback callback, String packageName, int value) = 7;
    void reportAppRestore(IStatusCallback callback, in List<String> packages, int code, in AppRestoreInfo info) = 8;
    void storeBytes(IStoreBytesCallback callback, in StoreBytesData data) = 9;
    void isEndToEndEncryptionAvailable(IIsEndToEndEncryptionAvailableCallback callback) = 10;
    void retrieveBytesWithRequest(IRetrieveBytesCallback callback, in RetrieveBytesRequest request) = 11;
    void deleteBytes(IDeleteBytesCallback callback, in DeleteBytesRequest request) = 12;
}