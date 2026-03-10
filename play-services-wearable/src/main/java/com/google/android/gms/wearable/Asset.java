/*
 * Copyright (C) 2013-2017 microG Project Team
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

import com.google.android.gms.wearable.internal.PutDataRequest;

import org.microg.gms.common.PublicApi;
import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

import java.util.Arrays;

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

    @SafeParceled(1)
    private int versionCode = 1;
    @SafeParceled(2)
    @PublicApi(exclude = true)
    public byte[] data;
    @SafeParceled(3)
    private String digest;
    @SafeParceled(4)
    private ParcelFileDescriptor fd;
    @SafeParceled(5)
    private Uri uri;

    private Asset() {
    }

    private Asset(byte[] data, String digest, ParcelFileDescriptor fd, Uri uri) {
        this.data = data;
        this.digest = digest;
        this.fd = fd;
        this.uri = uri;
    }

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
        if (fd == null) {
            throw new IllegalArgumentException("Asset fd cannot be null");
        }
        return new Asset(null, null, fd, null);
    }

    /**
     * Create an Asset using an existing Asset's digest.
     */
    public static Asset createFromRef(String digest) {
        if (digest == null) {
            throw new IllegalArgumentException("Asset digest cannot be null");
        }
        return new Asset(null, digest, null, null);
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
        return digest;
    }

    /**
     * @return the file descriptor referencing the asset.
     */
    public ParcelFileDescriptor getFd() {
        return fd;
    }

    /**
     * @return the uri referencing the asset data.
     */
    public Uri getUri() {
        return uri;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Asset asset = (Asset) o;

        if (!Arrays.equals(data, asset.data)) return false;
        if (digest != null ? !digest.equals(asset.digest) : asset.digest != null) return false;
        if (fd != null ? !fd.equals(asset.fd) : asset.fd != null) return false;
        return !(uri != null ? !uri.equals(asset.uri) : asset.uri != null);

    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{data, digest, fd, uri});
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Asset[@")
                .append(Integer.toHexString(hashCode()))
                .append(", ")
                .append(digest != null ? digest : "nodigest");
        if (this.data != null) sb.append(", size=").append(data.length);
        if (this.fd != null) sb.append(", fd=").append(fd);
        if (this.uri != null) sb.append(", uri=").append(uri);
        return sb.append("]").toString();
    }

    public static final Creator<Asset> CREATOR = new AutoCreator<Asset>(Asset.class);
}
