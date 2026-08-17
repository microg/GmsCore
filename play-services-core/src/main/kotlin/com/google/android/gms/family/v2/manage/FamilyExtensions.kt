/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.family.v2.manage

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.LocaleList
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.unit.Dp
import androidx.core.net.toUri
import com.google.android.gms.R
import com.google.android.gms.family.model.MemberDataModel
import com.google.android.gms.family.v2.model.BulletPoint
import com.google.android.gms.family.v2.model.HelpData
import com.google.android.gms.family.v2.model.PageData
import com.squareup.wire.GrpcClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.microg.gms.auth.AuthConstants
import org.microg.gms.auth.AuthManager
import org.microg.gms.checkin.LastCheckinInfo
import org.microg.gms.common.Constants
import org.microg.gms.family.CallerInfo
import org.microg.gms.family.DeviceInfo
import org.microg.gms.family.FamilyBulletPoint
import org.microg.gms.family.FamilyHelpLink
import org.microg.gms.family.FamilyPageBody
import org.microg.gms.family.FamilyRole
import org.microg.gms.family.GetFamilyManagementConfigResponse
import org.microg.gms.family.GetFamilyResponse
import org.microg.gms.family.GrpcFamilyManagementServiceClient
import org.microg.gms.family.RequestContext
import org.microg.gms.profile.Build
import java.text.DateFormat
import java.util.Date
import java.util.Locale

const val TAG = "FamilyManagement"

const val ACTION_FAMILY_MANAGEMENT = "com.google.android.gms.family.v2.MANAGE"

const val EXTRA_KEY_APP_ID = "appId"
const val EXTRA_KEY_PREDEFINED_THEME = "predefinedTheme"
const val EXTRA_KEY_ACCOUNT_NAME = "accountName"
const val EXTRA_KEY_ERROR_CODE = "errorCode"
const val EXTRA_KEY_FAMILY_CHANGED = "familyChanged"
const val EXTRA_KEY_CONSISTENCY_TOKEN = "consistencyToken"
const val EXTRA_KEY_TOKEN_EXPIRATION_TIME_SECS = "tokenExpirationTimeSecs"
const val EXTRA_KEY_CALLING_PACKAGE_NAME = "callingPackageName"
const val EXTRA_KEY_MEMBER_MODEL = "memberDataModel"
const val EXTRA_KEY_MEMBER_ID = "memberId"
const val EXTRA_KEY_MEMBER_GIVEN_NAME= "memberGivenName"
const val EXTRA_KEY_MEMBER_LEAVE_FAMILY= "leaveFamily"
const val EXTRA_KEY_MEMBER_HOH_GIVEN_NAME = "hohGivenName"
const val EXTRA_KEY_CLIENT_CALLING_PACKAGE = "clientCallingPackage"

const val FAMILY_MANAGEMENT_MODULE_VERSION = "228"
const val FAMILY_MANAGEMENT_MODULE_DASHBOARD = "family_module_management_dashboard"
const val FAMILY_MANAGEMENT_BASE_URL = "https://familymanagement-pa.googleapis.com/"
const val FAMILY_LINK_MEMBER_BASE_URL = "https://familylink.google.com/member/"
const val FAMILY_MANAGEMENT_DEFAULT_USER_AGENT = "grpc-java-okhttp/1.66.0-SNAPSHOT"

const val FAMILY_PAGE_CONTENT_TEXT_INDEX = 3
const val FAMILY_PAGE_CONTENT_POSITIVE_BUTTON_INDEX = 4
const val FAMILY_PAGE_CONTENT_NEGATIVE_BUTTON_INDEX = 5
const val FAMILY_PAGE_CONTENT_TITLE_INDEX = 28

const val FAMILY_PAGE_CONTENT_FLAG_MEMBER_LIST = 1
const val FAMILY_FLAG_PAGE_CONTENT_DELETE_FAMILY = 9
const val FAMILY_FLAG_PAGE_CONTENT_REMOVE_MEMBER = 10
const val FAMILY_FLAG_PAGE_CONTENT_LEAVE_FAMILY = 11

const val FAMILY_OPTION_INVITE_TITLE_ID = 19
const val FAMILY_OPTION_INVITE_ID = 20

const val FAMILY_RE_AUTH_PROOF_TOKENS_URL = "https://www.googleapis.com/reauth/v1beta/users/me/reauthProofTokens?alt=proto"
const val FAMILY_INVITE_MEMBER_URL = "https://myaccount.google.com/embedded/family/invitemembers"

private const val FAMILY_SCOPE = "https://www.googleapis.com/auth/kid.family"
val SERVICE_FAMILY_SCOPE: String
    get() = "${AuthConstants.SCOPE_OAUTH2}${FAMILY_SCOPE}"
private const val FAMILY_RE_AUTH_SCOPE = "https://www.googleapis.com/auth/accounts.reauth"
val SERVICE_FAMILY_RE_AUTH_SCOPE: String
    get() = "${AuthConstants.SCOPE_OAUTH2}${FAMILY_RE_AUTH_SCOPE}"

fun Dp.toPx(context: Context): Int = (this.value * context.resources.displayMetrics.density).toInt()

fun familyGrpcClient(context: Context, oauthToken: String?): GrpcFamilyManagementServiceClient {
    val okHttpClient = OkHttpClient.Builder().addInterceptor { chain ->
        val original = chain.request()
        val requestBuilder = original.newBuilder().header("te", "trailers").header("x-device-id", LastCheckinInfo.read(context).androidId.toString(16)).header("authorization", "Bearer $oauthToken")
            .header("user-agent", FAMILY_MANAGEMENT_DEFAULT_USER_AGENT)
            .header("accept-language", if (Build.VERSION.SDK_INT >= 24) LocaleList.getDefault().toLanguageTags() else Locale.getDefault().language).removeHeader("grpc-trace-bin")
        val request = requestBuilder.build()
        chain.proceed(request)
    }.build()
    val grpcClient = GrpcClient.Builder().client(okHttpClient).baseUrl(FAMILY_MANAGEMENT_BASE_URL).minMessageToCompress(Long.MAX_VALUE).build()
    return GrpcFamilyManagementServiceClient(grpcClient)
}

fun buildRequestContext(appId: String): RequestContext {
    val deviceInfo = DeviceInfo.build {
        moduleVersion(FAMILY_MANAGEMENT_MODULE_VERSION)
        clientType(7)
        moduleInfo(CallerInfo.build { appId(appId) })
    }
    return RequestContext.build {
        deviceInfo(deviceInfo)
        familyExperimentOverrides("")
        moduleSet("")
    }
}

suspend fun requestOauthToken(context: Context, accountName: String, service: String): String {
    val authResponse = withContext(Dispatchers.IO) {
        AuthManager(
            context, accountName, Constants.GMS_PACKAGE_NAME, service
        ).apply { isPermitted = true }.requestAuth(true)
    }
    return authResponse.auth ?: throw RuntimeException("oauthToken is null")
}

fun GetFamilyResponse.parseToMemberDataModels(context: Context, accountName: String, configResponse: GetFamilyManagementConfigResponse?): MutableList<MemberDataModel> {
    val inviteSlotSize = configResponse?.let {
        val inviteOption = it.configMain?.familyOption?.find { option -> option.optionId == FAMILY_OPTION_INVITE_ID }
        val inviteSlotsContent = inviteOption?.optionContents?.find { c -> c.optId == FAMILY_OPTION_INVITE_TITLE_ID }?.content
        inviteSlotsContent?.let { content -> Regex("\\d+").find(content)?.value?.toIntOrNull() ?: 0 } ?: 0
    } ?: 0
    val memberDataModels = mutableListOf<MemberDataModel>()
    memberDataList.map {
        MemberDataModel().apply {
            memberId = it.memberId ?: ""
            profilePhotoUrl = it.profile?.profilePhotoUrl ?: it.profile?.defaultPhotoUrl ?: ""
            displayName = it.profile?.displayName ?: it.profile?.email ?: ""
            email = it.profile?.email ?: ""
            hohGivenName = it.hohGivenName ?: ""
            role = it.role?.value ?: FamilyRole.UNCONFIRMED_MEMBER.value
            roleName = it.role?.name ?: FamilyRole.UNCONFIRMED_MEMBER.name
        }
    }.forEach { memberDataModels.add(it) }
    invitationList.map {
        MemberDataModel().apply {
            memberId = it.invitationId ?: ""
            profilePhotoUrl = it.profile?.profilePhotoUrl ?: it.profile?.defaultPhotoUrl ?: ""
            displayName = it.profile?.displayName ?: it.profile?.email ?: ""
            email = it.profile?.email ?: ""
            hohGivenName = context.resources.getString(R.string.family_management_invite_send)
            role = it.role?.value ?: FamilyRole.UNCONFIRMED_MEMBER.value
            roleName = it.role?.name ?: FamilyRole.UNCONFIRMED_MEMBER.name
            isInvited = true
            invitationId = it.invitationId ?: ""
            inviteState = it.inviteState ?: 0
            inviteSentDate = when {
                inviteState <= 0 -> ""
                else -> runCatching {
                    val locale = if (Build.VERSION.SDK_INT >= 24) {
                        context.resources.configuration.locales[0]
                    } else {
                        context.resources.configuration.locale
                    }
                    context.getString(
                        R.string.family_management_invite_sent_date_format, DateFormat.getDateInstance(DateFormat.MEDIUM, locale).format(Date(inviteState))
                    )
                }.getOrDefault("")
            }
        }
    }.forEach { memberDataModels.add(it) }
    if (memberDataModels.any { it.email == accountName && it.role == FamilyRole.HEAD_OF_HOUSEHOLD.value } && inviteSlotSize > 0) {
        memberDataModels.add(
            MemberDataModel().apply {
                isInviteEntry = true
                inviteSlots = inviteSlotSize
            }
        )
    }
    return memberDataModels
}

fun FamilyPageBody.parseToPageData(): PageData {
    val sectionMap = HashMap<Int?, String?>()
    val helpMap = HashMap<String?, HelpData?>()
    val bps = ArrayList<BulletPoint?>()
    for (section in sections) {
        sectionMap.put(section.sectionId, section.content)
    }
    for (link in helpLinks) {
        helpMap.put(link.tag, link.parseToHelpData())
    }
    for (bp in bulletPoints) {
        bps.add(bp.parseToBulletPoint())
    }
    return PageData(sectionMap, helpMap, bps)
}

private fun FamilyBulletPoint.parseToBulletPoint(): BulletPoint {
    val contentMap = HashMap<Int, String>()
    if (items.isNotEmpty()) {
        items.filter {
            it.sectionId != null && it.content != null
        }.forEach {
            contentMap.put(it.sectionId!!, it.content!!)
        }
    }
    return BulletPoint(contentMap)
}

private fun FamilyHelpLink.parseToHelpData(): HelpData {
    return HelpData(url, appContext)
}

fun buildFamilyInviteUrl(callerAppId: String?): String {
    return FAMILY_INVITE_MEMBER_URL.toUri().buildUpon()
        .appendQueryParameter("app_id", callerAppId?.let { (it.transformToInt() - 2).toString() })
        .appendQueryParameter("referrer_flow", FAMILY_MANAGEMENT_MODULE_DASHBOARD)
        .build().toString()
}

private fun String.transformToInt() = when (this) {
    "ytu" -> 4
    "g1" -> 5
    "pfl" -> 6
    "pfpp" -> 7
    "agsa" -> 8
    "asm" -> 9
    "calendar" -> 10
    "ytm" -> 11
    "ytr" -> 12
    "famlink" -> 13
    "com.google.android.gms" -> 14
    "yt-main" -> 15
    "yt-fc" -> 17
    "yt-tandem" -> 18
    else -> 2
}

fun Activity.showToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun Activity.errorResult(msg: String, code: Int? = null, accountName: String? = null) {
    Log.d(TAG, "errorResult: $msg")
    if (code != null) {
        setResult(4, Intent().apply {
            putExtra(EXTRA_KEY_ACCOUNT_NAME, accountName)
            putExtra(EXTRA_KEY_ERROR_CODE, code)
        })
    } else setResult(4)
}

fun Activity.onResult(accountName: String?, consistencyToken: String? = null) {
    val result = Intent().apply {
        putExtra(EXTRA_KEY_ACCOUNT_NAME, accountName)
        putExtra(EXTRA_KEY_FAMILY_CHANGED, true)
    }
    consistencyToken?.let {
        result.putExtra(EXTRA_KEY_CONSISTENCY_TOKEN, it)
        result.putExtra(EXTRA_KEY_TOKEN_EXPIRATION_TIME_SECS, 300)
    }
    setResult(3, result)
}