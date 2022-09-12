/*
 * Copyright (C) 2013-2017 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.microg.gms.common;

import android.net.Uri;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class HttpFormClient {
    private static final String TAG = "GmsHttpFormClient";

    public static <T> T request(String url, Request request, Class<T> tClass) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("POST");
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        StringBuilder content = new StringBuilder();
        request.prepare();
        for (Field field : request.getClass().getDeclaredFields()) {
            try {
                field.setAccessible(true);
                Object objVal = field.get(request);
                if (field.isAnnotationPresent(RequestContentDynamic.class)) {
                    Map<String, String> contentParams = (Map<String, String>) objVal;
                    for (Map.Entry<String, String> param : contentParams.entrySet()) {
                        appendParam(content, param.getKey(), param.getValue());
                    }
                    continue;
                }
                String value = objVal != null ? String.valueOf(objVal) : null;
                Boolean boolVal = null;
                if (field.getType().equals(boolean.class)) {
                    boolVal = field.getBoolean(request);
                }
                if (field.isAnnotationPresent(RequestHeader.class)) {
                    RequestHeader annotation = field.getAnnotation(RequestHeader.class);
                    value = valueFromBoolVal(value, boolVal, annotation.truePresent(), annotation.falsePresent());
                    if (value != null || annotation.nullPresent()) {
                        for (String key : annotation.value()) {
                            connection.setRequestProperty(key, String.valueOf(value));
                        }
                    }
                }
                if (field.isAnnotationPresent(RequestContent.class)) {
                    RequestContent annotation = field.getAnnotation(RequestContent.class);
                    value = valueFromBoolVal(value, boolVal, annotation.truePresent(), annotation.falsePresent());
                    if (value != null || annotation.nullPresent()) {
                        for (String key : annotation.value()) {
                            appendParam(content, key, value);
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        Log.d(TAG, "-- Request --\n" + content);
        OutputStream os = connection.getOutputStream();
        os.write(content.toString().getBytes());
        os.close();

        if (connection.getResponseCode() != 200) {
            String error = connection.getResponseMessage();
            try {
                error = new String(Utils.readStreamToEnd(connection.getErrorStream()));
            } catch (IOException e) {
                // Ignore
            }
            throw new IOException(error);
        }

        String result = new String(Utils.readStreamToEnd(connection.getInputStream()));
        Log.d(TAG, "-- Response --\n" + result);
        return parseResponse(tClass, connection, result);
    }

    private static String valueFromBoolVal(String value, Boolean boolVal, boolean truePresent, boolean falsePresent) {
        if (boolVal != null) {
            if (boolVal && truePresent) {
                return "1";
            } else if (!boolVal && falsePresent) {
                return "0";
            } else {
                return null;
            }
        } else {
            return value;
        }
    }

    private static void appendParam(StringBuilder content, String key, String value) {
        if (content.length() > 0)
            content.append("&");
        content.append(Uri.encode(key)).append("=").append(Uri.encode(String.valueOf(value)));
    }

    private static <T> T parseResponse(Class<T> tClass, HttpURLConnection connection, String result) throws IOException {
        Map<String, List<String>> headerFields = connection.getHeaderFields();
        T response;
        try {
            response = tClass.getConstructor().newInstance();
        } catch (Exception e) {
            return null;
        }
        String[] entries = result.split("\n");
        for (String s : entries) {
            String[] keyValuePair = s.split("=", 2);
            String key = keyValuePair[0].trim();
            String value = keyValuePair[1].trim();
            boolean matched = false;
            try {
                for (Field field : tClass.getDeclaredFields()) {
                    if (field.isAnnotationPresent(ResponseField.class) &&
                            key.equals(field.getAnnotation(ResponseField.class).value())) {
                        matched = true;
                        if (field.getType().equals(String.class)) {
                            field.set(response, value);
                        } else if (field.getType().equals(boolean.class)) {
                            field.setBoolean(response, value.equals("1"));
                        } else if (field.getType().equals(long.class)) {
                            field.setLong(response, Long.parseLong(value));
                        } else if (field.getType().equals(int.class)) {
                            field.setInt(response, Integer.parseInt(value));
                        }
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, e);
            }
            if (!matched) {
                Log.w(TAG, "Response line '" + s + "' not processed");
            }
        }
        for (Field field : tClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(ResponseHeader.class)) {
                List<String> strings = headerFields.get(field.getAnnotation(ResponseHeader.class).value());
                if (strings == null || strings.size() != 1) continue;
                String value = strings.get(0);
                try {
                    if (field.getType().equals(String.class)) {
                        field.set(response, value);
                    } else if (field.getType().equals(boolean.class)) {
                        field.setBoolean(response, value.equals("1"));
                    } else if (field.getType().equals(long.class)) {
                        field.setLong(response, Long.parseLong(value));
                    } else if (field.getType().equals(int.class)) {
                        field.setInt(response, Integer.parseInt(value));
                    }
                } catch (Exception e) {
                    Log.w(TAG, e);
                }
            }
            if (field.isAnnotationPresent(ResponseStatusCode.class) && field.getType() == int.class) {
                try {
                    field.setInt(response, connection.getResponseCode());
                } catch (IllegalAccessException e) {
                    Log.w(TAG, e);
                }
            }
            if (field.isAnnotationPresent(ResponseStatusText.class) && field.getType() == String.class) {
                try {
                    field.set(response, connection.getResponseMessage());
                } catch (IllegalAccessException e) {
                    Log.w(TAG, e);
                }
            }
        }
        return response;
    }

    public static <T> void requestAsync(final String url, final Request request, final Class<T> tClass,
                                        final Callback<T> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    callback.onResponse(request(url, request, tClass));
                } catch (Exception e) {
                    callback.onException(e);
                }
            }
        }).start();
    }

    public static abstract class Request {
        protected void prepare() {
        }
    }

    public interface Callback<T> {
        void onResponse(T response);

        void onException(Exception exception);
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface RequestHeader {
        public String[] value();

        public boolean truePresent() default true;

        public boolean falsePresent() default false;

        public boolean nullPresent() default false;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface RequestContent {
        public String[] value();

        public boolean truePresent() default true;

        public boolean falsePresent() default false;

        public boolean nullPresent() default false;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface RequestContentDynamic {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface ResponseField {
        public String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface ResponseHeader {
        public String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface ResponseStatusCode {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface ResponseStatusText {
    }
}
