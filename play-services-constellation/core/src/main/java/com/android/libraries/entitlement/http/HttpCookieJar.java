/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.libraries.entitlement.http;

import static com.android.libraries.entitlement.utils.DebugUtils.logPii;

import com.google.common.collect.ImmutableList;

import java.net.HttpCookie;
import java.util.List;

/**
 * Simple cookie management.
 *
 * <p>Use {@link #parseSetCookieHeaders} to parse the "Set-Cookie" headers in HTTP responses
 * from the server, and use {@link #toCookieHeaders} to generate the "Cookie" headers in
 * follow-up HTTP requests.
 */
public class HttpCookieJar {
    private final ImmutableList<HttpCookie> mCookies;

    private HttpCookieJar(ImmutableList<HttpCookie> cookies) {
        mCookies = cookies;
    }

    /**
     * Parses the "Set-Cookie" headers in HTTP responses from servers.
     */
    public static HttpCookieJar parseSetCookieHeaders(List<String> rawCookies) {
        ImmutableList.Builder<HttpCookie> parsedCookies = ImmutableList.builder();
        for (String rawCookie : rawCookies) {
            List<HttpCookie> cookies = parseCookiesSafely(rawCookie);
            parsedCookies.addAll(cookies);
        }
        return new HttpCookieJar(parsedCookies.build());
    }

    /**
     * Returns the cookies as "Cookie" headers in HTTP requests to servers.
     */
    public ImmutableList<String> toCookieHeaders() {
        ImmutableList.Builder<String> cookieHeader = ImmutableList.builder();
        for (HttpCookie cookie : mCookies) {
            cookieHeader.add(removeObsoleteCookieAttributes(cookie).toString());
        }
        return cookieHeader.build();
    }

    private static List<HttpCookie> parseCookiesSafely(String rawCookie) {
        try {
            return HttpCookie.parse(rawCookie);
        } catch (IllegalArgumentException e) {
            logPii("Failed to parse cookie: " + rawCookie);
            return ImmutableList.of();
        }
    }

    /**
     * Removes some attributes of the cookie that should not be set in HTTP requests.
     *
     * <p>Unfortunately, {@link HttpCookie#toString()} preserves some cookie attributes:
     * Domain, Path, and Port as per RFC 2965. Such behavior is obsoleted by the RFC 6265.
     *
     * <p>To be clear, Domain and Path are valid attributes by RFC 6265, but cookie attributes
     * be set in HTTP request "Cookie" headers.
     */
    private static HttpCookie removeObsoleteCookieAttributes(HttpCookie cookie) {
        cookie.setDomain(null);
        cookie.setPath(null);
        cookie.setPortlist(null);
        return cookie;
    }
}
