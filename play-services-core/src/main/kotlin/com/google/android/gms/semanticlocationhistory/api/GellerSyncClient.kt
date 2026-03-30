/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.semanticlocationhistory.api

import android.content.Context
import android.util.Log
import com.google.android.gms.geller.BatchSyncRequest
import com.google.android.gms.geller.BatchSyncResponse
import com.google.android.gms.geller.GellerDataType
import com.google.android.gms.geller.GellerElement
import com.google.android.gms.geller.GellerServiceClient
import com.google.android.gms.geller.RequestOptions
import com.google.android.gms.geller.RequestReason
import com.google.android.gms.geller.SyncItem
import com.google.android.gms.geller.SyncMode
import com.google.android.gms.geller.SyncReason
import com.google.android.gms.semanticlocationhistory.requestGellerOauthToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.microg.gms.utils.createGrpcClient

private const val TAG = "GellerSyncClient"
private const val CLIENT_ID = "SEMANTICLOCATION"
private const val GELLER_BASE_URL = "https://geller-pa.googleapis.com/"

object GellerSyncClient {

    private fun gellerGrpcClient(oauthToken: String): GellerServiceClient {
        return createGrpcClient<GellerServiceClient>(GELLER_BASE_URL, oauthToken)
    }

    private fun buildSyncItem(
        syncToken: String? = null, mutations: List<GellerElement> = emptyList(), deletions: List<GellerElement> = emptyList()
    ) = SyncItem(
        dataType = GellerDataType.ENCRYPTED_ONDEVICE_LOCATION_HISTORY, syncToken = syncToken ?: "", mutations = mutations, deletions = deletions
    )

    private suspend fun syncOdlh(context: Context, accountName: String, items: List<SyncItem>, syncReason: SyncReason): BatchSyncResponse? = withContext(Dispatchers.IO) {
        Log.d(TAG, "syncOdlh: items=${items.size}, syncReason=$syncReason")
        return@withContext runCatching {
            val oauthToken = context.requestGellerOauthToken(accountName)
            gellerGrpcClient(oauthToken).BatchSync().executeBlocking(
                BatchSyncRequest(
                    items = items,
                    clientId = CLIENT_ID,
                    options = RequestOptions(
                        clientInfo = null, requestReason = RequestReason.ON_DEMAND, syncMode = SyncMode.SYNC_MODE_FULL
                    ),
                    syncReason = syncReason,
                )
            ).also {
                Log.d(TAG, "syncOdlh: response items=${it}")
            }
        }.onFailure {
            Log.e(TAG, "syncOdlh failed", it)
        }.getOrNull()
    }

    suspend fun uploadOdlh(context: Context, accountName: String, syncToken: String? = null, mutations: List<GellerElement>): BatchSyncResponse? {
        Log.d(TAG, "uploadOdlh: ${mutations.size} entries")
        return syncOdlh(
            context, accountName, items = listOf(buildSyncItem(syncToken, mutations = mutations)), syncReason = SyncReason.SYNC_REASON_PUSH
        )
    }

    suspend fun deleteOdlh(context: Context, accountName: String, syncToken: String? = null, deletions: List<GellerElement>): BatchSyncResponse? {
        Log.d(TAG, "deleteOdlh: ${deletions.size} entries")
        return syncOdlh(
            context, accountName, items = listOf(buildSyncItem(syncToken, deletions = deletions)), syncReason = SyncReason.SYNC_REASON_RESTORE
        )
    }

    suspend fun queryOdlh(context: Context, accountName: String, syncToken: String? = null): BatchSyncResponse? {
        return syncOdlh(
            context, accountName, items = listOf(buildSyncItem(syncToken)), syncReason = SyncReason.SYNC_REASON_PERIODIC
        )
    }
}