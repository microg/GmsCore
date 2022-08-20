/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.fido.sourcedevice;

import org.microg.safeparcel.AutoSafeParcelable;

/**
 * Customized options to start direct transfer.
 */
public class SourceStartDirectTransferOptions extends AutoSafeParcelable {
    /**
     * Value of the callerType if the caller is unknown.
     */
    public static final int CALLER_TYPE_UNKNOWN = 0;
    /**
     * Value of the callerType if the caller is browser.
     */
    public static final int CALLER_TYPE_BROWSER = 2;

    @Field(1)
    private int callerType;

    private SourceStartDirectTransferOptions() {
    }

    /**
     * Constructor for the {@link SourceStartDirectTransferOptions}.
     */
    public SourceStartDirectTransferOptions(int callerType) {
        this.callerType = callerType;
    }

    public static final Creator<SourceStartDirectTransferOptions> CREATOR = new AutoCreator<>(SourceStartDirectTransferOptions.class);
}
