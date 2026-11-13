/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.mlkit.common;

import androidx.annotation.IntDef;
import org.microg.gms.common.PublicApi;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@PublicApi
public class MlKitException extends Exception {
    public static final int CANCELLED = 1;
    public static final int UNKNOWN = 2;
    public static final int INVALID_ARGUMENT = 3;
    public static final int DEADLINE_EXCEEDED = 4;
    public static final int NOT_FOUND = 5;
    public static final int ALREADY_EXISTS = 6;
    public static final int PERMISSION_DENIED = 7;
    public static final int RESOURCE_EXHAUSTED = 8;
    public static final int FAILED_PRECONDITION = 9;
    public static final int ABORTED = 10;
    public static final int OUT_OF_RANGE = 11;
    public static final int UNIMPLEMENTED = 12;
    public static final int INTERNAL = 13;
    public static final int UNAVAILABLE = 14;
    public static final int DATA_LOSS = 15;
    public static final int UNAUTHENTICATED = 16;
    public static final int NETWORK_ISSUE = 17;
    public static final int UNSUPPORTED = 18;
    public static final int MODEL_INCOMPATIBLE_WITH_TFLITE = 100;
    public static final int NOT_ENOUGH_SPACE = 101;
    public static final int MODEL_HASH_MISMATCH = 102;
    public static final int CODE_SCANNER_UNAVAILABLE = 200;
    public static final int CODE_SCANNER_CANCELLED = 201;
    public static final int CODE_SCANNER_CAMERA_PERMISSION_NOT_GRANTED = 202;
    public static final int CODE_SCANNER_APP_NAME_UNAVAILABLE = 203;
    public static final int CODE_SCANNER_TASK_IN_PROGRESS = 204;
    public static final int CODE_SCANNER_PIPELINE_INITIALIZATION_ERROR = 205;
    public static final int CODE_SCANNER_PIPELINE_INFERENCE_ERROR = 206;
    public static final int CODE_SCANNER_GOOGLE_PLAY_SERVICES_VERSION_TOO_OLD = 207;

    @IntDef({CANCELLED, UNKNOWN, INVALID_ARGUMENT, DEADLINE_EXCEEDED, NOT_FOUND,
            ALREADY_EXISTS, PERMISSION_DENIED, RESOURCE_EXHAUSTED, FAILED_PRECONDITION,
            ABORTED, OUT_OF_RANGE, UNIMPLEMENTED, INTERNAL, UNAVAILABLE, DATA_LOSS,
            UNAUTHENTICATED, NETWORK_ISSUE, UNSUPPORTED, MODEL_INCOMPATIBLE_WITH_TFLITE,
            NOT_ENOUGH_SPACE, MODEL_HASH_MISMATCH, CODE_SCANNER_UNAVAILABLE,
            CODE_SCANNER_CANCELLED, CODE_SCANNER_CAMERA_PERMISSION_NOT_GRANTED,
            CODE_SCANNER_APP_NAME_UNAVAILABLE, CODE_SCANNER_TASK_IN_PROGRESS,
            CODE_SCANNER_PIPELINE_INITIALIZATION_ERROR, CODE_SCANNER_PIPELINE_INFERENCE_ERROR,
            CODE_SCANNER_GOOGLE_PLAY_SERVICES_VERSION_TOO_OLD})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ErrorCode {
    }

    private final int errorCode;

    public MlKitException(String message, @ErrorCode int errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public MlKitException(String message, @ErrorCode int errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    @ErrorCode
    public int getErrorCode() {
        return errorCode;
    }
}
