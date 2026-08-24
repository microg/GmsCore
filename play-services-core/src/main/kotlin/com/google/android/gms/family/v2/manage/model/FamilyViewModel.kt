/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.family.v2.manage.model

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.R
import com.google.android.gms.family.model.MemberDataModel
import com.google.android.gms.family.v2.manage.FAMILY_PAGE_CONTENT_FLAG_MEMBER_LIST
import com.google.android.gms.family.v2.manage.FAMILY_PAGE_CONTENT_TITLE_INDEX
import com.google.android.gms.family.v2.manage.FamilyApiClient
import com.google.android.gms.family.v2.manage.SERVICE_FAMILY_RE_AUTH_SCOPE
import com.google.android.gms.family.v2.manage.SERVICE_FAMILY_SCOPE
import com.google.android.gms.family.v2.manage.TAG
import com.google.android.gms.family.v2.manage.parseToMemberDataModels
import com.google.android.gms.family.v2.manage.parseToPageData
import com.google.android.gms.family.v2.manage.requestOauthToken
import com.google.android.gms.family.v2.model.PageData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext

data class UiState(
    val title: String = "",
    val isError: Boolean = false,
    val isLoading: Boolean = false,
    val showMoreAction: Boolean = false,
    val currentMember: MemberDataModel = MemberDataModel(),
    val memberList: List<MemberDataModel> = emptyList(),
    val pageData: PageData? = null,
)

sealed class PasswordCheckState {
    object Idle : PasswordCheckState()
    object Checking : PasswordCheckState()
    data class Success(val member: MemberDataModel) : PasswordCheckState()
    data class Error(val message: String) : PasswordCheckState()
}

sealed class FamilyChangedState {
    object Idle : FamilyChangedState()
    data class Changed(val token: String, val deleteFamily: Boolean) : FamilyChangedState()
    data class Error(val message: String, val code: Int) : FamilyChangedState()
}

class FamilyViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    private val _passwordCheckState = MutableStateFlow<PasswordCheckState>(PasswordCheckState.Idle)
    val passwordCheckState: StateFlow<PasswordCheckState> = _passwordCheckState.asStateFlow()
    private val _familyChangedStateState = MutableStateFlow<FamilyChangedState>(FamilyChangedState.Idle)
    val familyChangedStateState: StateFlow<FamilyChangedState> = _familyChangedStateState.asStateFlow()
    private val _refreshData = MutableStateFlow(true)
    val refreshing: StateFlow<Boolean> = _refreshData.asStateFlow()
    private val _selectedMember = MutableSharedFlow<Pair<Int?, MemberDataModel>>()
    val selectedMember = _selectedMember.asSharedFlow()

    suspend fun selectMember(viewId: Int?, member: MemberDataModel) {
        _selectedMember.emit(Pair(viewId, member))
    }

    fun updateUIState(showAction: Boolean, title: String = "") {
        _uiState.update { it.copy(showMoreAction = showAction, title = title) }
    }

    fun refreshData() {
        _refreshData.value = !_refreshData.value
    }

    fun resetPasswordState() {
        _passwordCheckState.value = PasswordCheckState.Idle
    }

    fun loadFamilyMembers(context: Context, accountName: String, appId: String, flag: Int = FAMILY_PAGE_CONTENT_FLAG_MEMBER_LIST) {
        viewModelScope.launch {
            supervisorScope {
                runCatching {
                    _uiState.update { it.copy(isLoading = true, isError = false) }
                    val oauthToken = requestOauthToken(context, accountName, SERVICE_FAMILY_SCOPE)
                    val familyResponseDeferred = async {
                        FamilyApiClient.loadFamilyData(context, oauthToken, appId, flag)
                    }
                    val configResponseDeferred = async {
                        FamilyApiClient.loadFamilyManagementConfig(context, oauthToken, appId, false)
                    }
                    val familyResponse = familyResponseDeferred.await()
                    val configResponse = configResponseDeferred.await()
                    Log.d(TAG, "loadFamilyMembers: familyResponse: $familyResponse")
                    Log.d(TAG, "loadFamilyMembers: configResponse: $configResponse")
                    familyResponse?.parseToMemberDataModels(context, accountName, configResponse)
                        ?: throw RuntimeException("familyResponse is null")
                }.onFailure { throwable ->
                    _familyChangedStateState.value = FamilyChangedState.Error(throwable.message ?: "", 4)
                    _uiState.update { it.copy(isLoading = false, isError = true) }
                    Log.d(TAG, "loadFamilyMembers error", throwable)
                }.onSuccess { list ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            memberList = list,
                            currentMember = list.firstOrNull { m -> m.email == accountName } ?: MemberDataModel()
                        )
                    }
                }
            }
        }
    }

    fun loadFamilyManagementPageContent(
        context: Context,
        accountName: String,
        appId: String,
        memberId: String,
        leaveFamily: Boolean = false
    ) {
        viewModelScope.launch {
            runCatching {
                _uiState.update { it.copy(isLoading = true, isError = false) }
                val oauthToken = requestOauthToken(context, accountName, SERVICE_FAMILY_SCOPE)
                val currentMember = getCurrentMember(context, accountName, appId, oauthToken)
                val pageContent = FamilyApiClient.loadFamilyManagementPageContent(context, oauthToken, appId, memberId, currentMember, leaveFamily)
                pageContent?.body?.parseToPageData() ?: throw RuntimeException("pageContent is null")
            }.onFailure { throwable ->
                _uiState.update { it.copy(isLoading = false, isError = true) }
                _familyChangedStateState.value = FamilyChangedState.Error(throwable.message ?: "", 4)
                Log.d(TAG, "loadFamilyManagementPageContent error", throwable)
            }.onSuccess { pageData ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        title = pageData.sectionMap.getValue(FAMILY_PAGE_CONTENT_TITLE_INDEX),
                        pageData = pageData
                    )
                }
            }
        }
    }

    fun validatePassword(
        context: Context,
        accountName: String,
        password: String,
        currentMember: MemberDataModel
    ) {
        viewModelScope.launch {
            _passwordCheckState.value = PasswordCheckState.Checking
            runCatching {
                val oauthToken = requestOauthToken(context, accountName, SERVICE_FAMILY_RE_AUTH_SCOPE)
                FamilyApiClient.validatePassword(oauthToken, password)
            }.onFailure {
                _passwordCheckState.value =
                    PasswordCheckState.Error(context.getString(R.string.family_management_pwd_error))
                _familyChangedStateState.value = FamilyChangedState.Error(it.message ?: "", 4)
                Log.d(TAG, "validatePassword error", it)
            }.onSuccess { success ->
                if (success) {
                    _passwordCheckState.value = PasswordCheckState.Success(currentMember)
                } else {
                    _passwordCheckState.value =
                        PasswordCheckState.Error(context.getString(R.string.family_management_pwd_error))
                }
            }
        }
    }

    suspend fun deleteInvitationMember(
        context: Context,
        accountName: String,
        appId: String,
        memberId: String
    ) = withContext(Dispatchers.IO) {
        runCatching {
            val oauthToken = requestOauthToken(context, accountName, SERVICE_FAMILY_SCOPE)
            val operationResponse =
                FamilyApiClient.deleteInvitationMember(context, oauthToken, appId, memberId)
            val consistencyToken = operationResponse?.result?.consistencyToken
            consistencyToken?.takeIf { it.isNotEmpty() }?.let {
                _familyChangedStateState.value = FamilyChangedState.Changed(it, false)
            }
            Log.d(TAG, "deleteInvitationMember: operationResponse: $operationResponse")
            !consistencyToken.isNullOrEmpty()
        }.onFailure {
            _familyChangedStateState.value = FamilyChangedState.Error(it.message ?: "", 4)
            Log.d(TAG, "deleteInvitationMember error", it)
        }.getOrDefault(false)
    }


    suspend fun deleteMember(
        context: Context,
        accountName: String,
        appId: String,
        memberId: String
    ) = withContext(Dispatchers.IO) {
        runCatching {
            val oauthToken = requestOauthToken(context, accountName, SERVICE_FAMILY_SCOPE)
            val operationResponse = FamilyApiClient.deleteMember(context, oauthToken, appId, memberId)
            val consistencyToken = operationResponse?.result?.consistencyToken
            consistencyToken?.takeIf { it.isNotEmpty() }?.let {
                _familyChangedStateState.value = FamilyChangedState.Changed(it, false)
            }
            Log.d(TAG, "deleteMember: operationResponse: $operationResponse")
            !consistencyToken.isNullOrEmpty()
        }.onFailure {
            _familyChangedStateState.value = FamilyChangedState.Error(it.message ?: "", 4)
            Log.d(TAG, "deleteMember error", it)
        }.getOrDefault(false)
    }

    suspend fun deleteFamily(
        context: Context,
        accountName: String,
        appId: String
    ) = withContext(Dispatchers.IO) {
        runCatching {
            val oauthToken = requestOauthToken(context, accountName, SERVICE_FAMILY_SCOPE)
            val operationResponse = FamilyApiClient.deleteFamily(context, oauthToken, appId)
            val consistencyToken = operationResponse?.result?.consistencyToken
            consistencyToken?.takeIf { it.isNotEmpty() }?.let {
                _familyChangedStateState.value = FamilyChangedState.Changed(it, true)
            }
            Log.d(TAG, "deleteFamily: operationResponse: $operationResponse")
            !consistencyToken.isNullOrEmpty()
        }.onFailure {
            _familyChangedStateState.value = FamilyChangedState.Error(it.message ?: "", 4)
            Log.d(TAG, "deleteFamily error", it)
        }.getOrDefault(false)
    }

    suspend fun getCurrentMember(context: Context, accountName: String, appId: String, oauthToken: String? = null): MemberDataModel {
        val currentMember = uiState.value.currentMember
        if (!currentMember.memberId.isNullOrEmpty()) {
            return currentMember
        }
        val memberDataModels = withContext(Dispatchers.IO) {
            val oauthToken = oauthToken ?: requestOauthToken(context, accountName, SERVICE_FAMILY_SCOPE)
            val familyResponse = FamilyApiClient.loadFamilyData(context, oauthToken, appId, FAMILY_PAGE_CONTENT_FLAG_MEMBER_LIST)
            familyResponse?.parseToMemberDataModels(context, accountName, null)
        }
        return memberDataModels?.firstOrNull { m -> m.email == accountName }?.also {
            _uiState.update { state -> state.copy(currentMember = it) }
        } ?: MemberDataModel()
    }

}