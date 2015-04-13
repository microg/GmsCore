/*
 * Copyright 2013-2015 µg Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.gms.wearable;

import android.net.Uri;
import android.os.ParcelFileDescriptor;

import org.microg.gms.common.PublicApi;
import org.microg.safeparcel.AutoSafeParcelable;

/**
 * An asset is a binary blob shared between data items that is replicated across the wearable
 * network on demand.
 * <p/>
 * It may represent an asset not yet added with the Android Wear network. DataItemAssets
 * are representations of an asset after it has been added to the network through a
 * {@link PutDataRequest}.
 */
@PublicApi
public class Asset extends AutoSafeParcelable {

    /**
     * Creates an Asset using a byte array.
     */
    public static Asset createFromBytes(byte[] assetData) {
        return null;
    }

    /**
     * Creates an Asset using a file descriptor. The FD should be closed after being successfully
     * sent in a putDataItem request.
     */
    public static Asset createFromFd(ParcelFileDescriptor fd) {
        return null;
    }

    /**
     * Create an Asset using an existing Asset's digest.
     */
    public static Asset createFromRef(String digest) {
        return null;
    }

    /**
     * Creates an Asset using a content URI. Google Play services must have permission to read this
     * Uri.
     */
    public static Asset createFromUri(Uri uri) {
        return null;
    }

    /**
     * @return the digest associated with the asset data. A digest is a content identifier used to
     * identify the asset across devices.
     */
    public String getDigest() {
        return null;
    }

    /**
     * @return the file descriptor referencing the asset.
     */
    public ParcelFileDescriptor getFd() {
        return null;
    }

    /**
     * @return the uri referencing the asset data.
     */
    public Uri getUri() {
        return null;
    }

    public static final Creator<Asset> CREATOR = new AutoCreator<Asset>(Asset.class);
}
