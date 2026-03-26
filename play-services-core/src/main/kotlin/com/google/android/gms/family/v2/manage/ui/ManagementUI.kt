/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.family.v2.manage.ui

import android.view.ViewGroup
import android.widget.ImageView
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.R
import com.google.android.gms.family.model.MemberDataModel
import com.google.android.gms.family.v2.manage.model.FamilyViewModel
import com.google.android.gms.family.v2.manage.toPx
import de.hdodenhof.circleimageview.CircleImageView
import org.microg.gms.family.FamilyRole

@Composable
fun FamilyManagementFragmentScreen(
    viewModel: FamilyViewModel,
    onMemberClick: (MemberDataModel) -> Unit,
    loadImage: (String?, ImageView) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(uiState.memberList, key = { it.memberId }) { member ->
            if (member.isInviteEntry) {
                InviteItem(
                    member = member,
                    onMemberClick = onMemberClick
                )
            } else {
                MemberItem(
                    member = member,
                    currentMember = uiState.currentMember,
                    onMemberClick = onMemberClick,
                    imageLoader = loadImage
                )
            }
        }
    }
}

@Composable
fun MemberDetailItem(
    viewModel: FamilyViewModel,
    member: MemberDataModel,
    onMemberClick: (MemberDataModel) -> Unit,
    loadImage: (String?, ImageView) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    val isHeadOfHousehold = uiState.currentMember.role == FamilyRole.HEAD_OF_HOUSEHOLD.value
    val actionTextId = if (member.isInvited) R.string.family_management_cancel_invite else R.string.family_management_remove_member

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        MemberItem(
            member = member,
            currentMember = uiState.currentMember,
            imageLoader = loadImage,
            isDetail = true
        )

        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )

        Text(
            text = stringResource(id = actionTextId),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(16.dp)
                .clickable(isHeadOfHousehold) { onMemberClick.invoke(member) }
        )
    }
}

@Composable
fun MemberItem(
    member: MemberDataModel,
    currentMember: MemberDataModel,
    onMemberClick: ((MemberDataModel) -> Unit)? = null,
    imageLoader: (String?, ImageView) -> Unit,
    isDetail: Boolean = false
) {
    val isSame = member.memberId == currentMember.memberId
    val isHeadOfHousehold = currentMember.role == FamilyRole.HEAD_OF_HOUSEHOLD.value
    val isInvited = member.isInvited
    var roleColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
    val roleName = if (isInvited && !member.inviteSentDate.isNullOrEmpty()) {
        if (isDetail) member.inviteSentDate else member.hohGivenName.also { roleColor = Color.Green }
    } else member.hohGivenName

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(66.dp)
            .padding(horizontal = 16.dp)
            .clickable(enabled = isHeadOfHousehold && !isSame && onMemberClick != null) {
                onMemberClick?.invoke(member)
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        AndroidView(
            factory = { context ->
                CircleImageView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(36.dp.toPx(context), 36.dp.toPx(context))
                    scaleType = ImageView.ScaleType.CENTER_CROP
                }
            },
            modifier = Modifier.size(36.dp),
            update = { imageView ->
                if (member.profilePhotoUrl.isNullOrEmpty()) {
                    imageView.setImageResource(R.drawable.ic_generic_man)
                } else {
                    imageLoader.invoke(member.profilePhotoUrl, imageView)
                }
            }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {
            Text(
                text = if (isDetail) member.email else member.displayName,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 14.sp,
            )
            if (!roleName.isNullOrEmpty()) {
                Text(
                    text = roleName,
                    color = roleColor,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun InviteItem(
    member: MemberDataModel,
    onMemberClick: (MemberDataModel) -> Unit,
    modifier: Modifier = Modifier,
    iconRes: Int = android.R.drawable.ic_menu_add,
    title: String = stringResource(id = R.string.family_management_invite_family_member),
    subTitle: String = stringResource(id = R.string.family_management_invite_slots_left, member.inviteSlots)
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(66.dp)
            .padding(horizontal = 16.dp)
            .clickable { onMemberClick(member) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = title,
            modifier = Modifier.size(36.dp)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = subTitle,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
    }
}