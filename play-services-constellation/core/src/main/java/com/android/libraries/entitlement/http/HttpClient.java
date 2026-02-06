/*
 * Copyright (C) 2021 The Android Open Source Project
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

import static com.android.libraries.entitlement.ServiceEntitlementException.ERROR_HTTP_STATUS_NOT_SUCCESS;
import static com.android.libraries.entitlement.ServiceEntitlementException.ERROR_MALFORMED_HTTP_RESPONSE;
import static com.android.libraries.entitlement.ServiceEntitlementException.ERROR_SERVER_NOT_CONNECTABLE;
import static com.android.libraries.entitlement.http.HttpConstants.RequestMethod.POST;
import static com.android.libraries.entitlement.utils.DebugUtils.logPii;

import static com.google.common.base.Strings.nullToEmpty;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.SECONDS;

import android.net.Network;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.WorkerThread;

import com.android.libraries.entitlement.ServiceEntitlementException;
import com.android.libraries.entitlement.http.HttpConstants.ContentType;
import com.android.libraries.entitlement.utils.StreamUtils;
import com.android.libraries.entitlement.utils.UrlConnectionFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.net.HttpHeaders;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Implement the HTTP request method according to TS.43 specification. */
public class HttpClient {
    private static final String TAG = "ServiceEntitlement";

    private HttpURLConnection mConnection;
    private boolean mSaveHistory;
    private ArrayList<String> mHistory;

    public HttpClient(boolean saveHistory) {
        mSaveHistory = saveHistory;
        mHistory = new ArrayList<>();
    }

    @WorkerThread
    public HttpResponse request(HttpRequest request) throws ServiceEntitlementException {
        if (mSaveHistory) {
            mHistory.add(request.toString());
        }
        logPii("HttpClient.request url: " + request.url());
        createConnection(request);
        logPii("HttpClient.request headers (partial): " + mConnection.getRequestProperties());
        try {
            if (POST.equals(request.requestMethod())) {
                try (OutputStream out = new DataOutputStream(mConnection.getOutputStream())) {
                    // Android JSON toString() escapes forward-slash with back-slash. It's not
                    // supported by some vendor and not mandatory in JSON spec. Undo escaping.
                    String postData = request.postData().toString().replace("\\/", "/");
                    out.write(postData.getBytes(UTF_8));
                    logPii("HttpClient.request post data: " + postData);
                }
            }
            mConnection.connect(); // This is to trigger SocketTimeoutException early
            HttpResponse response = getHttpResponse(mConnection);
            Log.d(TAG, "HttpClient.response : " + response.toShortDebugString());
            if (mSaveHistory) {
                mHistory.add(response.toString());
            }
            return response;
        } catch (IOException ioe) {
            throw new ServiceEntitlementException(
                    ERROR_HTTP_STATUS_NOT_SUCCESS,
                    "Connection error stream: "
                            + StreamUtils.inputStreamToStringSafe(mConnection.getErrorStream())
                            + " IOException: "
                            + ioe.toString(),
                    ioe);
        } finally {
            closeConnection();
        }
    }

    /**
     * Retrieves the history of past HTTP request and responses.
     */
    public List<String> getHistory() {
        return mHistory;
    }

    /**
     * Clears the history of past HTTP request and responses.
     */
    public void clearHistory() {
        mHistory.clear();
    }

    private void createConnection(HttpRequest request) throws ServiceEntitlementException {
        try {
            URL url = new URL(request.url());
            UrlConnectionFactory urlConnectionFactory = request.urlConnectionFactory();
            Network network = request.network();
            if (network != null) {
                mConnection = (HttpURLConnection) network.openConnection(url);
            } else if (urlConnectionFactory != null) {
                mConnection = (HttpURLConnection) urlConnectionFactory.openConnection(url);
            } else  {
                mConnection = (HttpURLConnection) url.openConnection();
            }

            mConnection.setInstanceFollowRedirects(false);
            // add HTTP headers
            for (Map.Entry<String, String> entry : request.requestProperties().entries()) {
                mConnection.addRequestProperty(entry.getKey(), entry.getValue());
            }

            // set parameters
            mConnection.setRequestMethod(request.requestMethod());
            mConnection.setConnectTimeout((int) SECONDS.toMillis(request.timeoutInSec()));
            mConnection.setReadTimeout((int) SECONDS.toMillis(request.timeoutInSec()));
            if (POST.equals(request.requestMethod())) {
                mConnection.setDoOutput(true);
            }
        } catch (IOException ioe) {
            throw new ServiceEntitlementException(
                    ERROR_SERVER_NOT_CONNECTABLE, "Configure connection failed!", ioe);
        }
    }

    private void closeConnection() {
        if (mConnection != null) {
            mConnection.disconnect();
            mConnection = null;
        }
    }

    private static HttpResponse getHttpResponse(HttpURLConnection connection)
            throws ServiceEntitlementException {
        HttpResponse.Builder responseBuilder = HttpResponse.builder();
        responseBuilder.setContentType(getContentType(connection));
        try {
            int responseCode = connection.getResponseCode();
            logPii("HttpClient.response headers: " + connection.getHeaderFields());
            if (responseCode != HttpURLConnection.HTTP_OK
                    && responseCode != HttpURLConnection.HTTP_MOVED_TEMP) {
                throw new ServiceEntitlementException(ERROR_HTTP_STATUS_NOT_SUCCESS, responseCode,
                        connection.getHeaderField(HttpHeaders.RETRY_AFTER),
                        "Invalid connection response: " + responseCode);
            }
            responseBuilder.setResponseCode(responseCode);
            responseBuilder.setResponseMessage(nullToEmpty(connection.getResponseMessage()));
            responseBuilder.setLocation(
                    nullToEmpty(connection.getHeaderField(HttpHeaders.LOCATION)));
        } catch (IOException e) {
            throw new ServiceEntitlementException(
                    ERROR_HTTP_STATUS_NOT_SUCCESS, "Read response code failed!", e);
        }
        responseBuilder.setCookies(getCookies(connection));
        try {
            // {@code CronetHttpURLConnection.getInputStream()} throws if the
            // caller tries to read the response body of a redirect when
            // redirects are disabled.
            String responseBody =
                    connection.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP
                            ? ""
                            : readResponse(connection);
            logPii("HttpClient.response body: " + responseBody);
            responseBuilder.setBody(responseBody);
        } catch (IOException e) {
            throw new ServiceEntitlementException(
                    ERROR_MALFORMED_HTTP_RESPONSE, "Read response body/message failed!", e);
        }
        return responseBuilder.build();
    }

    private static String readResponse(URLConnection connection) throws IOException {
        try (InputStream in = connection.getInputStream()) {
            return StreamUtils.inputStreamToStringSafe(in);
        }
    }

    private static int getContentType(URLConnection connection) {
        String contentType = connection.getHeaderField(ContentType.NAME);
        if (TextUtils.isEmpty(contentType)) {
            return ContentType.UNKNOWN;
        }

        if (contentType.contains("xml")) {
            return ContentType.XML;
        } else if ("text/vnd.wap.connectivity".equals(contentType)) {
            // Workaround that a server vendor uses this type for XML
            return ContentType.XML;
        } else if (contentType.contains("json")) {
            return ContentType.JSON;
        }
        return ContentType.UNKNOWN;
    }

    private static List<String> getCookies(URLConnection connection) {
        List<String> cookies = connection.getHeaderFields().get(HttpHeaders.SET_COOKIE);
        return cookies == null ? ImmutableList.of() : cookies;
    }
}
