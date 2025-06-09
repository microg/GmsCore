package com.google.android.gms.auth.api.identity;

import static com.google.common.truth.Truth.assertThat;

import com.google.android.gms.common.internal.safeparcel.SafeParcelableTestUtil;

import org.junit.Test;

public class AuthorizationServiceRequestTest {

    @Test
    public void testEqualsAndHashCode() {
        AuthorizationServiceRequest request1 = new AuthorizationServiceRequest.Builder()
                .setTokenType("access_token")
                .setClientId("client-id-1")
                .setScope("profile email")
                .setRedirectUri("https://redirect.uri")
                .build();

        AuthorizationServiceRequest request2 = new AuthorizationServiceRequest.Builder()
                .setTokenType("access_token")
                .setClientId("client-id-1")
                .setScope("profile email")
                .setRedirectUri("https://redirect.uri")
                .build();

        assertThat(request1).isEqualTo(request2);
        assertThat(request1.hashCode()).isEqualTo(request2.hashCode());
    }

    @Test
    public void testParcelable() {
        AuthorizationServiceRequest request = new AuthorizationServiceRequest.Builder()
                .setTokenType("access_token")
                .setClientId("client-id-1")
                .setScope("profile email")
                .setRedirectUri("https://redirect.uri")
                .build();

        SafeParcelableTestUtil.assertSafeParcelableRoundTrip(AuthorizationServiceRequest.CREATOR, request);
    }

    @Test
    public void testToString() {
        AuthorizationServiceRequest request = new AuthorizationServiceRequest.Builder()
                .setTokenType("access_token")
                .setClientId("client-id-1")
                .setScope("profile email")
                .setRedirectUri("https://redirect.uri")
                .build();

        String string = request.toString();
        assertThat(string).contains("access_token");
        assertThat(string).contains("client-id-1");
        assertThat(string).contains("profile email");
        assertThat(string).contains("https://redirect.uri");
    }
          }
