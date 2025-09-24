/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.auth.blockstore;

import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.HasApiKey;
import com.google.android.gms.tasks.Task;

import org.microg.gms.common.PublicApi;

/**
 * The interface for clients to access Block Store.
 * Clients should call {@link #storeBytes(StoreBytesData)} to store authentication credentials byte[] bytes to enable seamless sign-in on other devices.
 * Clients should call {@link #retrieveBytes()} to fetch the authentication credentials to seamlessly sign in users on a newly setup device.
 */
@PublicApi
public interface BlockstoreClient extends HasApiKey<Api.ApiOptions.NoOptions> {
    /**
     * The default key with which the bytes are associated when storeBytes(StoreBytesData) is called without explicitly setting a key.
     */
    String DEFAULT_BYTES_DATA_KEY = "com.google.android.gms.auth.blockstore.DEFAULT_BYTES_DATA_KEY";

    /**
     * Maximum number of distinct data entries, differentiated by the data keys, that can be stored using {@link BlockstoreClient#storeBytes}.
     * The data key is the value provided when storing the data via {@link BlockstoreClient#storeBytes(StoreBytesData)}, as StoreBytesData.key.
     */
    int MAX_ENTRY_COUNT = 16;

    /**
     * Maximum allowed size of byte blobs that can be stored using {@link BlockstoreClient#storeBytes}.
     */
    int MAX_SIZE = 1024;

    /**
     * @param deleteBytesRequest if deleteBytesRequest is null. Throws NullPointerException
     * @return a Task which asynchronously deletes the bytes matching the filter(s) specified in deleteBytesRequest,
     * with a Boolean result representing whether any bytes were actually deleted.
     * If no bytes were found to delete, the task succeeds with a false return value.
     */
    Task<Boolean> deleteBytes(DeleteBytesRequest deleteBytesRequest);

    /**
     * @return a Task which asynchronously determines whether Block Store data backed up to the cloud will be end-to-end encrypted.
     * End-to-end encryption is available for Pie and above devices with a lockscreen PIN/pattern.
     * The Boolean return value is whether Block Store data backed up to the cloud will be end-to-end encrypted.
     */
    Task<Boolean> isEndToEndEncryptionAvailable();

    /**
     * @param retrieveBytesRequest if retrieveBytesRequest is null. Throws NullPointerException
     * @return a Task which asynchronously retrieves the previously-stored bytes, if any, matching the filter(s) specified in retrieveBytesRequest.
     * The returned RetrieveBytesResponse contains a map from data keys to BlockstoreData.
     * The data may have been written on the same device or may have been transferred during the device setup.
     * Use this API to seamlessly sign-in users to the apps on a new device.
     * If no data is found, returns an empty data map.
     * Note that the data may be cleared by Google Play services on certain user actions, like user clearing app storage (among others).
     * The bytes stored without an explicitly specified StoreBytesData.key can be requested with, and is returned associated with, the default key DEFAULT_BYTES_DATA_KEY.
     */
    Task<RetrieveBytesResponse> retrieveBytes(RetrieveBytesRequest retrieveBytesRequest);

    /**
     * @return a Task which asynchronously retrieves the previously-stored bytes that was stored without an explicitly specified StoreBytesData.key, if any.
     * The maximum size of the byte[] is the MAX_SIZE.
     * The byte[] may have been written on the same device or may have been transferred during the device setup.
     * Use this API to seamlessly sign-in users to the apps on a new device.
     * If no data is found, returns an empty byte array. Note that the data may be cleared by Google Play services on certain user actions, like user clearing app storage (among others).
     */
    @Deprecated
    Task<byte[]> retrieveBytes();

    /**
     * @param storeBytesData
     * @return a Task which asynchronously stores the provided byte[] bytes and associates it with the provided String key.
     * If the key is not explicitly set, then the bytes will be associated with the default key DEFAULT_BYTES_DATA_KEY.
     * The data is stored locally. It is transferred to a new device during the device-to-device restore if a google account is also transferred.
     * If in StoreBytesData.shouldBackupToCloud() is set to true, the data will also be backed up to the cloud in the next periodic sync. Cloud backup data is transferred to a new device during the cloud restore using Google's Backup & Restore services.
     * The maximum size of String key and byte[] bytes combined is MAX_SIZE; otherwise, the API fails with BlockstoreStatusCodes.MAX_SIZE_EXCEEDED error code.
     * The maximum number of data entries allowed is MAX_ENTRY_COUNT; otherwise, the API fails with BlockstoreStatusCodes.TOO_MANY_ENTRIES error code.
     * The Integer return value is the size of byte[] bytes successfully stored.
     * Use this API to store small data blobs that can enable seamless sign in for your apps. The API may be called periodically (for example, a few times per day) to refresh the data blob.
     * Successive calls with the same key to this API will overwrite the existing bytes.
     */
    Task<Integer> storeBytes(StoreBytesData storeBytesData);
}
