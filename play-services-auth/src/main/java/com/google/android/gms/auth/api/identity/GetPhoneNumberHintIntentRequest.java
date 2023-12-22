/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.auth.api.identity;

import org.microg.safeparcel.AutoSafeParcelable;

public class GetPhoneNumberHintIntentRequest extends AutoSafeParcelable {
    @Field(1)
    public int code;

    public static final Creator<GetPhoneNumberHintIntentRequest> CREATOR = findCreator(GetPhoneNumberHintIntentRequest.class);
}
