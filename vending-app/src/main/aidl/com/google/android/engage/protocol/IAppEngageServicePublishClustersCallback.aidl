/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
 package com.google.android.engage.protocol;

 import android.os.Bundle;

 /**
  * Callback interface for publishClusters operation.
  * This callback is used to receive the result of a cluster publishing operation.
  */
 interface IAppEngageServicePublishClustersCallback {
     /**
      * Called when the publish operation has completed.
      *
      * @param result Bundle containing the result of the publish operation
      */
     void onResult(in Bundle result);
 }