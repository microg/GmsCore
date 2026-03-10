/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.cast.framework;

/**
 * An exception thrown when the internal Cast module fails to load.
 */
public class ModuleUnavailableException extends Exception {
    public ModuleUnavailableException(Throwable cause) {
        super(cause);
    }
}
