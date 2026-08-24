/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.games.internal.connect;

import org.microg.safeparcel.AutoSafeParcelable;

public class GamesSignInResponse extends AutoSafeParcelable {
    @Field(1)
    public String gameRunToken;
    public static final Creator<GamesSignInResponse> CREATOR = findCreator(GamesSignInResponse.class);
}
