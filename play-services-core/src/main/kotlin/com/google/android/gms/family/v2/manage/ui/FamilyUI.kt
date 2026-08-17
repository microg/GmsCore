/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.family.v2.manage.ui

import android.app.Activity
import android.view.View
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import com.google.android.gms.R
import com.google.android.gms.family.model.MemberDataModel
import com.google.android.gms.family.v2.manage.model.FamilyViewModel
import com.google.android.gms.family.v2.manage.model.UiState
import kotlinx.coroutines.launch
import org.microg.gms.family.FamilyRole
import org.microg.gms.profile.Build

@Composable
fun FamilyActivityScreen(
    viewModel: FamilyViewModel,
    type: String?,
    addFragment: (View) -> Unit,
    onBackClick: () -> Unit,
    onMoreClick: ((MemberDataModel, Boolean) -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    FamilyTheme(familyThemeType = FamilyThemeType.from(type)) {
        Scaffold(
            snackbarHost = {
                SnackbarHost(hostState = snackHostState) { data ->
                    Snackbar(
                        shape = RectangleShape,
                        action = {
                            TextButton(onClick = {
                                scope.launch {
                                    snackHostState.currentSnackbarData?.dismiss()
                                    viewModel.refreshData()
                                }
                            }) {
                                Text(stringResource(R.string.family_management_retry))
                            }
                        }
                    ) {
                        Text(data.visuals.message)
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                FamilyToolbar(
                    uiState = uiState,
                    currentMember = uiState.currentMember,
                    onBackClick = onBackClick,
                    onMoreClick = onMoreClick
                )
                Box(modifier = Modifier.fillMaxSize()) {
                    val containerId = remember { View.generateViewId() }
                    AndroidView(
                        factory = { context -> FrameLayout(context).apply { id = containerId } },
                        update = { addFragment(it) }
                    )
                    if (uiState.isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(uiState.isError) {
        if (uiState.isError) {
            snackHostState.showSnackbar(context.getString(R.string.family_management_load_error))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FamilyToolbar(
    uiState: UiState,
    currentMember: MemberDataModel?,
    onBackClick: () -> Unit,
    onMoreClick: ((MemberDataModel, Boolean) -> Unit)? = null
) {
    val title = uiState.title
    val showMore = uiState.showMoreAction
    TopAppBar(
        title = { Text(title, color = MaterialTheme.colorScheme.onPrimary, fontSize = 18.sp) },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_back),
                    contentDescription = stringResource(id = R.string.family_management_toolbar_back),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        actions = {
            currentMember?.let { member ->
                onMoreClick?.let { moreClick ->
                    val action = when (member.role) {
                        FamilyRole.HEAD_OF_HOUSEHOLD.value -> stringResource(R.string.family_management_delete_family_group)
                        FamilyRole.MEMBER.value -> stringResource(R.string.family_management_exit_family_group)
                        else -> null
                    }
                    action?.let { text ->
                        if (showMore) {
                            MoreOptionsMenu(
                                menuItems = listOf(text),
                                member = member,
                                onMenuClick = moreClick
                            )
                        }
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
private fun MoreOptionsMenu(
    menuItems: List<String>,
    member: MemberDataModel,
    onMenuClick: (MemberDataModel, Boolean) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                painter = painterResource(id = R.drawable.ic_more_vert),
                contentDescription = stringResource(id = R.string.family_management_toolbar_more),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            menuItems.forEach { title ->
                DropdownMenuItem(
                    text = { Text(title) },
                    onClick = {
                        onMenuClick(member, member.role != FamilyRole.HEAD_OF_HOUSEHOLD.value)
                        expanded = false
                    }
                )
            }
        }
    }
}

private data class ThemeColors(
    val primary: Color,
    val onPrimary: Color,
    val background: Color,
    val onBackground: Color,
)

private enum class FamilyThemeType(
    val typeName: String, val lightColorScheme: ThemeColors, val darkColorScheme: ThemeColors? = null
) {
    FAMILY_MANAGEMENT(
        "family_management", lightColorScheme = ThemeColors(
            primary = Color(0xFF1A73E8),
            onPrimary = Color.White,
            background = Color.White,
            onBackground = Color(0xFF5F6368),
        ), darkColorScheme = ThemeColors(
            primary = Color(0xFF89B4F8),
            onPrimary = Color.Black,
            background = Color(0xFF202124),
            onBackground = Color(0xFFEEEEEE),
        )
    ),

    PLAY_PASS(
        "play_pass", lightColorScheme = ThemeColors(
            primary = Color(0xFF01875F),
            onPrimary = Color.White,
            background = Color.White,
            onBackground = Color.Black,
        ), darkColorScheme = ThemeColors(
            primary = Color(0xFF00A173),
            onPrimary = Color.Black,
            background = Color(0xFF202124),
            onBackground = Color(0xFFEEEEEE),
        )
    ),

    PLAY_MUSIC(
        "play_music", lightColorScheme = ThemeColors(
            primary = Color(0xFFEF6C00),
            onPrimary = Color.White,
            background = Color.White,
            onBackground = Color.Black,
        )
    ),

    YOUTUBE(
        "youtube", lightColorScheme = ThemeColors(
            primary = Color(0xFFE62117),
            onPrimary = Color.White,
            background = Color.White,
            onBackground = Color.Black,
        )
    ),

    ASSISTANT(
        "assistant", lightColorScheme = ThemeColors(
            primary = Color(0xFFFFFFFF),
            onPrimary = Color.Black,
            background = Color.White,
            onBackground = Color.Black,
        )
    ),

    G1(
        "g1", lightColorScheme = ThemeColors(
            primary = Color(0xFFFFFFFF),
            onPrimary = Color.Black,
            background = Color.White,
            onBackground = Color.Black,
        )
    ),

    PLAY(
        "play", lightColorScheme = ThemeColors(
            primary = Color(0xFF455A64), onPrimary = Color.White, background = Color.White, onBackground = Color.Black
        )
    );

    companion object {
        fun from(type: String?) = FamilyThemeType.entries.firstOrNull {
            it.typeName.equals(type, ignoreCase = true)
        } ?: FAMILY_MANAGEMENT
    }
}

@Composable
private fun FamilyTheme(
    familyThemeType: FamilyThemeType, darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit
) {
    val colors = if (darkTheme && familyThemeType.darkColorScheme != null) {
        familyThemeType.darkColorScheme
    } else {
        familyThemeType.lightColorScheme
    }

    val colorScheme = ColorScheme(
        primary = colors.primary,
        onPrimary = colors.onPrimary,
        background = colors.background,
        onBackground = colors.onBackground,
        surface = colors.background,
        onSurface = colors.onBackground,
        primaryContainer = colors.primary.copy(alpha = 0.2f),
        onPrimaryContainer = colors.onPrimary,
        secondary = colors.primary,
        onSecondary = colors.onPrimary,
        secondaryContainer = colors.primary.copy(alpha = 0.2f),
        onSecondaryContainer = colors.onPrimary,
        error = Color(0xFFB00020),
        onError = Color.White,
        errorContainer = Color(0xFFCF6679),
        onErrorContainer = Color.White,
        outline = Color.Gray,
        inverseOnSurface = Color.White,
        inverseSurface = Color.DarkGray,
        inversePrimary = colors.primary,
        surfaceVariant = colors.background,
        onSurfaceVariant = colors.onBackground,
        scrim = Color.Black.copy(alpha = 0.5f),
        tertiary = colors.primary,
        onTertiary = colors.onPrimary,
        tertiaryContainer = colors.primary,
        onTertiaryContainer = colors.onPrimary,
        surfaceTint = Color.White,
        outlineVariant = Color.White
    )

    val view = LocalView.current
    val activity = view.context as? Activity

    SideEffect {
        activity?.window?.apply {
            if (Build.VERSION.SDK_INT >= 21) {
                statusBarColor = colorScheme.primary.toArgb()
                navigationBarColor = colorScheme.primary.toArgb()
            }
            WindowCompat.getInsetsController(this, decorView).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme, typography = MaterialTheme.typography, shapes = MaterialTheme.shapes, content = content
    )
}
