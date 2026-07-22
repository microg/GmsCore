/*
 * SPDX-FileCopyrightText: 2016, microG Project Team
 * SPDX-License-Identifier: Apache-2.0 AND CC-BY-4.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.tasks;

import org.microg.gms.common.PublicApi;

import java.util.concurrent.ExecutionException;

/**
 * Runtime version of {@link ExecutionException}.
 *
 * @see Task#getResult(Class)
 */
@PublicApi
public class RuntimeExecutionException extends RuntimeException {
    public RuntimeExecutionException(Throwable cause) {
        super(cause);
    }
}
