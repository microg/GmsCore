/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.languageprofile;

import org.microg.safeparcel.AutoSafeParcelable;

public class LanguageFluencyParams extends AutoSafeParcelable {
    public static final Creator<LanguageFluencyParams> CREATOR = new AutoCreator<>(LanguageFluencyParams.class);
}
