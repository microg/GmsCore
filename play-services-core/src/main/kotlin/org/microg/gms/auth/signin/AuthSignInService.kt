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
package org.microg.gms.auth.signin

import android.os.Bundle
import android.os.Parcel
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.internal.ISignInCallbacks
import com.google.android.gms.auth.api.signin.internal.ISignInService
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Scope
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import org.microg.gms.common.PackageUtils
import org.microg.gms.utils.warnOnTransactionIssues

private const val TAG = "AuthSignInService"

class AuthSignInService : BaseService(TAG, GmsService.AUTH_SIGN_IN) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        val packageName = PackageUtils.getAndCheckCallingPackage(this, request.packageName)
            ?: throw IllegalArgumentException("Missing package name")
        val binder = SignInServiceImpl(packageName, request.scopes.asList(), request.extras).asBinder()
        callback.onPostInitComplete(CommonStatusCodes.SUCCESS, binder, Bundle())
    }
}

class SignInServiceImpl(private val packageName: String, private val scopes: List<Scope>, private val extras: Bundle) : ISignInService.Stub() {
    override fun silentSignIn(callbacks: ISignInCallbacks?, options: GoogleSignInOptions?) {
        Log.d(TAG, "Not yet implemented: signIn: $options")
        callbacks?.onSignIn(null, Status.INTERNAL_ERROR)
    }

    override fun signOut(callbacks: ISignInCallbacks?, options: GoogleSignInOptions?) {
        Log.d(TAG, "Not yet implemented: signOut: $options")
        callbacks?.onSignOut(Status.INTERNAL_ERROR)
    }

    override fun revokeAccess(callbacks: ISignInCallbacks?, options: GoogleSignInOptions?) {
        Log.d(TAG, "Not yet implemented: revokeAccess: $options")
        callbacks?.onRevokeAccess(Status.INTERNAL_ERROR)
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean = warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }
}