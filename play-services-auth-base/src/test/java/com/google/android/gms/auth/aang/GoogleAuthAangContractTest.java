/*
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.auth.aang;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GoogleAuthAangContractTest {
    private static final String ACTION = "com.google.android.gms.auth.aang.events.services.START";

    @Test
    public void serviceIdentityAndFeatureContract() throws Exception {
        String gmsService = source("play-services-basement/src/main/java/org/microg/gms/common/GmsService.java");
        String manifest = source("play-services-core/src/main/AndroidManifest.xml");
        String service = source("play-services-core/src/main/kotlin/org/microg/gms/auth/aang/GoogleAuthAangService.kt");
        assertTrue(gmsService.contains("GOOGLE_AUTH_AANG(343, \"" + ACTION + "\")"));
        assertTrue(manifest.contains("org.microg.gms.auth.aang.GoogleAuthAangService"));
        assertTrue(manifest.contains(ACTION));
        assertTrue(service.contains("Feature(\"google_auth_api\", 1)"));
        assertFalse(service.contains("Feature(\"sync_account_state_api\""));
        assertFalse(service.contains("Feature(\"embedded_reauth\""));
    }

    @Test
    public void serviceBinderTransactionsMatchStockContract() throws Exception {
        String aidl = source("play-services-auth-base/src/main/aidl/com/google/android/gms/auth/aang/internal/IGoogleAuthAangService.aidl");
        assertTrue(aidl.contains("getAccounts(IGoogleAuthAangCallbacks callback, in GetAccountsRequest request) = 0;"));
        assertTrue(aidl.contains("getToken(IGoogleAuthAangCallbacks callback, in GetTokenRequest request) = 1;"));
        assertTrue(aidl.contains("clearToken(IStatusCallback callback, String token) = 2;"));
        assertTrue(aidl.contains("hasCapabilities(IGoogleAuthAangCallbacks callback, in HasCapabilitiesRequest request) = 3;"));
        assertTrue(aidl.contains("fetchAppRestriction(IGoogleAuthAangCallbacks callback, in FetchAppRestrictionRequest request) = 4;"));
    }

    @Test
    public void callbackBinderTransactionsMatchStockContract() throws Exception {
        String aidl = source("play-services-auth-base/src/main/aidl/com/google/android/gms/auth/aang/internal/IGoogleAuthAangCallbacks.aidl");
        assertTrue(aidl.contains("onGetAccounts(in Status status, in GetAccountsResponse response) = 0;"));
        assertTrue(aidl.contains("onGetToken(in Status status, in GetTokenResponse response) = 1;"));
        assertTrue(aidl.contains("onHasCapabilities(in Status status, int result) = 2;"));
        assertTrue(aidl.contains("onFetchAppRestriction(in Status status, in AppRestriction restriction) = 3;"));
    }

    @Test
    public void safeParcelableFieldIdsMatchRecoveredContract() throws Exception {
        assertFieldIds("AccountWithAppRestrictionState.java", 1, 2);
        assertFieldIds("AppRestriction.java", 1, 2);
        assertFieldIds("AppRestrictionInfo.java", 1, 3, 4, 5, 6);
        assertFieldIds("AppRestrictionState.java", 1, 2);
        assertFieldIds("FetchAppRestrictionRequest.java", 1, 2);
        assertFieldIds("GetAccountsRequest.java", 1, 2, 3, 4);
        assertFieldIds("GetAccountsResponse.java", 1, 2);
        assertFieldIds("GetTokenRequest.java", 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15);
        assertFieldIds("GetTokenResponse.java", 1, 2);
        assertFieldIds("GoogleAccount.java", 1, 2, 3);
        assertFieldIds("HasCapabilitiesRequest.java", 1, 2);
        assertFieldIds("Oauth2TokenMetadata.java", 1, 2);
    }

    private static void assertFieldIds(String file, Integer... expected) throws Exception {
        String text = source("play-services-auth-base/src/main/java/com/google/android/gms/auth/aang/" + file);
        Matcher matcher = Pattern.compile("@Field\\((\\d+)\\)").matcher(text);
        List<Integer> actual = new ArrayList<>();
        while (matcher.find()) actual.add(Integer.parseInt(matcher.group(1)));
        assertEquals(Arrays.asList(expected), actual);
    }

    private static String source(String relative) throws IOException {
        Path root = repoRoot();
        return new String(Files.readAllBytes(root.resolve(relative)), StandardCharsets.UTF_8);
    }

    private static Path repoRoot() {
        Path current = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null && !Files.isDirectory(current.resolve("play-services-core"))) current = current.getParent();
        if (current == null) throw new IllegalStateException("Repository root not found");
        return current;
    }
}
