package org.microg.gms.wearable;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.wearable.ConnectionRestrictions;
import com.google.android.gms.wearable.DataItemFilter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ConnectionRestrictionFilter {
    private static final String TAG = "WearRestrictions";

    public static final int FILTER_UNSPECIFIED = 1;
    public static final int FILTER_PATTERN_LITERAL = 2;
    public static final int FILTER_PATTERN_PREFIX = 3;

    private ConnectionRestrictionFilter() {}

    public static boolean isValid(ConnectionRestrictions restrictions) {
        if (restrictions == null) return true;
        List<DataItemFilter> filters = restrictions.allowedDataItemFilters;
        if (filters == null) return true;
        Set<String> seen = new HashSet<>();
        for (DataItemFilter filter : filters) {
            if (filter == null || filter.uri == null) {
                Log.w(TAG, "Restriction set contains a null filter uri");
                return false;
            }
            if (!"wear".equals(filter.uri.getScheme())) {
                Log.w(TAG, "Restriction set contains non-wear uri: " + filter.uri);
                return false;
            }
            if (!seen.add(filter.uri + "#" + filter.filterType)) {
                Log.w(TAG, "Restriction set contains a duplicate filter: " + filter.uri);
                return false;
            }
        }
        return true;
    }

    public static boolean isPackageAllowed(ConnectionRestrictions restrictions, String packageName) {
        if (restrictions == null || restrictions.allowedPackages == null) return true;
        return packageName != null && restrictions.allowedPackages.contains(packageName);
    }

    public static boolean isCapabilityAllowed(ConnectionRestrictions restrictions, String capability) {
        if (restrictions == null || restrictions.allowedCapabilities == null) return true;
        return capability != null && restrictions.allowedCapabilities.contains(capability);
    }

    public static boolean isDataItemAllowed(ConnectionRestrictions restrictions,
                                            String packageName, Uri uri) {
        if (restrictions == null) return true;
        if (!isPackageAllowed(restrictions, packageName)) {
            return false;
        }
        List<DataItemFilter> filters = restrictions.allowedDataItemFilters;
        if (filters == null) return true;
        if (uri == null) return false;
        for (DataItemFilter filter : filters) {
            if (matches(filter, uri)) return true;
        }
        return false;
    }

    private static boolean matches(DataItemFilter filter, Uri uri) {
        if (filter == null || filter.uri == null) return false;

        String filterHost = filter.uri.getAuthority();
        if (!TextUtils.isEmpty(filterHost)
                && !filterHost.equals(uri.getAuthority())
                && !"*".equals(filterHost)) {
            return false;
        }

        String filterPath = filter.uri.getPath();
        String path = uri.getPath();
        if (TextUtils.isEmpty(filterPath)) return true;
        if (path == null) return false;

        if (filter.filterType == FILTER_PATTERN_LITERAL) {
            return filterPath.equals(path);
        }

        if (!path.startsWith(filterPath)) return false;
        return path.length() == filterPath.length()
                || filterPath.endsWith("/")
                || path.charAt(filterPath.length()) == '/';
    }
}