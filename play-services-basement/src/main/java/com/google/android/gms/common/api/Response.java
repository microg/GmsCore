/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.common.api;

import org.microg.gms.common.PublicApi;

/**
 * Represents the successful result of invoking an API method in Google Play services using a subclass of GoogleApi.
 * Wraps a instance of a {@link Result}.
 */
@PublicApi
public class Response<T extends Result> {
    private T result;

    public Response() {
    }

    protected Response(T result) {
        this.result = result;
    }

    public T getResult() {
        return result;
    }

    public void setResult(T result) {
        this.result = result;
    }
}
