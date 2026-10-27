/*
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.gms.wearable;

import java.util.ArrayList;
import java.util.List;

/** Package-scoped selection for the multi-item Data API operations. */
final class DataItemQuery {
    static final int FILTER_LITERAL = 0;
    static final int FILTER_PREFIX = 1;

    final String selection;
    final String[] arguments;

    private DataItemQuery(String selection, List<String> arguments) {
        this.selection = selection;
        this.arguments = arguments.toArray(new String[0]);
    }

    static DataItemQuery all(String packageName, String signatureDigest) {
        List<String> arguments = new ArrayList<>();
        arguments.add(packageName);
        arguments.add(signatureDigest);
        return new DataItemQuery("packageName = ? AND signatureDigest = ? AND deleted = 0", arguments);
    }

    static DataItemQuery filtered(String packageName, String signatureDigest, String host,
                                  String path, int filterType) {
        if (filterType != FILTER_LITERAL && filterType != FILTER_PREFIX) {
            throw new IllegalArgumentException("Unsupported data item filter type");
        }
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("A data item URI must contain a path");
        }
        List<String> arguments = new ArrayList<>();
        arguments.add(packageName);
        arguments.add(signatureDigest);
        String selection = "packageName = ? AND signatureDigest = ? AND deleted = 0";
        if (host != null && !host.isEmpty() && !"*".equals(host)) {
            selection += " AND host = ?";
            arguments.add(host);
        }
        if (filterType == FILTER_PREFIX) {
            // LIKE is case-insensitive and treats '%' and '_' as wildcards. Paths are literal.
            selection += " AND substr(path, 1, length(?)) = ? COLLATE BINARY";
            arguments.add(path);
        } else {
            selection += " AND path = ? COLLATE BINARY";
        }
        arguments.add(path);
        return new DataItemQuery(selection, arguments);
    }
}
