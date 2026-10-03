/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.gms.auth;

import org.junit.Test;
import static org.junit.Assert.*;

public class AuthRequestLoggingTest {
    @Test public void webloginNeverLogsContinuationOrTokens() {
        AuthRequest request = new AuthRequest();
        request.service = "weblogin:continue=https%3A%2F%2Faccounts.google.com%2Ffixture%3Fsession%3Dsynthetic";
        request.token = "synthetic-token";
        assertFalse(request.isContentLoggingAllowed());
        request.service = "weblogin:url=https://accounts.google.com";
        assertFalse(request.isContentLoggingAllowed());
    }
    @Test public void oauthAndUninitializedAuthRequestsNeverLogCredentials() {
        AuthRequest request = new AuthRequest();
        assertFalse(request.isContentLoggingAllowed());
        request.service = "oauth2:synthetic-scope";
        assertFalse(request.isContentLoggingAllowed());
    }
}
