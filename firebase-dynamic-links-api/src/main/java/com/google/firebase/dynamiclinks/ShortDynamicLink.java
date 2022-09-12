/*
 * SPDX-FileCopyrightText: 2021, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.firebase.dynamiclinks;

import android.net.Uri;

import org.microg.gms.common.PublicApi;

import java.util.List;

/**
 * Response from {@link DynamicLink.Builder#buildShortDynamicLink()} that returns the shortened Dynamic Link, link flow chart, and warnings from the requested Dynamic Link.
 */
@PublicApi
public interface ShortDynamicLink {
    /**
     * Gets the preview link to show the link flow chart.
     */
    Uri getPreviewLink();

    /**
     * Gets the short Dynamic Link value.
     */
    Uri getShortLink();

    /**
     * Gets information about potential warnings on link creation.
     */
    List<? extends Warning> getWarnings();

    /**
     * Path generation option for short Dynamic Link length
     */
    @interface Suffix {
        /**
         * Shorten the path to an unguessable string. Such strings are created by base62-encoding randomly generated
         * 96-bit numbers, and consist of 17 alphanumeric characters. Use unguessable strings to prevent your Dynamic
         * Links from being crawled, which can potentially expose sensitive information.
         */
        int UNGUESSABLE = 1;
        /**
         * Shorten the path to a string that is only as long as needed to be unique, with a minimum length of 4
         * characters. Use this method if sensitive information would not be exposed if a short Dynamic Link URL were
         * guessed.
         */
        int SHORT = 2;
    }

    /**
     * Information about potential warnings on short Dynamic Link creation.
     */
    interface Warning {
        /**
         * Gets the warning code.
         *
         * @deprecated See {@link #getMessage()} for more information on this warning and how to correct it.
         */
        @Deprecated
        String getCode();

        /**
         * Gets the warning message to help developers improve their requests.
         */
        String getMessage();
    }
}
