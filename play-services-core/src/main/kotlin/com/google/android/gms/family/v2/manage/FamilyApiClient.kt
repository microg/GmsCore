/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.family.v2.manage

import android.content.Context
import android.util.Log
import com.google.android.gms.family.model.MemberDataModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.microg.gms.common.Constants
import org.microg.gms.family.DeleteOperationRequest
import org.microg.gms.family.DeleteOperationResponse
import org.microg.gms.family.FamilyRole
import org.microg.gms.family.GetFamilyManagementConfigRequest
import org.microg.gms.family.GetFamilyManagementConfigResponse
import org.microg.gms.family.GetFamilyManagementPageContentResponse
import org.microg.gms.family.GetFamilyRequest
import org.microg.gms.family.GetFamilyResponse
import org.microg.gms.family.MemberInfo
import org.microg.gms.family.PlaceHolder
import org.microg.gms.family.ReAuthProofTokensRequest
import org.microg.gms.profile.Build

object FamilyApiClient {

    private val FAMILY_RE_AUTH_PROOF_TOKENS_USER_AGENT = "${Constants.GMS_PACKAGE_NAME}/ ${Constants.GMS_VERSION_CODE} (Linux; Android ${Build.VERSION.RELEASE}; ${Build.MODEL} Build/${Build.ID};)"

    suspend fun loadFamilyData(
        context: Context,
        oauthToken: String?,
        appId: String,
        flag: Int
    ): GetFamilyResponse? = withContext(Dispatchers.IO) {
        val requestContext = buildRequestContext(appId)
        val request = GetFamilyRequest.build {
            context(requestContext)
            flag(flag)
        }
        Log.d(TAG, "getFamily request: $request")
        familyGrpcClient(context, oauthToken).GetFamily().executeBlocking(request)
    }

    suspend fun loadFamilyManagementPageContent(
        context: Context,
        oauthToken: String?,
        appId: String,
        memberId: String,
        currentMember: MemberDataModel,
        leaveFamily: Boolean = false
    ): GetFamilyManagementPageContentResponse? = withContext(Dispatchers.IO) {
        val requestContext = buildRequestContext(appId)
        val request = GetFamilyRequest.build {
            val type = if (leaveFamily) {
                FAMILY_FLAG_PAGE_CONTENT_REMOVE_MEMBER
            } else if (currentMember.role == FamilyRole.HEAD_OF_HOUSEHOLD.value) {
                FAMILY_FLAG_PAGE_CONTENT_DELETE_FAMILY
            } else {
                FAMILY_FLAG_PAGE_CONTENT_LEAVE_FAMILY
            }
            context(requestContext)
            flag(type)
            if (leaveFamily && memberId.isNotEmpty()) {
                memberInfo(MemberInfo.build { memberId(memberId) })
            } else {
                placeHolder(PlaceHolder())
            }
        }
        Log.d(TAG, "getFamilyManagementPageContent request: $request")
        familyGrpcClient(context, oauthToken).GetFamilyManagementPageContent().executeBlocking(request)
    }

    suspend fun loadFamilyManagementConfig(
        context: Context,
        oauthToken: String?,
        appId: String,
        directAdd: Boolean = false
    ): GetFamilyManagementConfigResponse? = withContext(Dispatchers.IO) {
        val requestContext = buildRequestContext(appId)
        val request = GetFamilyManagementConfigRequest.build {
            context(requestContext)
            directAdd(directAdd)
        }
        Log.d(TAG, "getFamilyManagementConfig request: $request")
        familyGrpcClient(context, oauthToken).GetFamilyManagementConfig().executeBlocking(request)
    }

    suspend fun deleteInvitationMember(
        context: Context,
        oauthToken: String?,
        appId: String,
        memberId: String
    ): DeleteOperationResponse? = withContext(Dispatchers.IO) {
        val requestContext = buildRequestContext(appId)
        val request = DeleteOperationRequest.build {
            context(requestContext)
            placeHolder(PlaceHolder())
            memberId(memberId)
        }
        Log.d(TAG, "deleteInvitation request: $request")
        familyGrpcClient(context, oauthToken).DeleteInvitation().executeBlocking(request)
    }

    suspend fun deleteMember(
        context: Context,
        oauthToken: String?,
        appId: String,
        memberId: String
    ): DeleteOperationResponse? = withContext(Dispatchers.IO) {
        val requestContext = buildRequestContext(appId)
        val request = DeleteOperationRequest.build {
            context(requestContext)
            memberId(memberId)
        }
        Log.d(TAG, "deleteMember request: $request")
        familyGrpcClient(context, oauthToken).DeleteMember().executeBlocking(request)
    }

    suspend fun deleteFamily(
        context: Context,
        oauthToken: String?,
        appId: String,
    ): DeleteOperationResponse? = withContext(Dispatchers.IO) {
        val requestContext = buildRequestContext(appId)
        val request = DeleteOperationRequest.build { context(requestContext) }
        Log.d(TAG, "deleteFamily request: $request")
        familyGrpcClient(context, oauthToken).DeleteFamily().executeBlocking(request)
    }

    suspend fun validatePassword(oauthToken: String?, password: String) = withContext(Dispatchers.IO) {
        runCatching {
            val client = OkHttpClient()
            val mediaType = "application/x-protobuf".toMediaTypeOrNull()
            val reAuthRequest = ReAuthProofTokensRequest.build {
                type(2)
                password(password)
            }.encode()
            val requestBody = reAuthRequest.toRequestBody(mediaType)

            val request = Request.Builder()
                .url(FAMILY_RE_AUTH_PROOF_TOKENS_URL)
                .post(requestBody)
                .addHeader("Authorization", "Bearer $oauthToken")
                .addHeader("Content-Type", "application/x-protobuf")
                .addHeader("User-Agent", FAMILY_RE_AUTH_PROOF_TOKENS_USER_AGENT)
                .build()
            val response = client.newCall(request).execute()
            if (response.code != 200) {
                throw RuntimeException("Invalid response code: ${response.code} body: ${response.body?.string()}")
            }
            true
        }.onFailure {
            Log.d(TAG, "requestReAuthProofTokens: failed ", it)
        }.getOrDefault(false)
    }
}