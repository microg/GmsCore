/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.auth.blockstore;

import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.HasApiKey;
import com.google.android.gms.tasks.Task;

/**
 * The interface for clients to access Block Store.
 * <ul>
 *     <li>
 *         Clients should call storeBytes(StoreBytesData) to store authentication credentials byte[] bytes to enable seamless sign-in on
 *         other devices.
 *     </li>
 *     <li>Clients should call retrieveBytes() to fetch the authentication credentials to seamlessly sign in users on a newly setup device.</li>
 * </ul>
 */
public interface BlockstoreClient extends HasApiKey<Api.ApiOptions.NoOptions> {
    /**
     * The default key with which the bytes are associated when {@link #storeBytes(StoreBytesData)} is called without explicitly setting a {@code key}.
     */
    String DEFAULT_BYTES_DATA_KEY = "com.google.android.gms.auth.blockstore.DEFAULT_BYTES_DATA_KEY";

    /**
     * Maximum number of distinct data entries, differentiated by the data keys, that can be stored using {@link BlockstoreClient#storeBytes(StoreBytesData)}.
     * <p>
     * The data key is the value provided when storing the data via {@link #storeBytes(StoreBytesData)}, as {@code StoreBytesData.key}.
     */
    int MAX_ENTRY_COUNT = 16;

    /**
     * Maximum allowed size of byte blobs that can be stored using {@link BlockstoreClient#storeBytes(StoreBytesData)}.
     */
    int MAX_SIZE = 1024;

    /**
     * Returns a {@link Task} which asynchronously deletes the bytes matching the filter(s) specified in {@code deleteBytesRequest}, with a Boolean result
     * representing whether any bytes were actually deleted.
     * <p>
     * If no bytes were found to delete, the task succeeds with a {@code false} return value.
     *
     * @throws NullPointerException if {@code deleteBytesRequest} is null.
     */
    Task<Boolean> deleteBytes(DeleteBytesRequest deleteBytesRequest) throws NullPointerException;

    /**
     * Returns a {@code Task} which asynchronously determines whether Block Store data backed up to the cloud will be end-to-end encrypted.
     * <p>
     * End-to-end encryption is available for Pie and above devices with a lockscreen PIN/pattern.
     * <p>
     * The {@code Boolean} return value is whether Block Store data backed up to the cloud will be end-to-end encrypted.
     */
    Task<Boolean> isEndToEndEncryptionAvailable();

    /**
     * Returns a {@code Task} which asynchronously retrieves the previously-stored bytes, if any, matching the filter(s) specified in
     * {@code retrieveBytesRequest}.
     * <p>
     * The returned {@code RetrieveBytesResponse} contains a map from data keys to {@code BlockstoreData}. The data may have been written on the same
     * device or may have been transferred during the device setup.
     * <p>
     * Use this API to seamlessly sign-in users to the apps on a new device.
     * <p>
     * If no data is found, returns an empty data map. Note that the data may be cleared by Google Play services on certain user actions, like user
     * clearing app storage (among others).
     * <p>
     * The bytes stored without an explicitly specified {@code StoreBytesData.key} can be requested with, and is returned associated with, the default
     * key {@link #DEFAULT_BYTES_DATA_KEY}.
     *
     * @throws NullPointerException if {@code retrieveBytesRequest} is null.
     */
    Task<RetrieveBytesResponse> retrieveBytes(RetrieveBytesRequest retrieveBytesRequest) throws NullPointerException;

    /**
     * Returns a {@link Task} which asynchronously retrieves the previously-stored bytes that was stored without an explicitly specified
     * {@code StoreBytesData.key}, if any. The maximum size of the {@code byte[]} is the {@link #MAX_SIZE}.
     * <p>
     * The {@code byte[]} may have been written on the same device or may have been transferred during the device setup.
     * <p>
     * Use this API to seamlessly sign-in users to the apps on a new device.
     * <p>
     * If no data is found, returns an empty byte array. Note that the data may be cleared by Google Play services on certain user actions, like user
     * clearing app storage (among others).
     *
     * @deprecated Use {@link #retrieveBytes(RetrieveBytesRequest)} instead.
     */
    @Deprecated
    Task<byte[]> retrieveBytes();

    /**
     * Returns a {@link Task} which asynchronously stores the provided {@code byte[] bytes} and associates it with the provided {@code String key}.
     * <p>
     * If the {@code key} is not explicitly set, then the {@code bytes} will be associated with the default key {@link #DEFAULT_BYTES_DATA_KEY}.
     * <p>
     * The data is stored locally. It is transferred to a new device during the device-to-device restore if a google account is also transferred.
     * <p>
     * If in {@link StoreBytesData#shouldBackupToCloud()} is set to {@code true}, the data will also be backed up to the cloud in the next periodic sync.
     * Cloud backup data is transferred to a new device during the cloud restore using Google's Backup & Restore services.
     * <p>
     * The maximum size of {@code String key} and {@code byte[] bytes} combined is {@link #MAX_SIZE}; otherwise, the API fails with
     * {@link BlockstoreStatusCodes#MAX_SIZE_EXCEEDED} error code.
     * <p>
     * The maximum number of data entries allowed is {@link #MAX_ENTRY_COUNT}; otherwise, the API fails with
     * {@link BlockstoreStatusCodes#TOO_MANY_ENTRIES} error code.
     * <p>
     * The {@code Integer} return value is the size of {@code byte[] bytes} successfully stored.
     * <p>
     * Use this API to store small data blobs that can enable seamless sign in for your apps. The API may be called periodically (for example, a few
     * times per day) to refresh the data blob. Successive calls with the same key to this API will overwrite the existing bytes.
     */
    Task<Integer> storeBytes(StoreBytesData storeBytesData);
}
