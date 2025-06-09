package com.google.android.gms.auth.api.identity;

import static com.google.common.truth.Truth.assertThat;

import com.google.android.gms.common.internal.safeparcel.SafeParcelableTestUtil;

import org.junit.Test;

public class AuthorizationServiceResponseTest {

    @Test
    public void testEqualsAndHashCode() {
        AuthorizationServiceResponse response1 = new AuthorizationServiceResponse.Builder()
                .setAccessToken("access-token-123")
                .setTokenType("Bearer")
                .setExpiresIn(3600)
                .setScope("profile email")
                .build();

        AuthorizationServiceResponse response2 = new AuthorizationServiceResponse.Builder()
                .setAccessToken("access-token-123")
                .setTokenType("Bearer")
                .setExpiresIn(3600)
                .setScope("profile email")
                .build();

        assertThat(response1).isEqualTo(response2);
        assertThat(response1.hashCode()).isEqualTo(response2.hashCode());
    }

    @Test
    public void testParcelable() {
        AuthorizationServiceResponse response = new AuthorizationServiceResponse.Builder()
                .setAccessToken("access-token-123")
                .setTokenType("Bearer")
                .setExpiresIn(3600)
                .setScope("profile email")
                .build();

        SafeParcelableTestUtil.assertSafeParcelableRoundTrip(AuthorizationServiceResponse.CREATOR, response);
    }

    @Test
    public void testToString() {
        AuthorizationServiceResponse response = new AuthorizationServiceResponse.Builder()
                .setAccessToken("access-token-123")
                .setTokenType("Bearer")
                .setExpiresIn(3600)
                .setScope("profile email")
                .build();

        String string = response.toString();
        assertThat(string).contains("access-token-123");
        assertThat(string).contains("Bearer");
        assertThat(string).contains("3600");
        assertThat(string).contains("profile email");
    }
          }
