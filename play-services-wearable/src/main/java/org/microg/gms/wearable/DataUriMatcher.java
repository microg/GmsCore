/*
 * Copyright (C) 2026 microG Project Team
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

package org.microg.gms.wearable;

import com.google.android.gms.wearable.DataClient;

public final class DataUriMatcher {
    private DataUriMatcher() {
    }

    public static boolean isPrefix(int filterType) {
        return filterType == DataClient.FILTER_PREFIX;
    }

    public static boolean constrainsHost(String host) {
        return host != null && !host.isEmpty() && !"*".equals(host);
    }

    public static boolean matches(String itemHost, String itemPath, String queryHost, String queryPath, int filterType) {
        if (constrainsHost(queryHost) && (itemHost == null || !queryHost.equals(itemHost))) {
            return false;
        }
        if (queryPath == null) {
            return true;
        }
        if (itemPath == null) {
            return false;
        }
        if (isPrefix(filterType)) {
            return itemPath.startsWith(queryPath);
        }
        return itemPath.equals(queryPath);
    }

    public static String toLikePrefix(String path) {
        StringBuilder escaped = new StringBuilder(path.length() + 8);
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (c == '\\' || c == '%' || c == '_') {
                escaped.append('\\');
            }
            escaped.append(c);
        }
        escaped.append('%');
        return escaped.toString();
    }
}
