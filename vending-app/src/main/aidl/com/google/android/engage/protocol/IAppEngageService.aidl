/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
 package com.google.android.engage.protocol;

 import android.os.Bundle;
 import com.google.android.engage.protocol.IAppEngageServicePublishClustersCallback;
 import com.google.android.engage.protocol.IAppEngageServiceDeleteClustersCallback;
 import com.google.android.engage.protocol.IAppEngageServiceAvailableCallback;
 import com.google.android.engage.protocol.IAppEngageServicePublishStatusCallback;

 interface IAppEngageService {
     /**
      * Publishes clusters of app engagement data.
      *
      * @param bundle Contains cluster data to be published
      * @param callback Callback to receive results of the publish operation
      */
     void publishClusters(in Bundle bundle, IAppEngageServicePublishClustersCallback callback);

     /**
      * Deletes previously published clusters of app engagement data.
      *
      * @param bundle Contains specifications about which clusters to delete
      * @param callback Callback to receive results of the delete operation
      */
     void deleteClusters(in Bundle bundle, IAppEngageServiceDeleteClustersCallback callback);

     /**
      * Checks if the App Engage Service is available for the calling application.
      *
      * @param bundle Contains parameters for the availability check
      * @param callback Callback to receive availability status
      */
     void isServiceAvailable(in Bundle bundle, IAppEngageServiceAvailableCallback callback);

     /**
      * Updates the publishing status for previously published clusters.
      *
      * @param bundle Contains status update information
      * @param callback Callback to receive results of the status update operation
      */
     void updatePublishStatus(in Bundle bundle, IAppEngageServicePublishStatusCallback callback);
 }