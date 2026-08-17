/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.auth.api.identity;

import android.app.Activity;
import android.app.PendingIntent;
import android.os.Parcel;
import androidx.annotation.NonNull;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import org.microg.gms.common.Hide;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Configurations that allow saving a token to Google for the purpose of account linking.
 */
public class SaveAccountLinkingTokenRequest extends AbstractSafeParcelable {
    public static final String EXTRA_TOKEN = "extra_token";
    /**
     * A constant to indicate that the type of token that will be saved is an Authorization Code.
     */
    public static final String TOKEN_TYPE_AUTH_CODE = "auth_code";

    @Field(value = 1, getterName = "getConsentPendingIntent")
    private final PendingIntent consentPendingIntent;
    @Field(value = 2, getterName = "getTokenType")
    private final String tokenType;
    @Field(value = 3, getterName = "getServiceId")
    private final String serviceId;
    @Field(value = 4, getterName = "getScopes")
    private final List<String> scopes;
    @Field(value = 5, getterName = "getSessionId")
    private final String sessionId;
    @Field(value = 6, getterName = "getTheme")
    private final int theme;

    @Constructor
    SaveAccountLinkingTokenRequest(@Param(1) PendingIntent consentPendingIntent, @Param(2) String tokenType, @Param(3) String serviceId, @Param(4) List<String> scopes, @Param(5) String sessionId, @Param(6) int theme) {
        this.consentPendingIntent = consentPendingIntent;
        this.tokenType = tokenType;
        this.serviceId = serviceId;
        this.scopes = scopes;
        this.sessionId = sessionId;
        this.theme = theme;
    }

    /**
     * Returns an instance of the {@link SaveAccountLinkingTokenRequest.Builder} that can be used to build an instance of {@link SaveAccountLinkingTokenRequest}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the {@link PendingIntent} that is set in the request and will be used to launch the consent page by Google Play Services during the flow.
     */
    public PendingIntent getConsentPendingIntent() {
        return consentPendingIntent;
    }

    /**
     * Returns the scopes that were set in the request. These are the requested scopes for the token that will be issued by your application.
     */
    public List<String> getScopes() {
        return scopes;
    }

    /**
     * Returns the service-id that was set in the request. This service-id can be found in your cloud project.
     */
    public String getServiceId() {
        return serviceId;
    }

    @Hide
    public String getSessionId() {
        return sessionId;
    }

    @Hide
    public int getTheme() {
        return theme;
    }

    /**
     * Returns the type of token that is requested.
     */
    public String getTokenType() {
        return tokenType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SaveAccountLinkingTokenRequest)) return false;

        SaveAccountLinkingTokenRequest that = (SaveAccountLinkingTokenRequest) o;

        if (theme != that.theme) return false;
        if (!Objects.equals(consentPendingIntent, that.consentPendingIntent)) return false;
        if (!Objects.equals(tokenType, that.tokenType)) return false;
        if (!Objects.equals(serviceId, that.serviceId)) return false;
        if (!Objects.equals(scopes, that.scopes)) return false;
        return Objects.equals(sessionId, that.sessionId);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{consentPendingIntent, tokenType, serviceId, scopes, serviceId, theme});
    }

    /**
     * Builder for {@link SaveAccountLinkingTokenRequest}.
     */
    public static class Builder {
        private PendingIntent consentPendingIntent;
        private String tokenType;
        private String serviceId;
        private List<String> scopes;
        private String sessionId;
        private int theme;

        /**
         * Builds an immutable instance of the {@link SaveAccountLinkingTokenRequest}.
         */
        @NonNull
        public SaveAccountLinkingTokenRequest build() {
            return new SaveAccountLinkingTokenRequest(consentPendingIntent, tokenType, serviceId, scopes, sessionId, theme);
        }

        /**
         * Sets the (mandatory) {@link PendingIntent} that can be launched by Google Play services to show the consent page during
         * the flow. When the launched Activity is finished, it has to provide the appropriate data in the result that it returns to the
         * caller, based on the following contract:
         * <ul>
         *     <li>
         *         If the user has accepted the consent, the launched Activity must call {@code setResult} with {@link Activity#RESULT_OK}, along with a
         *         token as a (string) intent extra in the result, with the key {@link SaveAccountLinkingTokenRequest#EXTRA_TOKEN},
         *         similar to the following snippet:
         *         <pre>
         *  Intent intent = new Intent();
         *  intent.putExtra(SaveAccountLinkingTokenRequest.EXTRA_TOKEN, token);
         *  setResult(Activity.RESULT_OK, intent);
         *  finish();
         *         </pre>
         *     </li>
         *     <li>
         *         If, however, the user has rejected the consent, the Activity has to call {@code setResult} with {@link Activity#RESULT_CANCELED}.
         *     </li>
         * </ul>
         */
        @NonNull
        public Builder setConsentPendingIntent(PendingIntent consentPendingIntent) {
            this.consentPendingIntent = consentPendingIntent;
            return this;
        }

        /**
         * Sets the list of scopes that are associated with the token that will be saved to Google. Calling this method with the
         * correct scope(s) is required.
         */
        @NonNull
        public Builder setScopes(List<String> scopes) {
            this.scopes = scopes;
            return this;
        }

        /**
         * Sets the service-id that can be obtained from your Google Cloud project. Calling this method to set {@code serviceId}
         * is required.
         */
        @NonNull
        public Builder setServiceId(String serviceId) {
            this.serviceId = serviceId;
            return this;
        }

        @Hide
        @NonNull
        public Builder setSessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        @Hide
        @NonNull
        public Builder setTheme(int theme) {
            this.theme = theme;
            return this;
        }

        /**
         * Sets the type of token that will be saved to Google. Valid options are:
         * <ul>
         *     <li>{@link SaveAccountLinkingTokenRequest#TOKEN_TYPE_AUTH_CODE}</li>
         * </ul>
         * Calling this method with a valid token type is required.
         */
        @NonNull
        public Builder setTokenType(String tokenType) {
            this.tokenType = tokenType;
            return this;
        }
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<SaveAccountLinkingTokenRequest> CREATOR = findCreator(SaveAccountLinkingTokenRequest.class);
}
