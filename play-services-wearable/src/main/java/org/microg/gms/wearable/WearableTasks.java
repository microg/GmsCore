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

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.TaskCompletionSource;

final class WearableTasks {
    private WearableTasks() {}

    static void completeVoid(TaskCompletionSource<Void> tcs, int statusCode) {
        complete(tcs, statusCode, null);
    }

    static <T> void complete(TaskCompletionSource<T> tcs, int statusCode, T value) {
        if (statusCode == 0) {
            tcs.setResult(value);
        } else {
            tcs.setException(new ApiException(new Status(statusCode)));
        }
    }
}
