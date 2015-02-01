/*
 * Copyright 2013-2015 µg Project Team
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

package org.microg.gms.auth;

import android.util.Log;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class AuthResponse {
    private static final String TAG = "GmsAuthResponse";

    @ResponseField("SID")
    public String Sid;
    @ResponseField("LSID")
    public String LSid;
    @ResponseField("Auth")
    public String auth;
    @ResponseField("Token")
    public String token;
    @ResponseField("Email")
    public String email;
    @ResponseField("services")
    public Set<String> services = new HashSet<>();
    @ResponseField("GooglePlusUpgrade")
    public boolean isGooglePlusUpgrade;
    @ResponseField("PicasaUser")
    public String picasaUserName;
    @ResponseField("RopText")
    public String ropText;
    @ResponseField("RopRevision")
    public int ropRevision;
    @ResponseField("firstName")
    public String firstName;
    @ResponseField("lastName")
    public String lastName;
    @ResponseField("issueAdvice")
    public String issueAdvice;

    public static AuthResponse parse(String result) {
        AuthResponse response = new AuthResponse();
        String[] entries = result.split("\n");
        for (String s : entries) {
            String[] keyValuePair = s.split("=");
            String key = keyValuePair[0].trim();
            String value = keyValuePair[1].trim();
            try {
                for (Field field : AuthResponse.class.getDeclaredFields()) {
                    if (field.isAnnotationPresent(ResponseField.class) &&
                            key.equals(field.getAnnotation(ResponseField.class).value())) {
                        if (field.getType().equals(String.class)) {
                            field.set(response, value);
                        } else if (field.getType().equals(boolean.class)) {
                            field.setBoolean(response, value.equals("1"));
                        } else if (field.getType().equals(int.class)) {
                            field.setInt(response, Integer.parseInt(value));
                        } else if (field.getType().isAssignableFrom(Set.class)) {
                            //noinspection unchecked
                            ((Set)field.get(response)).addAll(Arrays.asList(value.split(",")));
                        }
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, e);
            }
        }
        return response;
    }



    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    private @interface ResponseField {
        public String value();
    }
}
