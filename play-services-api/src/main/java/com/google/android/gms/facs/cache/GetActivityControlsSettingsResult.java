/*
 * SPDX-FileCopyrightText: 2021, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.facs.cache;

import org.microg.gms.common.PublicApi;
import org.microg.safeparcel.AutoSafeParcelable;

public class GetActivityControlsSettingsResult extends AutoSafeParcelable {
    @Field(1)
    @PublicApi(exclude = true)
    public byte[] data;
    public static final Creator<GetActivityControlsSettingsResult> CREATOR = new AutoCreator<>(GetActivityControlsSettingsResult.class);
}
