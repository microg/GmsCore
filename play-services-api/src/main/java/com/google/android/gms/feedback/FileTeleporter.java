/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.feedback;

import org.microg.safeparcel.AutoSafeParcelable;

public class FileTeleporter extends AutoSafeParcelable {
    public static final Creator<FileTeleporter> CREATOR = findCreator(FileTeleporter.class);
}
