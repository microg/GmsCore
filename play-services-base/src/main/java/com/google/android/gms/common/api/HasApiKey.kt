/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.common.api

import com.google.android.gms.common.api.Api.ApiOptions
import com.google.android.gms.common.api.internal.ApiKey

interface HasApiKey<O : ApiOptions?> {
    fun getApiKey(): ApiKey<O>?
}