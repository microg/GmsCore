/*
 * SPDX-FileCopyrightText: 2021, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.facs.cache;

import org.microg.safeparcel.AutoSafeParcelable;

public class ForceSettingsCacheRefreshResult extends AutoSafeParcelable {

    public static final Creator<ForceSettingsCacheRefreshResult> CREATOR = new AutoCreator<>(ForceSettingsCacheRefreshResult.class);
}
