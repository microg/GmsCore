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

/**
 * Http constants using for entitlement flow of TS.43.
 */
public final class HttpConstants {
    private HttpConstants() {
    }

    /**
     * Possible request methods for Entitlement server response.
     */
    public static final class RequestMethod {
        private RequestMethod() {
        }

        public static final String GET = "GET";
        public static final String POST = "POST";
    }

    /**
     * Possible content type for Entitlement server response.
     */
    public static final class ContentType {
        private ContentType() {
        }

        public static final int UNKNOWN =
                com.android.libraries.entitlement.utils.HttpConstants.UNKNOWN;
        public static final int JSON =
                com.android.libraries.entitlement.utils.HttpConstants.JSON;
        public static final int XML =
                com.android.libraries.entitlement.utils.HttpConstants.XML;

        public static final String NAME = "Content-Type";
    }
}
