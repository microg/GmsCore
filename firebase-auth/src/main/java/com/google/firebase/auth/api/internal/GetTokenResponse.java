/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.firebase.auth.api.internal;

import org.json.JSONException;
import org.json.JSONObject;
import org.microg.safeparcel.AutoSafeParcelable;

public class GetTokenResponse extends AutoSafeParcelable {
    @Field(2)
    public String refreshToken;
    @Field(3)
    public String accessToken;
    @Field(4)
    public Long expiresIn;
    @Field(5)
    public String tokenType;
    @Field(6)
    public Long issuedAt;

    public GetTokenResponse() {
        issuedAt = System.currentTimeMillis();
    }

    public static GetTokenResponse parseJson(String json) {
        try {
            JSONObject object = new JSONObject(json);
            GetTokenResponse response = new GetTokenResponse();
            response.refreshToken = object.optString("refresh_token", null);
            response.accessToken = object.optString("access_token", null);
            response.tokenType = object.optString("token_type", null);
            response.expiresIn = object.optLong("expires_in");
            response.issuedAt = object.optLong("issued_at");
            return response;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public static final Creator<GetTokenResponse> CREATOR = new AutoCreator<>(GetTokenResponse.class);
}
