/*
 * SPDX-FileCopyrightText: 2025 e foundation
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.vending.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.vending.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkVendingTopAppBar() = TopAppBar(
    title = {
        Row {
            Icon(
                painterResource(R.drawable.ic_work),
                contentDescription = null,
                Modifier.align(Alignment.CenterVertically),
                tint = LocalContentColor.current
            )
            Text(
                stringResource(R.string.vending_activity_name),
                Modifier
                    .align(Alignment.CenterVertically)
                    .padding(start = 8.dp)
            )
        }
    },
    colors = TopAppBarDefaults.smallTopAppBarColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
    )
)

@Preview
@Composable
fun PreviewWorkVendingTopAppBar() {
    WorkVendingTopAppBar()
}