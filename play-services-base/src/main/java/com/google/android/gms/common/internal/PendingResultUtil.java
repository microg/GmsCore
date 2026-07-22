/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.common.internal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Response;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import org.microg.gms.common.Hide;

import java.util.concurrent.TimeUnit;

@Hide
public class PendingResultUtil {
    public interface ResultConverter<R extends Result, T> {
        @Nullable
        T convert(@NonNull R r);
    }

    @NonNull
    public static <R extends Result, T> Task<T> toTask(@NonNull PendingResult<R> pendingResult, @NonNull ResultConverter<R, T> resultConverter) {
        TaskCompletionSource<T> taskCompletionSource = new TaskCompletionSource<>();
        pendingResult.addStatusListener((status) -> {
            if (!status.isSuccess()) {
                taskCompletionSource.setException(ApiExceptionUtil.fromStatus(status));
            } else {
                taskCompletionSource.setResult(resultConverter.convert(pendingResult.await(0, TimeUnit.MILLISECONDS)));
            }
        });
        return taskCompletionSource.getTask();
    }

    @NonNull
    public static <R extends Result, T extends Response<R>> Task<T> toResponseTask(@NonNull PendingResult<R> pendingResult, @NonNull T t) {
        return toTask(pendingResult, (result) -> {
            t.setResult(result);
            return t;
        });
    }

    @NonNull
    public static <R extends Result> Task<Void> toVoidTask(@NonNull PendingResult<R> pendingResult) {
        return toTask(pendingResult, (result) -> null);
    }
}
