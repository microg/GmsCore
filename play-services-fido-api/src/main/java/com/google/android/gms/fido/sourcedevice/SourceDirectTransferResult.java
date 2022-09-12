/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.fido.sourcedevice;

import android.app.Activity;
import android.content.Intent;

import com.google.android.gms.common.api.Status;

import org.microg.gms.common.PublicApi;
import org.microg.safeparcel.AutoSafeParcelable;

/**
 * Result returned from the UI activity in {@link Activity#onActivityResult(int, int, Intent)} after the direct transfer finishes.
 */
@PublicApi
public class SourceDirectTransferResult extends AutoSafeParcelable {
    @Field(1)
    private Status status;

    private SourceDirectTransferResult() {
    }

    public SourceDirectTransferResult(Status status) {
        this.status = status;
    }

    /**
     * Gets the {@link Status} from the returned {@link SourceDirectTransferResult}.
     */
    public Status getStatus() {
        return status;
    }

    public static final Creator<SourceDirectTransferResult> CREATOR = new AutoCreator<>(SourceDirectTransferResult.class);
}
