/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.vending.billing.ui

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.android.vending.R
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import org.microg.vending.billing.TAG
import org.microg.vending.billing.core.ui.*
import org.microg.vending.billing.ui.logic.BillingUiViewState
import org.microg.vending.billing.ui.logic.InAppBillingViewModel
import org.microg.vending.billing.ui.theme.InAppBillingTheme
import org.microg.vending.billing.ui.widgets.LoadingDialog
import org.microg.vending.billing.ui.widgets.PasswdInputDialog

val LocalBillingUiViewState =
    compositionLocalOf<BillingUiViewState> { error("No default value provided.") }

@Composable
fun SetStatusBarColor(color: Color) {
    val systemUiController = rememberSystemUiController()
    SideEffect {
        systemUiController.setSystemBarsColor(color)
    }
}

@RequiresApi(21)
@Composable
fun BillingUiPage(viewModel: InAppBillingViewModel) {
    InAppBillingTheme {
        SetStatusBarColor(MaterialTheme.colorScheme.background)
        CompositionLocalProvider(LocalBillingUiViewState provides viewModel.billingUiViewState) {
            BillingUiView(viewModel.billingUiViewState)
        }
        LoadingDialog(viewModel.loadingDialogVisible)
        PasswdInputDialog(viewModel.passwdInputViewState)
    }
}

@Composable
private fun BillingUiView(viewState: BillingUiViewState) {
    if (!viewState.visible)
        return
    BackHandler(true) {
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "OnBackPressed")
    }
    Surface(
        modifier = Modifier
            .wrapContentHeight()
            .wrapContentWidth(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
        ) {
            UIComponents(viewState)
        }
    }
}

@Composable
private fun UIComponents(viewState: BillingUiViewState) {
    val uiComponents = viewState.showScreen.uiComponents ?: return
    val headerComponents = uiComponents.headerComponents
    val contentComponents = uiComponents.contentComponents
    val footerComponents = uiComponents.footerComponents
    val action = viewState.showScreen.action
    if (headerComponents.isNotEmpty()) {
        Column(
            modifier = Modifier
                .wrapContentHeight(Alignment.CenterVertically)
                .wrapContentWidth()
        ) {
            for (component in headerComponents) {
                if (component.uiInfo?.uiType == UIType.BILLING_PROFILE_MORE_OPTION_BUTTON_SHOW_HIDEABLE_INSTRUMENT)
                    continue
                UIComponent(Modifier, component, viewState)
            }
        }
    }
    if (contentComponents.isNotEmpty() || footerComponents.isNotEmpty()) {
        Column(
            modifier = Modifier
                .applyUITypePadding(viewState.showScreen.uiInfo?.uiType)
                .wrapContentHeight(Alignment.CenterVertically)
                .wrapContentWidth()
                .verticalScroll(rememberScrollState())
        ) {
            for (component in contentComponents) {
                UIComponent(
                    Modifier,
                    component,
                    viewState
                )
            }
            if (viewState.showScreen.uiInfo?.uiType == UIType.PURCHASE_ERROR_SCREEN) {
                Box(
                    modifier = Modifier
                        .padding(top = 40.dp)
                )
            }

            for (component in footerComponents) {
                UIComponent(Modifier, component, viewState)
            }
        }
    }

    Box(modifier = Modifier.padding(top = 15.dp))
    if (action?.type == ActionType.DELAY && action.delay != null) {
        viewState.onClickAction(action)
    }
}

@Composable
private fun UIComponent(
    modifier: Modifier = Modifier,
    component: BComponent,
    viewState: BillingUiViewState
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .applyViewInfo(
                component.viewInfo
            )
            .wrapContentHeight(align = Alignment.CenterVertically),
    ) {
        when (val type = component.viewType) {
            ViewType.ICONTEXTCOMBINATIONVIEW -> IconTextCombinationView(
                modifier = applyAlignment(Modifier, component.viewInfo),
                component.iconTextCombinationView!!
            )

            ViewType.CLICKABLETEXTVIEW -> ClickableTextView(
                modifier = applyAlignment(Modifier, component.viewInfo),
                component.clickableTextView!!
            )

            ViewType.VIEWGROUP -> ViewGroup(
                modifier = applyAlignment(Modifier, component.viewInfo),
                component.viewGroup!!
            )

            ViewType.DIVIDERVIEW -> DividerView(
                modifier = applyAlignment(Modifier, component.viewInfo)
            )

            ViewType.MODULOIMAGEVIEW -> ModuloImageView(
                modifier = applyAlignment(Modifier, component.viewInfo),
                component.moduloImageView!!
            )

            ViewType.BUTTONGROUPVIEW -> ButtonGroupView(
                modifier = applyAlignment(Modifier, component.viewInfo),
                viewState,
                component.buttonGroupView!!
            )

            ViewType.INSTRUMENTITEMVIEW -> InstrumentItemView(
                modifier = applyAlignment(Modifier, component.viewInfo),
                viewState,
                component.instrumentItemView!!
            )

            else -> Log.d(TAG, "invalid component type $type")
        }
    }
}

@Composable
private fun PlayTextView(
    modifier: Modifier = Modifier,
    data: BPlayTextView
) {
    val textColor = getColorByType(data.textInfo?.colorType)
        ?: LocalContentColor.current
    var textValue = ""
    if (data.textSpan.isNotEmpty()) {
        data.textSpan.forEach {
            when (it.textSpanType) {
                TextSpanType.BULLETSPAN -> textValue += "\u2022 "
                else -> {
                    if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "Unknown TextSpan type")
                }
            }
        }
    }
    val fontSize = getFontSizeByType(data.textInfo?.styleType)
    textValue += data.text
    HtmlText(
        text = textValue,
        color = textColor,
        fontSize = fontSize,
        textAlign = getTextAlignment(data.textInfo),
        maxLines = data.textInfo?.maxLines ?: Int.MAX_VALUE,
        modifier = modifier.applyViewInfo(data.viewInfo),
        lineHeight = fontSize.times(1.1)
    )
}

@Composable
private fun IconTextCombinationView(
    modifier: Modifier = Modifier,
    data: BIconTextCombinationView
) {
    Column(
        modifier = modifier
            .wrapContentHeight()
            .wrapContentWidth()
    ) {
        //角标
        data.badgeTextView?.let {
            PlayTextView(
                modifier = applyAlignment(Modifier, it.viewInfo),
                data = it
            )
        }
        Row(
            modifier = Modifier
                .wrapContentHeight()
                .wrapContentWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            data.headerImageView?.let {
                ImageView(
                    data = it,
                    modifier = applyAlignment(Modifier, it.viewInfo),
                )
            }
            data.playTextView?.let {
                PlayTextView(modifier = applyAlignment(Modifier, it.viewInfo), data = it)
            }
            if (data.middleTextViewList == null)
                return
            Column(
                modifier = applyAlignment(Modifier, data.viewInfo)
                    .applyViewInfo(data.viewInfo)
                    .wrapContentHeight()
                    .wrapContentWidth()
            ) {
                for (singleLineTextView in data.middleTextViewList) {
                    Row(
                        modifier = Modifier
                            .wrapContentWidth()
                            .wrapContentHeight()
                    ) {
                        singleLineTextView.playTextView1?.let {
                            PlayTextView(
                                applyAlignment(
                                    Modifier,
                                    it.viewInfo
                                ).wrapContentWidth(align = Alignment.CenterHorizontally),
                                data = it
                            )
                        }
                        singleLineTextView.playTextView2?.let {
                            PlayTextView(
                                applyAlignment(
                                    Modifier,
                                    it.viewInfo
                                ).fillMaxWidth(),
                                data = it
                            )
                        }
                    }
                }
            }
            if (data.footerImageGroup?.imageViews?.isNotEmpty() == true) {
                val viewInfo = data.footerImageGroup.viewInfo
                val imageViews = data.footerImageGroup.imageViews
                Spacer(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
                Column(
                    modifier = applyAlignment(Modifier, viewInfo)
                        .applyViewInfo(viewInfo)
                        .wrapContentHeight()
                        .wrapContentWidth()
                ) {
                    imageViews.forEach {
                        ImageView(
                            data = it,
                            modifier = applyAlignment(Modifier, it.viewInfo),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ClickableTextView(
    modifier: Modifier = Modifier,
    data: BClickableTextView
) {
    val playTextView = data.playTextView ?: return
    if (playTextView.text.isEmpty())
        return
    PlayTextView(modifier, data = playTextView)
}

@Composable
private fun InstrumentItemView(
    modifier: Modifier = Modifier,
    viewState: BillingUiViewState,
    data: BInstrumentItemView
) {
    var newModify = modifier
        .wrapContentHeight()
        .fillMaxWidth()
    newModify = when (data.action?.uiInfo?.uiType) {
        UIType.BILLING_PROFILE_EXISTING_INSTRUMENT,
        UIType.BILLING_PROFILE_OPTION_ADD_PLAY_CREDIT,
        UIType.BILLING_PROFILE_OPTION_REDEEM_CODE,
        UIType.BILLING_PROFILE_OPTION_CREATE_INSTRUMENT -> {
            newModify.clickable {
                viewState.onClickAction(data.action)
            }
        }

        else -> newModify
    }

    Row(
        modifier = newModify
    ) {
        data.icon?.let {
            ImageView(
                data = it,
                modifier = applyAlignment(Modifier, it.viewInfo)
                    .align(Alignment.CenterVertically),
                size = DpSize(32.dp, 32.dp)
            )
        }
        if (data.text != null || data.tips != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .wrapContentSize(),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center
            ) {
                data.text?.let {
                    PlayTextView(
                        applyAlignment(
                            Modifier,
                            it.viewInfo
                        ).wrapContentSize(),
                        data = it
                    )
                }
                data.tips?.let {
                    PlayTextView(
                        applyAlignment(
                            Modifier,
                            it.viewInfo
                        ).wrapContentSize(),
                        data = it
                    )
                }
                data.extraInfo?.let {
                    PlayTextView(
                        applyAlignment(
                            Modifier,
                            it.viewInfo
                        ).wrapContentSize(),
                        data = it
                    )
                }
            }
        }
        data.state?.let {
            Spacer(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color.Red)
            )
            ImageView(
                data = it,
                modifier = applyAlignment(Modifier, it.viewInfo)
                    .align(Alignment.CenterVertically),
                size = DpSize(32.dp, 32.dp)
            )
        }
    }
}

@Composable
private fun ButtonGroupView(
    modifier: Modifier = Modifier,
    viewState: BillingUiViewState,
    data: BButtonGroupView
) {
    if (data.buttonViewList.isNullOrEmpty())
        return
    Row(
        modifier = modifier
            .wrapContentHeight(align = Alignment.CenterVertically)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        data.buttonViewList.forEach {
            Button(
                onClick = { viewState.onClickAction(it.action) },
                shape = RoundedCornerShape(10),
                modifier = modifier
                    .weight(1f)
                    .defaultMinSize(minWidth = 1.dp, minHeight = 1.dp),
                contentPadding = PaddingValues(7.dp)
            ) {
                Text(
                    text = it.text
                )
            }
        }
    }
}

@Composable
private fun ImageView(
    modifier: Modifier = Modifier,
    data: BImageView,
    size: DpSize? = null,
) {
    ((if (isSystemInDarkTheme()) data.darkUrl else data.lightUrl) ?: data.darkUrl
    ?: data.lightUrl)?.let {
        AsyncImage(
            modifier = modifier
                .applyViewInfo(data.viewInfo)
                .applySize(size),
            model = it,
            contentDescription = data.viewInfo?.contentDescription,
            colorFilter = getColorFilter(data.imageInfo),
            contentScale = getContentScale(data.imageInfo) ?: ContentScale.Fit
        )
    }
    data.animation?.let {
        if (it.type == AnimationType.CHECK_MARK.value) {
            AnimatedVector(
                modifier = modifier
                    .applyViewInfo(data.viewInfo)
                    .applySize(size),
                R.drawable.anim_check_mark
            )
        }
    }
    data.iconView?.let {
        when (it.type) {
            1 -> Icon(
                Icons.Filled.CheckCircle,
                modifier = modifier
                    .applyViewInfo(data.viewInfo)
                    .size(24.dp),
                contentDescription = data.viewInfo?.contentDescription
            )

            3 -> Icon(
                Icons.Filled.KeyboardArrowRight,
                modifier = modifier
                    .applyViewInfo(data.viewInfo)
                    .size(24.dp),
                contentDescription = data.viewInfo?.contentDescription
            )

            21 -> Icon(
                Icons.Filled.ArrowBack,
                modifier = modifier
                    .applyViewInfo(data.viewInfo)
                    .size(48.dp),
                contentDescription = data.viewInfo?.contentDescription
            )

            27 -> Icon(
                Icons.Outlined.Info,
                modifier = modifier
                    .applyViewInfo(data.viewInfo)
                    .size(11.dp),
                contentDescription = data.viewInfo?.contentDescription
            )

            99 -> Image(
                painter = painterResource(R.drawable.google_play),
                modifier = modifier
                    .applyViewInfo(data.viewInfo)
                    .applySize(size),
                contentDescription = data.viewInfo?.contentDescription,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground)
            )
        }
    }
}

@Composable
private fun ModuloImageView(
    modifier: Modifier = Modifier,
    data: BModuloImageView
) {
    data.imageView?.let {
        ImageView(modifier = modifier, data = it)
    }
}

@Composable
private fun ViewGroup(
    modifier: Modifier = Modifier,
    data: BViewGroup
) {
    Row(
        modifier = modifier
            .wrapContentHeight()
            .wrapContentHeight()
    ) {
        data.imageView2?.let {
            ImageView(modifier = applyAlignment(Modifier, it.viewInfo), data = it)
        }
        data.playTextView?.let {
            PlayTextView(modifier = applyAlignment(Modifier, it.viewInfo), data = it)
        }
        data.imageView3?.let {
            Spacer(modifier = Modifier.weight(1f))
            ImageView(modifier = applyAlignment(Modifier, it.viewInfo), data = it)
        }
    }
}

@Composable
private fun DividerView(
    modifier: Modifier = Modifier
) {
    Divider(
        modifier = modifier
    )
}

@OptIn(ExperimentalAnimationGraphicsApi::class)
@Composable
private fun AnimatedVector(modifier: Modifier = Modifier, @DrawableRes drawableId: Int) {
    val image = AnimatedImageVector.animatedVectorResource(drawableId)
    var atEnd by remember { mutableStateOf(false) }
    Image(
        painter = rememberAnimatedVectorPainter(image, atEnd),
        contentDescription = "",
        modifier = modifier
    )
    atEnd = true
}