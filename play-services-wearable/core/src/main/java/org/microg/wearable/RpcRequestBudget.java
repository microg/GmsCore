/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.wearable;

import java.util.HashMap;
import java.util.Map;

/** Admission remains occupied until the caller consumes its completion, even if Binder blocks. */
public final class RpcRequestBudget {
    private final Map<String, Integer> packages = new HashMap<>();
    private int total;

    public final class Lease {
        private final String packageName;
        private boolean released;

        private Lease(String packageName) { this.packageName = packageName; }

        public void release() {
            synchronized (RpcRequestBudget.this) {
                if (released) return;
                released = true;
                int remaining = packages.get(packageName) - 1;
                if (remaining == 0) packages.remove(packageName);
                else packages.put(packageName, remaining);
                total--;
            }
        }
    }

    public synchronized Lease acquire(String packageName) {
        int count = packages.containsKey(packageName) ? packages.get(packageName) : 0;
        if (total >= PendingRpcRequests.MAX_PENDING || count >= PendingRpcRequests.MAX_PENDING_PER_PACKAGE) return null;
        packages.put(packageName, count + 1);
        total++;
        return new Lease(packageName);
    }
}
