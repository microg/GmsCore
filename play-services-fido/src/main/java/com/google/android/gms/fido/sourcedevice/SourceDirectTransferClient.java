/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.fido.sourcedevice;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.ParcelFileDescriptor;

import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.HasApiKey;
import com.google.android.gms.tasks.Task;

import org.microg.gms.common.PublicApi;

/**
 * The entry point for interacting with the FIDO SourceDirectTransfer APIs.
 */
@PublicApi
public interface SourceDirectTransferClient extends HasApiKey<Api.ApiOptions.NoOptions> {
    /**
     * The key used to retrieve {@link SourceDirectTransferResult} from the intent received by
     * {@link Activity#onActivityResult(int, int, Intent)} after launching {@link PendingIntent} returned by
     * {@link #startDirectTransfer(SourceStartDirectTransferOptions, ParcelFileDescriptor, ParcelFileDescriptor)}.
     */
    String KEY_SOURCE_DIRECT_TRANSFER_RESULT = "source_direct_transfer_result";

    /**
     * Retrieves {@link SourceDirectTransferResult} from the intent received by
     * {@link Activity#onActivityResult(int, int, Intent)} after launching {@link PendingIntent} returned by
     * {@link #startDirectTransfer(SourceStartDirectTransferOptions, ParcelFileDescriptor, ParcelFileDescriptor)}.
     */
    SourceDirectTransferResult getSourceDirectTransferResultFromIntent(Intent intent) throws ApiException;

    /**
     * Creates a Task with {@link PendingIntent}, which when started, will start direct transfer.
     *
     * @param input  read side of pipe from the other device.
     * @param output write side of pipe to the other device.
     */
    Task<PendingIntent> startDirectTransfer(SourceStartDirectTransferOptions options, ParcelFileDescriptor input, ParcelFileDescriptor output);
}
