/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.recaptcha;

/**
 * Class for wrapping an optional object (i.e., an immutable object that may contain a non-null reference to another
 * object) to be used in {@link VerificationResult}.
 */
public class RecaptchaOptionalObject<T> {
    private T object;

    private RecaptchaOptionalObject(T object) {
        this.object = object;
    }

    /**
     * Returns a {@link RecaptchaOptionalObject} wrapping the specified object, which can be {@code null}.
     */
    public static <T> RecaptchaOptionalObject<T> ofNullable(T object) {
        return new RecaptchaOptionalObject<>(object);
    }

    /**
     * Returns the wrapped object.
     */
    public T orNull() {
        return object;
    }
}
