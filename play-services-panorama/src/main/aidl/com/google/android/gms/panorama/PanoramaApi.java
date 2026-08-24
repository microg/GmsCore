/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.panorama;

import android.content.Intent;
import android.net.Uri;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Result;

/**
 * The main entry point for interacting with Panorama viewer. This class provides methods for obtaining an Intent to view a Panorama.
 */
@Deprecated
public interface PanoramaApi {
    /**
     * Loads information about a panorama.
     *
     * @param uri the URI of the panorama to load info about. May be a file:, content:, or android_resource: scheme.
     */
    @Deprecated
    PendingResult<PanoramaApi.PanoramaResult> loadPanoramaInfo(GoogleApiClient client, Uri uri);

    /**
     * Loads information about a panorama from a content provider. This method will also explicitly grant and revoke access to
     * the URI while the load is happening so images in content providers may be inspected without giving permission to an
     * entire content provider. The returned viewer intent will also have the {@link Intent#FLAG_GRANT_READ_URI_PERMISSION} set so
     * the viewer has access.
     */
    @Deprecated
    PendingResult<PanoramaApi.PanoramaResult> loadPanoramaInfoAndGrantAccess(GoogleApiClient client, Uri uri);

    /**
     * Result interface for loading panorama info.
     */
    @Deprecated
    interface PanoramaResult extends Result {
        /**
         * Returns if the image is a panorama this is not null and will launch a viewer when started. If the image is not a panorama this will be null.
         */
        @Deprecated
        Intent getViewerIntent();
    }
}
