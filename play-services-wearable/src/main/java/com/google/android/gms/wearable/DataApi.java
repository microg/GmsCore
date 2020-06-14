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

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.data.Freezable;
import com.google.android.gms.wearable.internal.PutDataRequest;

import org.microg.gms.common.PublicApi;

import java.io.InputStream;

/**
 * Exposes an API for components to read or write data items and assets.
 * <p/>
 * A {@link DataItem} is synchronized across all devices in an Android Wear network. It is possible
 * to set data items while not connected to any nodes. Those data items will be synchronized when
 * the nodes eventually come online.
 * <p/>
 * Data items are private to the application that created them, and are only accessible by that
 * application on other nodes. They should generally be small in size, relying on assets for the
 * transfer of larger, more persistent data objects such as images.
 * <p/>
 * Each data item is identified by a URI, accessible with {@link DataItem#getUri()}, that indicates
 * the item's creator and path. Fully specified URIs follow the following format:
 * {@code wear://<node_id>/<path>}, where <node_id> is the node ID of the wearable node that
 * created the data item, and <path> is an application-defined path. This means that given a data
 * item's URI, calling {@link Uri#getHost()} will return the creator's node ID.
 * <p/>
 * In some of the methods below (such as {@link #getDataItems(GoogleApiClient, Uri)}), it is
 * possible to omit the node ID from the URI, and only leave a path. In that case, the URI may
 * refer to multiple data items, since multiple nodes may create data items with the same path.
 * Partially specified data item URIs follow the following format:
 * {@ocde wear:/<path>}
 * Note the single / after wear:.
 */
@PublicApi
public interface DataApi {
    /**
     * Registers a listener to receive data item changed and deleted events. This call should be
     * balanced with a call to {@link #removeListener(GoogleApiClient, DataListener)}, to avoid
     * leaking resources.
     * <p/>
     * The listener will be notified of changes initiated by this node.
     */
    PendingResult<Status> addListener(GoogleApiClient client, DataListener listener);

    /**
     * Removes all specified data items from the Android Wear network.
     * <p/>
     * If uri is fully specified, this method will delete at most one data item. If {@code uri}
     * contains no host, multiple data items may be deleted, since different nodes may create data
     * items with the same path. See {@link DataApi} for details of the URI format.
     */
    PendingResult<DeleteDataItemsResult> deleteDataItems(GoogleApiClient client, Uri uri);

    /**
     * Retrieves a single {@link DataItem} from the Android Wear network. A fully qualified URI
     * must be specified. The URI's host must be the ID of the node that created the item.
     * <p/>
     * See {@link DataApi} for details of the URI format.
     */
    PendingResult<DataItemResult> getDataItem(GoogleApiClient client, Uri uri);

    /**
     * Retrieves all data items from the Android Wear network.
     * <p/>
     * Callers must call {@link DataItemBuffer#release()} on the returned buffer when finished
     * processing results.
     */
    PendingResult<DataItemBuffer> getDataItems(GoogleApiClient client);

    /**
     * Retrieves all data items matching the provided URI, from the Android Wear network.
     * <p/>
     * The URI must contain a path. If {@code uri} is fully specified, at most one data item will
     * be returned. If uri contains no host, multiple data items may be returned, since different
     * nodes may create data items with the same path. See {@link DataApi} for details of the URI
     * format.
     * <p/>
     * Callers must call {@link DataItemBuffer#release()} on the returned buffer when finished
     * processing results.
     */
    PendingResult<DataItemBuffer> getDataItems(GoogleApiClient client, Uri uri);

    /**
     * Retrieves a {@link ParcelFileDescriptor} pointing at the bytes of an asset. Only assets
     * previously stored in a {@link DataItem} may be retrieved.
     */
    PendingResult<GetFdForAssetResult> getFdForAsset(GoogleApiClient client, DataItemAsset asset);

    /**
     * Retrieves a {@link ParcelFileDescriptor} pointing at the bytes of an asset. Only assets
     * previously stored in a {@link DataItem} may be retrieved.
     */
    PendingResult<GetFdForAssetResult> getFdForAsset(GoogleApiClient client, Asset asset);

    /**
     * Adds a {@link DataItem} to the Android Wear network. The updated item is synchronized across
     * all devices.
     */
    PendingResult<DataItemResult> putDataItem(GoogleApiClient client, PutDataRequest request);

    /**
     * Removes a data listener which was previously added through
     * {@link #addListener(GoogleApiClient, DataListener)}.
     */
    PendingResult<Status> removeListener(GoogleApiClient client, DataListener listener);

    interface DataItemResult extends Result {
        /**
         * @return data item, or {@code null} if the item does not exit.
         */
        DataItem getDataItem();
    }

    interface DataListener {
        /**
         * Notification that a set of data items have been changed or deleted. The data buffer is
         * released upon completion of this method. If a caller wishes to use the events outside
         * this callback, they should be sure to {@link Freezable#freeze()} the DataEvent objects
         * they wish to use.
         */
        void onDataChanged(DataEventBuffer dataEvents);
    }

    interface DeleteDataItemsResult extends Result {
        /**
         * @return the number of items deleted by
         * {@link DataApi#deleteDataItems(GoogleApiClient, Uri)}.
         */
        int getNumDeleted();
    }

    interface GetFdForAssetResult extends Result {
        /**
         * @return a file descriptor for the requested asset.
         */
        ParcelFileDescriptor getFd();

        /**
         * @return an input stream wrapping the file descriptor. When this input stream is closed, the file descriptor is, as well.
         */
        InputStream getInputStream();
    }
}
