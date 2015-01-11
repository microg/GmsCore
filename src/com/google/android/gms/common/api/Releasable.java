package com.google.android.gms.common.api;

/**
 * Represents a resource, or a holder of resources, which may be released once they are no longer
 * needed.
 */
public interface Releasable {
    public void release();
}
