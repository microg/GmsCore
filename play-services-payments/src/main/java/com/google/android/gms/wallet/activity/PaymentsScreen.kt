/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.wallet.activity

import android.os.Build
import android.text.Html
import android.util.Log
import android.text.Spanned
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import org.microg.vending.billing.proto.LayoutModeProto
import org.microg.vending.billing.proto.PageElement

private const val TAG = "PaymentsScreen"

@Composable
fun PaymentsScreen(
    pageElements: List<PageElement>,
    elementMap: Map<Long, PageElement> = emptyMap(),
    layoutModes: Map<Long, LayoutModeProto> = emptyMap(),
    condManaged: Set<Long> = emptySet(),
    condVisible: Set<Long> = emptySet(),
    animatedImageStateProvider: (Long) -> Int = { 1 },
    inputTextProvider: (Long) -> String = { "" },
    onTextChange: ((Long, String) -> Unit)? = null,
    modifier: Modifier = Modifier,
    onButtonClick: ((Long) -> Unit)? = null
) {
    val childIds = mutableSetOf<Long>()
    for (element in pageElements) {
        val kids = element.dataValue?.childComponentIds?.map { it.toLong() }?.takeIf { it.isNotEmpty() }
            ?: element.verticalContainerExtension?.options?.flatMap { it.children }
            ?: element.groupingDataExtension?.groupedDataReferenceList
            ?: emptyList()
        childIds.addAll(kids)
    }
    for ((_, element) in elementMap) {
        val kids = element.verticalContainerExtension?.options?.flatMap { it.children }
            ?: element.groupingDataExtension?.groupedDataReferenceList
            ?: emptyList()
        childIds.addAll(kids)
    }

    val rootElements = pageElements.filter { (it.dataValue?.componentId ?: 0L) !in childIds }
    Log.d(TAG, "PaymentsScreen render: total=${pageElements.size}, " +
            "roots=${rootElements.map { it.dataValue?.componentId }}, " +
            "condManaged=$condManaged, condVisible=$condVisible")

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        rootElements.forEach { element ->
            PageElementItem(
                element = element,
                elementMap = elementMap,
                layoutModes = layoutModes,
                condManaged = condManaged,
                condVisible = condVisible,
                animatedImageStateProvider = animatedImageStateProvider,
                inputTextProvider = inputTextProvider,
                onTextChange = onTextChange,
                onButtonClick = onButtonClick
            )
        }
    }
}

@Composable
fun PageElementItem(
    element: PageElement,
    elementMap: Map<Long, PageElement> = emptyMap(),
    layoutModes: Map<Long, LayoutModeProto> = emptyMap(),
    condManaged: Set<Long> = emptySet(),
    condVisible: Set<Long> = emptySet(),
    animatedImageStateProvider: (Long) -> Int = { 1 },
    inputTextProvider: (Long) -> String = { "" },
    onTextChange: ((Long, String) -> Unit)? = null,
    onButtonClick: ((Long) -> Unit)? = null
) {
    val cid = element.dataValue?.componentId

    if (cid != null && cid in condManaged && cid !in condVisible) {
        Log.d(TAG, "SKIP cid=$cid (cond filter: managed but not visible)")
        return
    }

    val extensionFieldNumber = element.extensionFieldNumber
    if (extensionFieldNumber == null) {
        Log.d(TAG, "SKIP cid=$cid (no extensionFieldNumber)")
        return
    }

    when (extensionFieldNumber) {
        217440216 -> ImageDataComponent(element)
        223344552 -> TextComponent(element)
        217437962 -> InputFieldComponent(element, inputTextProvider, onTextChange)
        232057536 -> ButtonComponent(element, onButtonClick)
        // 233780159 (gcru/CardContainer): Infrastructure data component, NOT a UI container
        // It contains InfrastructureResultingAction in field 6 (submit/finish handlers),
        // NOT child PageElements. The card UI comes from parent LayoutContainer styling.
        // Children are NOT in dataValue.childComponentIds (it's empty), not in GroupingDataExtension.
        // Fix: Do not render as card - parent LayoutContainer applies card styling.
        233780159 -> { /* CardContainer: data component, rendered by parent as card */ }
        265527174 -> AnimatedImageComponent(element, animatedImageStateProvider(cid ?: 0L))
        223344553 -> ConditionalContainerComponent(element, elementMap, layoutModes, condManaged, condVisible, animatedImageStateProvider, inputTextProvider, onTextChange, onButtonClick)
        223344555 -> LayoutContainerComponent(element, elementMap, layoutModes, condManaged, condVisible, animatedImageStateProvider, inputTextProvider, onTextChange, onButtonClick)
        264984587 -> SpacerElementComponent(element)
        265529774 -> FlexContainerComponent(element, elementMap, layoutModes, condManaged, condVisible, animatedImageStateProvider, inputTextProvider, onTextChange, onButtonClick)
        232946268 -> { /* SMS auto reader: not wired */ }
        else -> {
            Log.w(TAG, "Unhandled extensionFieldNumber=$extensionFieldNumber, componentId=$cid")
        }
    }
}

@Composable
fun ImageDataComponent(element: PageElement) {
    val imageData = element.imageDataExtension?.imageData ?: return
    val layoutDirection = LocalLayoutDirection.current

    val imageUrl = imageData.imageUrl
        ?: imageData.values.firstOrNull()?.imageUrl
        ?: return

    val mirrored = imageData.isAutoMirrored == true &&
            layoutDirection == LayoutDirection.Rtl

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = rememberAsyncImagePainter(imageUrl),
            contentDescription = imageData.title,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .heightIn(max = 64.dp)
                .widthIn(max = 200.dp)
                .then(
                    if (mirrored) Modifier.graphicsLayer { scaleX = -1f }
                    else Modifier
                )
        )
    }
}

@Composable
fun TextComponent(element: PageElement) {
    val text = element.textInfoDataExtension?.text
        ?: element.textInfoDataExtension?.displayText?.text
        ?: return

    val spanned = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        Html.fromHtml(text, Html.FROM_HTML_MODE_COMPACT)
    } else {
        @Suppress("DEPRECATION")
        Html.fromHtml(text)
    }

    val annotated = buildHtmlAnnotatedString(spanned)

    Text(
        text = annotated,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun ButtonComponent(element: PageElement, onButtonClick: ((Long) -> Unit)?) {
    val componentId = element.dataValue?.componentId ?: 0L
    val buttonText = element.dataValue?.message204201689?.text

    if (buttonText.isNullOrEmpty()) {
        return
    }

    Button(
        onClick = { onButtonClick?.invoke(componentId) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = buttonText)
    }
}

@Composable
fun AnimatedImageComponent(element: PageElement, animatedImageState: Int) {
    val text = element.animatedImageDataExtension?.conditionalContentSource?.contentDescription ?: ""

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (animatedImageState != 3) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp).padding(8.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }
        if (text.isNotEmpty()) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

/**
 *
 * layoutMode=1 (RELATIVE): Box
 * layoutMode=2 (FLEX): Column
 */
@Composable
fun LayoutContainerComponent(element: PageElement, elementMap: Map<Long, PageElement>, layoutModes: Map<Long, LayoutModeProto>, condManaged: Set<Long> = emptySet(), condVisible: Set<Long> = emptySet(), animatedImageStateProvider: (Long) -> Int = { 1 }, inputTextProvider: (Long) -> String = { "" }, onTextChange: ((Long, String) -> Unit)? = null, onButtonClick: ((Long) -> Unit)?) {
    val cid = element.dataValue?.componentId ?: 0L
    val childIds = element.dataValue?.childComponentIds?.map { it.toLong() }?.takeIf { it.isNotEmpty() }
        ?: element.verticalContainerExtension?.options?.flatMap { it.children }
        ?: element.groupingDataExtension?.groupedDataReferenceList
        ?: emptyList()

    val layoutMode = layoutModes[cid] ?: LayoutModeProto.LAYOUT_MODE_FLEX
    if (layoutMode == LayoutModeProto.LAYOUT_MODE_RELATIVE) {
        Box(modifier = Modifier.fillMaxWidth()) {
            childIds.forEach { childId ->
                val childElement = elementMap[childId] ?: return@forEach
                PageElementItem(element = childElement, elementMap = elementMap, layoutModes = layoutModes, condManaged = condManaged, condVisible = condVisible, animatedImageStateProvider = animatedImageStateProvider, inputTextProvider = inputTextProvider, onTextChange = onTextChange, onButtonClick = onButtonClick)
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            childIds.forEach { childId ->
                val childElement = elementMap[childId] ?: return@forEach
                PageElementItem(element = childElement, elementMap = elementMap, layoutModes = layoutModes, condManaged = condManaged, condVisible = condVisible, animatedImageStateProvider = animatedImageStateProvider, inputTextProvider = inputTextProvider, onTextChange = onTextChange, onButtonClick = onButtonClick)
            }
        }
    }
}

@Composable
fun SpacerElementComponent(element: PageElement) {
    val height = element.dataValue?.spacerHeight?.toInt() ?: 8
    Spacer(modifier = Modifier.height(height.dp))
}

@Composable
fun ConditionalContainerComponent(element: PageElement, elementMap: Map<Long, PageElement>, layoutModes: Map<Long, LayoutModeProto>, condManaged: Set<Long> = emptySet(), condVisible: Set<Long> = emptySet(), animatedImageStateProvider: (Long) -> Int = { 1 }, inputTextProvider: (Long) -> String = { "" }, onTextChange: ((Long, String) -> Unit)? = null, onButtonClick: ((Long) -> Unit)?) {
}

@Composable
fun FlexContainerComponent(element: PageElement, elementMap: Map<Long, PageElement>, layoutModes: Map<Long, LayoutModeProto>, condManaged: Set<Long> = emptySet(), condVisible: Set<Long> = emptySet(), animatedImageStateProvider: (Long) -> Int = { 1 }, inputTextProvider: (Long) -> String = { "" }, onTextChange: ((Long, String) -> Unit)? = null, onButtonClick: ((Long) -> Unit)?) {
    val childIds = element.dataValue?.childComponentIds?.map { it.toLong() }?.takeIf { it.isNotEmpty() }
        ?: element.verticalContainerExtension?.options?.flatMap { it.children }
        ?: element.groupingDataExtension?.groupedDataReferenceList
        ?: emptyList()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        childIds.forEach { childId ->
            val childElement = elementMap[childId] ?: return@forEach
            Box(modifier = Modifier.weight(1f)) {
                PageElementItem(element = childElement, elementMap = elementMap, layoutModes = layoutModes, condManaged = condManaged, condVisible = condVisible, animatedImageStateProvider = animatedImageStateProvider, inputTextProvider = inputTextProvider, onTextChange = onTextChange, onButtonClick = onButtonClick)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputFieldComponent(
    element: PageElement,
    inputTextProvider: (Long) -> String,
    onTextChange: ((Long, String) -> Unit)?
) {
    val cid = element.dataValue?.componentId ?: return
    val currentText = inputTextProvider(cid)
    val henc = element.textInputFieldExtension
    val hint = henc?.hint?.takeIf { it.isNotEmpty() } ?: element.errorText ?: ""
    val label = henc?.labelPrefix ?: henc?.labelCaption ?: ""

    OutlinedTextField(
        value = currentText,
        onValueChange = { newText ->
            Log.d(TAG, "InputFieldComponent[cid=$cid] onValueChange: newText.len=${newText.length}")
            onTextChange?.invoke(cid, newText)
        },
        label = if (label.isNotEmpty()) { { Text(label) } } else null,
        placeholder = if (hint.isNotEmpty()) { { Text(hint) } } else null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        modifier = Modifier.fillMaxWidth()
    )
}

// Spanned → Compose AnnotatedString
private fun buildHtmlAnnotatedString(spanned: Spanned): AnnotatedString {
    return buildAnnotatedString {
        append(spanned.toString())
        for (span in spanned.getSpans(0, spanned.length, Any::class.java)) {
            val start = spanned.getSpanStart(span)
            val end = spanned.getSpanEnd(span)
            when (span) {
                is StyleSpan -> when (span.style) {
                    android.graphics.Typeface.BOLD -> addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, end)
                    android.graphics.Typeface.ITALIC -> addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, end)
                    android.graphics.Typeface.BOLD_ITALIC -> addStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic), start, end)
                }
                is UnderlineSpan -> addStyle(SpanStyle(textDecoration = TextDecoration.Underline), start, end)
            }
        }
    }
}
