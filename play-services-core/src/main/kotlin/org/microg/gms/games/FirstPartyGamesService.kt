/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.games

import android.content.Context
import android.os.Parcel
import android.util.Log
import androidx.lifecycle.Lifecycle
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.data.DataHolder
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.games.client.IPlayGamesCallbacks
import com.google.android.gms.games.client.IPlayGamesService
import com.google.android.gms.games.client.PlayGamesConsistencyTokens
import org.microg.gms.BaseService
import org.microg.gms.common.Constants
import org.microg.gms.common.GmsService
import org.microg.gms.common.PackageUtils
import org.microg.gms.utils.warnOnTransactionIssues

private const val TAG = "PlayGamesService"
private val FIRST_PARTY_PACKAGES = setOf(Constants.GMS_PACKAGE_NAME, GAMES_PACKAGE_NAME)

class FirstPartyGamesService : BaseService(TAG, GmsService.GAMES) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        val packageName = PackageUtils.getAndCheckCallingPackageOrExtendedAccess(this, request.packageName)
            ?: throw IllegalArgumentException("Missing package name")
        val callingPackageName = PackageUtils.getCallingPackage(this) ?: packageName
        if (!PackageUtils.callerHasExtendedAccess(this)) throw IllegalArgumentException("$callingPackageName does not have extended access")
        if (callingPackageName !in FIRST_PARTY_PACKAGES) throw IllegalArgumentException("$callingPackageName is not first-party")
        callback.onPostInitCompleteWithConnectionInfo(
            CommonStatusCodes.SUCCESS,
            PlayGamesServiceImpl(this, lifecycle, packageName),
            ConnectionInfo()
        )
    }
}

class PlayGamesServiceImpl(val context: Context, val lifecycle: Lifecycle, val packageName: String) : IPlayGamesService.Stub() {

    override fun getGameCollection(callbacks: IPlayGamesCallbacks?, maxResults: Int, gameCollectionType: Int, z: Boolean, forceReload: Boolean) {
        Log.d(TAG, "Not yet implemented: getGameCollection($maxResults, $gameCollectionType, $z, $forceReload)")
        callbacks?.onData(DataHolder.empty(CommonStatusCodes.SUCCESS))
    }

    override fun loadGames(callbacks: IPlayGamesCallbacks?, playerId: String?, maxResults: Int, z: Boolean, forceReload: Boolean) {
        Log.d(TAG, "Not yet implemented: loadGames($playerId, $maxResults, $z, $forceReload)")
        callbacks?.onData(DataHolder.empty(CommonStatusCodes.SUCCESS))
    }

    override fun getConsistencyTokens(): PlayGamesConsistencyTokens {
        Log.d(TAG, "Not yet implemented: getConsistencyTokens")
        return PlayGamesConsistencyTokens(null, null)
    }

    override fun updateConsistencyTokens(tokens: PlayGamesConsistencyTokens?) {
        Log.d(TAG, "Not yet implemented: updateConsistencyTokens($tokens)")
    }

    override fun fun5041(callbacks: IPlayGamesCallbacks?) {
        callbacks?.onStatus5028(Status.SUCCESS)
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
        warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }
}