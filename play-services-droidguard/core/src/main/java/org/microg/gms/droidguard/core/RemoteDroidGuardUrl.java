/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard.core;

import java.net.MalformedURLException;
import java.net.URL;

final class RemoteDroidGuardUrl {
    private RemoteDroidGuardUrl() {}

    /** Appends already percent-encoded parameters without altering the server's query. */
    static URL appendQuery(String endpoint, String parameters) throws MalformedURLException {
        URL base = new URL(endpoint);
        String external = base.toExternalForm();
        int fragment = external.indexOf('#');
        if (fragment >= 0) external = external.substring(0, fragment);
        if (parameters.isEmpty()) return new URL(external);
        String separator = base.getQuery() == null ? "?" : base.getQuery().isEmpty() ? "" : "&";
        return new URL(external + separator + parameters);
    }
}
