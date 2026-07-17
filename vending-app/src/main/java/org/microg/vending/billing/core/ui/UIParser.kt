package org.microg.vending.billing.core.ui

import okio.ByteString
import org.json.JSONObject
import org.microg.vending.billing.core.AcquireParams
import org.microg.vending.billing.core.PurchaseItem
import org.microg.vending.billing.core.parsePurchaseItem
import org.microg.vending.billing.core.responseBundleToMap
import org.microg.vending.billing.proto.*

data class AcquireParsedResult(
    val action: BAction? = null,
    val result: Map<String, Any> = mapOf("RESPONSE_CODE" to 4, "DEBUG_MESSAGE" to ""),
    val purchaseItems: List<PurchaseItem>,
    val screenMap: Map<String, BScreen> = emptyMap()
)

private fun typeToDpSize(type: Int): Float {
    return when (type) {
        1 -> 2.0f
        2 -> 4.0f
        3 -> 8.0f
        4 -> 12.0f
        5 -> 16.0f
        6 -> 24.0f
        7 -> 32.0f
        8 -> 24.0f
        9 -> 18.0f
        10 -> 0.0f
        11 -> 6.0f
        else -> {
            0.0f
        }
    }
}

private fun parseUIInfo(uiInfo: UIInfo): BUIInfo {
    if (uiInfo.classType == 1) return BUIInfo(UIType.UNKNOWN)
    return BUIInfo(UIType.fromValue(uiInfo.uiType ?: 0))
}

private fun parseScreen(screen: Screen): BScreen {
    var uiInfo: BUIInfo? = null
    var uiComponents: BUIComponents? = null
    var action: BAction? = null
    screen.uiInfo?.let {
        uiInfo = parseUIInfo(it)
    }
    screen.uiComponents?.let {
        uiComponents = parseScreenComponents(it)
    }
    screen.action?.let {
        action = BAction(ActionType.UNKNOWN)
        parseAction(it, action!!)
    }
    return BScreen(uiInfo, action, uiComponents, screen)
}

private fun parseScreenMap(screenMap: Map<String, Screen>): Map<String, BScreen> {
    val result: MutableMap<String, BScreen> = mutableMapOf()
    for ((screenId, screen) in screenMap) {
        val bScreen = parseScreen(screen)
        result[screenId] = bScreen
    }
    return result
}

fun parseAcquireResponse(acquireResponse: AcquireResponse): BAcquireResult {
    val action = BAction(ActionType.UNKNOWN)
    parseAction(acquireResponse.action, action)
    val screenMap = parseScreenMap(acquireResponse.screen)
    return BAcquireResult(action, screenMap)
}

private fun parseAction(action: Action?, result: BAction): Boolean {
    if (action == null) return false
    action.actionContext?.let { ctx ->
        if (ctx != ByteString.EMPTY) {
            result.actionContext.add(ctx.toByteArray())
        }
    }
    action.timerAction?.let { timerAction ->
        result.delay = timerAction.delay ?: 0
        result.type = ActionType.DELAY
        result.result = responseBundleToMap(timerAction.responseBundle)
        return true
    }
    action.actionExt?.extAction?.let { extAction ->
        extAction.droidGuardMap?.let { result.droidGuardMap = it.map }
        extAction.action?.let { return parseAction(it, result) }
    }
    action.showAction?.let { showAction ->
        result.type = ActionType.SHOW
        result.screenId = showAction.screenId
        showAction.action1?.let { parseAction(it, result) }
        showAction.action?.let { parseAction(it, result) }
        return true
    }
    action.viewClickAction?.let { viewClickAction ->
        if (viewClickAction.uiInfo != null) {
            val parsedUiInfo = parseUIInfo(viewClickAction.uiInfo!!)
            if (parsedUiInfo.uiType != UIType.UNKNOWN) {
                result.uiInfo = parsedUiInfo
            }
        }
        return parseAction(viewClickAction.action, result)
    }
    action.optionalAction?.let { optionalAction ->
        return parseAction(optionalAction.action1, result)
    }
    action.navigateToPage?.let { navigateToPage ->
        result.srcScreenId = navigateToPage.from
        return parseAction(navigateToPage.action, result)
    }
    return false
}

private fun parseAnimation(animation: Animation): BAnimation {
    var type: Int? = null
    var repeatCount: Int? = null
    if (animation.type != 0) {
        type = animation.type
    }
    if (animation.repeatCount != 0) {
        repeatCount = animation.repeatCount
    }
    return BAnimation(type, repeatCount)
}

private fun parseIconView(iconView: IconView): BIconView {
    var type: Int? = null
    var text: String? = null
    if (iconView.type != 0) {
        type = iconView.type
    }
    if (iconView.text?.isNotBlank() == true) {
        text = iconView.text
    }
    return BIconView(type, text)
}

private fun parseInstrumentItemView(instrumentItemView: InstrumentItemView): BInstrumentItemView {
    var icon: BImageView? = null
    var text: BPlayTextView? = null
    var tips: BPlayTextView? = null
    var extraInfo: BPlayTextView? = null
    var state: BImageView? = null
    var action: BAction? = null
    if (instrumentItemView.icon != null) {
        icon = parseImageView(instrumentItemView.icon!!)
    }
    if (instrumentItemView.text != null) {
        text = parsePlayTextView(instrumentItemView.text!!)
    }
    if (instrumentItemView.tips != null) {
        tips = parsePlayTextView(instrumentItemView.tips!!)
    }
    if (instrumentItemView.state != null) {
        state = parseImageView(instrumentItemView.state!!)
    }
    if (instrumentItemView.action != null) {
        action = BAction(ActionType.UNKNOWN)
        parseAction(instrumentItemView.action, action)
    }
    if (instrumentItemView.extraInfo != null) {
        extraInfo = parsePlayTextView(instrumentItemView.extraInfo!!)
    }
    return BInstrumentItemView(icon, text, tips, extraInfo, state, action)
}

private fun parseImageGroup(imageGroup: ImageGroup): BImageGroup {
    val imageViews = mutableListOf<BImageView>()
    var viewInfo: BViewInfo? = null

    if (imageGroup.viewInfo != null) {
        viewInfo = parseViewInfo(imageGroup.viewInfo!!)
    }
    imageGroup.imageView.forEach {
        imageViews.add(parseImageView(it))
    }
    return BImageGroup(imageViews, viewInfo)
}

private fun parseImageView(imageView: ImageView): BImageView {
    var darkUrl: String? = null
    var lightUrl: String? = null
    var viewInfo: BViewInfo? = null
    var imageInfo: BImageInfo? = null
    var iconView: BIconView? = null
    var animation: BAnimation? = null
    imageView.thumbnailImageView?.let { thumb ->
        if (thumb.darkUrl?.isNotBlank() == true) darkUrl = thumb.darkUrl
        if (thumb.lightUrl?.isNotBlank() == true) lightUrl = thumb.lightUrl
    }
    imageView.viewInfo?.let { viewInfo = parseViewInfo(it) }
    imageView.imageInfo?.let { imageInfo = parseImageInfo(it) }
    imageView.iconView?.let { iconView = parseIconView(it) }
    imageView.animation?.let { animation = parseAnimation(it) }
    return BImageView(viewInfo, imageInfo, lightUrl, darkUrl, animation, iconView)
}

private fun parseTextInfo(textInfo: TextInfo): BTextInfo {
    var colorType: ColorType? = null
    var maxLines: Int? = null
    var gravityList: List<BGravity>? = null
    var textAlignmentType: TextAlignmentType? = null
    var styleType: Int? = null
    if (textInfo.maxLines != 0) {
        maxLines = textInfo.maxLines
    }
    if (textInfo.gravity.isNotEmpty()) {
        gravityList = textInfo.gravity.map { BGravity.values()[it] }
    }
    textInfo.textColorType?.let { colorType = ColorType.values()[it] }
    if ((textInfo.textAlignmentType ?: 0) != 0) {
        textAlignmentType = TextAlignmentType.values()[textInfo.textAlignmentType ?: 0]
    }
    if (textInfo.styleType != 0) {
        styleType = textInfo.styleType
    }
    return BTextInfo(colorType, maxLines, gravityList, textAlignmentType, styleType)
}

private fun parseBulletSpan(bulletSpan: BulletSpan): BBulletSpan {
    return BBulletSpan(bulletSpan.gapWidth?.unitValue?.toInt() ?: 0)
}

private fun parseTextSpan(textSpan: TextSpan): BTextSpan {
    return if (textSpan.bulletSpan != null) {
        BTextSpan(
            TextSpanType.BULLETSPAN,
            parseBulletSpan(textSpan.bulletSpan!!)
        )
    } else {
        BTextSpan(TextSpanType.UNKNOWNSPAN)
    }
}

private fun parsePlayTextView(playTextView: PlayTextView): BPlayTextView {
    var textInfo: BTextInfo? = null
    var viewInfo: BViewInfo? = null
    var textSpanList: MutableList<BTextSpan> = mutableListOf()
    if (playTextView.textInfo != null) {
        textInfo = parseTextInfo(playTextView.textInfo!!)
    }
    if (playTextView.viewInfo != null) {
        viewInfo = parseViewInfo(playTextView.viewInfo!!)
    }
    if (playTextView.textSpan.isNotEmpty()) {
        playTextView.textSpan.forEach {
            textSpanList.add(parseTextSpan(it))
        }
    }
    return BPlayTextView(
        playTextView.text ?: "",
        playTextView.isHtml ?: false,
        textInfo,
        viewInfo,
        textSpanList
    )
}

private fun parseSingleLineTextView(singleLineTextView: SingleLineTextView): BSingleLineTextView {
    var playTextView1: BPlayTextView? = null
    var playTextView2: BPlayTextView? = null

    if (singleLineTextView.playTextView1 != null)
        playTextView1 = parsePlayTextView(singleLineTextView.playTextView1!!)
    if (singleLineTextView.playTextView2 != null)
        playTextView2 = parsePlayTextView(singleLineTextView.playTextView2!!)

    return BSingleLineTextView(playTextView1, playTextView2)
}

private fun parseIconTextCombinationView(iconTextCombinationView: IconTextCombinationView): BIconTextCombinationView {
    var headerImageView: BImageView? = null
    var playTextView: BPlayTextView? = null
    var badgeTextView: BPlayTextView? = null
    var middleTextViewList: List<BSingleLineTextView>? = null
    var viewInfo: BViewInfo? = null
    var footerImageGroup: BImageGroup? = null

    if (iconTextCombinationView.headerImageView != null) {
        headerImageView = parseImageView(iconTextCombinationView.headerImageView!!)
    }
    if (iconTextCombinationView.playTextView != null) {
        playTextView = parsePlayTextView(iconTextCombinationView.playTextView!!)
    }
    if (iconTextCombinationView.badgeTextView != null) {
        badgeTextView = parsePlayTextView(iconTextCombinationView.badgeTextView!!)
    }
    if (iconTextCombinationView.singleLineTextView.isNotEmpty()) {
        middleTextViewList =
            iconTextCombinationView.singleLineTextView.map { parseSingleLineTextView(it) }
    }
    if (iconTextCombinationView.footerImageGroup != null) {
        footerImageGroup = parseImageGroup(iconTextCombinationView.footerImageGroup!!)
    }
    if (iconTextCombinationView.viewInfo != null) {
        viewInfo = parseViewInfo(iconTextCombinationView.viewInfo!!)
    }

    return BIconTextCombinationView(
        headerImageView,
        playTextView,
        badgeTextView,
        middleTextViewList,
        footerImageGroup,
        viewInfo
    )
}

private fun parseClickableTextView(clickableTextView: ClickableTextView): BClickableTextView {
    var playTextView: BPlayTextView? = null
    if (clickableTextView.playTextView != null)
        playTextView = parsePlayTextView(clickableTextView.playTextView!!)
    return BClickableTextView(playTextView)
}

private fun parseViewGroup(viewGroup: ViewGroup): BViewGroup {
    var imageView1: BImageView? = null
    var imageView2: BImageView? = null
    var imageView3: BImageView? = null
    var imageView4: BImageView? = null
    var playTextView: BPlayTextView? = null
    if (viewGroup.imageView1 != null) {
        imageView1 = parseImageView(viewGroup.imageView1!!)
    }
    if (viewGroup.imageView2 != null) {
        imageView2 = parseImageView(viewGroup.imageView2!!)
    }
    if (viewGroup.imageView3 != null) {
        imageView3 = parseImageView(viewGroup.imageView3!!)
    }
    if (viewGroup.imageView4 != null) {
        imageView4 = parseImageView(viewGroup.imageView4!!)
    }
    if (viewGroup.playTextView != null) {
        playTextView = parsePlayTextView(viewGroup.playTextView!!)
    }
    return BViewGroup(imageView1, imageView2, imageView3, imageView4, playTextView)
}

private fun parseModuloImageView(moduloImageView: ModuloImageView): BModuloImageView {
    var imageView: BImageView? = null
    if (moduloImageView.imageView != null) {
        imageView = parseImageView(moduloImageView.imageView!!)
    }
    return BModuloImageView(imageView)
}

private fun parseDividerView(dividerView: DividerView): BDividerView {
    return BDividerView()
}

private fun parseImageInfo(imageInfo: ImageInfo): BImageInfo {
    var colorFilterValue: Int? = null
    var colorFilterType: Int? = null
    var filterMode: Int? = null
    var scaleType: Int? = null
    if (imageInfo.value_ != null) {
        colorFilterValue = imageInfo.value_
    } else if (imageInfo.valueType != null) {
        colorFilterType = imageInfo.valueType
    }
    if (imageInfo.modeType != 0) {
        filterMode = imageInfo.modeType
    }
    if (imageInfo.scaleType != 0) {
        scaleType = imageInfo.scaleType
    }
    return BImageInfo(colorFilterValue, colorFilterType, filterMode, scaleType)
}

private fun parseViewInfo(viewInfo: ViewInfo): BViewInfo {
    var tag: String? = null
    var width: Float? = null
    var height: Float? = null
    var startMargin: Float? = null
    var topMargin: Float? = null
    var endMargin: Float? = null
    var bottomMargin: Float? = null
    var startPadding: Float? = null
    var topPadding: Float? = null
    var endPadding: Float? = null
    var bottomPadding: Float? = null
    var contentDescription: String? = null
    var gravityList: List<BGravity>? = null
    var backgroundColorType: ColorType? = null
    var borderColorType: ColorType? = null
    var action: BAction? = null
    var visibilityType: Int? = null

    if (viewInfo.tag?.isNotBlank() == true) {
        tag = viewInfo.tag
    }
    if (viewInfo.widthValue != 0f) {
        width = viewInfo.widthValue
    }
    if (viewInfo.heightValue != 0f) {
        height = viewInfo.heightValue
    }
    if (viewInfo.startMargin != 0f) {
        startMargin = viewInfo.startMargin
    }
    if (viewInfo.topMargin != 0f) {
        topMargin = viewInfo.topMargin
    }
    if (viewInfo.endMargin != 0f) {
        endMargin = viewInfo.endMargin
    }
    if (viewInfo.bottomMargin != 0f) {
        bottomMargin = viewInfo.bottomMargin
    }
    if (viewInfo.startPadding != 0f) {
        startPadding = viewInfo.startPadding
    }
    if (viewInfo.topPadding != 0f) {
        topPadding = viewInfo.topPadding
    }
    if (viewInfo.endPadding != 0f) {
        endPadding = viewInfo.endPadding
    }
    if (viewInfo.bottomPadding != 0f) {
        bottomPadding = viewInfo.bottomPadding
    }
    if (viewInfo.startMarginType != 0) {
        startMargin = typeToDpSize(viewInfo.startMarginType ?: 0)
    }
    if (viewInfo.topMarginType != 0) {
        topMargin = typeToDpSize(viewInfo.topMarginType ?: 0)
    }
    if (viewInfo.endMarginType != 0) {
        endMargin = typeToDpSize(viewInfo.endMarginType ?: 0)
    }
    if (viewInfo.bottomMarginType != 0) {
        bottomMargin = typeToDpSize(viewInfo.bottomMarginType ?: 0)
    }
    if (viewInfo.startPaddingType != 0) {
        startPadding = typeToDpSize(viewInfo.startPaddingType ?: 0)
    }
    if (viewInfo.topPaddingType != 0) {
        topPadding = typeToDpSize(viewInfo.topPaddingType ?: 0)
    }
    if (viewInfo.endPaddingType != 0) {
        endPadding = typeToDpSize(viewInfo.endPaddingType ?: 0)
    }
    if (viewInfo.bottomPaddingType != 0) {
        bottomPadding = typeToDpSize(viewInfo.bottomPaddingType ?: 0)
    }
    if (viewInfo.contentDescription?.isNotBlank() == true) {
        contentDescription = viewInfo.contentDescription
    }
    if (viewInfo.gravity.isNotEmpty()) {
        gravityList = viewInfo.gravity.map { BGravity.values()[it] }
    }
    if (viewInfo.backgroundColorType != 0) {
        backgroundColorType = ColorType.values()[viewInfo.backgroundColorType ?: 0]
    }
    if (viewInfo.borderColorType != 0) {
        borderColorType = ColorType.values()[viewInfo.borderColorType ?: 0]
    }
    if (viewInfo.action != null) {
        action = BAction(ActionType.UNKNOWN)
        parseAction(viewInfo.action, action)
    }
    if (viewInfo.visibilityType != 0) {
        visibilityType = viewInfo.visibilityType
    }
    return BViewInfo(
        tag,
        width,
        height,
        startMargin,
        topMargin,
        endMargin,
        bottomMargin,
        startPadding,
        topPadding,
        endPadding,
        bottomPadding,
        contentDescription,
        gravityList,
        backgroundColorType,
        borderColorType,
        action,
        visibilityType
    )
}

private fun parseContentComponent(contentComponent: ContentComponent): BComponent {
    val tag = contentComponent.tag
    val viewInfo = contentComponent.viewInfo?.let { parseViewInfo(it) }
    val uiInfo = contentComponent.uiInfo?.let {
        parseUIInfo(it) }
    val cc = contentComponent
    return when {
        cc.iconTextCombinationView != null -> BComponent(tag, uiInfo, viewInfo, ViewType.ICONTEXTCOMBINATIONVIEW, iconTextCombinationView = parseIconTextCombinationView(cc.iconTextCombinationView!!))
        cc.clickableTextView != null -> BComponent(tag, uiInfo, viewInfo, ViewType.CLICKABLETEXTVIEW, clickableTextView = parseClickableTextView(cc.clickableTextView!!))
        cc.viewGroup != null -> BComponent(tag, uiInfo, viewInfo, ViewType.VIEWGROUP, viewGroup = parseViewGroup(cc.viewGroup!!))
        cc.dividerView != null -> BComponent(tag, uiInfo, viewInfo, ViewType.DIVIDERVIEW, dividerView = parseDividerView(cc.dividerView!!))
        cc.moduloImageView != null -> BComponent(tag, uiInfo, viewInfo, ViewType.MODULOIMAGEVIEW, moduloImageView = parseModuloImageView(cc.moduloImageView!!))
        cc.buttonGroupView != null -> BComponent(tag, uiInfo, viewInfo, ViewType.BUTTONGROUPVIEW, buttonGroupView = parseButtonGroupView(cc.buttonGroupView!!))
        cc.instrumentItemView != null -> BComponent(tag, uiInfo, viewInfo, ViewType.INSTRUMENTITEMVIEW, instrumentItemView = parseInstrumentItemView(cc.instrumentItemView!!))
        else -> BComponent(viewType = ViewType.UNKNOWNVIEW)
    }
}

private fun parseButtonView(buttonView: ButtonView): BButtonView {
    var text = ""
    var viewInfo: BViewInfo? = null
    val action = BAction(ActionType.UNKNOWN)
    text = buttonView.text ?: ""
    if (buttonView.viewInfo != null) {
        viewInfo = parseViewInfo(buttonView.viewInfo!!)
    }
    if (buttonView.action != null) {
        parseAction(buttonView.action, action)
    }
    return BButtonView(text, viewInfo, action)
}

private fun parseButtonGroupView(buttonGroupView: ButtonGroupView): BButtonGroupView {
    val buttonViewList = mutableListOf<BButtonView>()
    buttonGroupView.newButtonView?.let { nbv ->
        nbv.buttonView?.let { buttonViewList.add(parseButtonView(it)) }
        nbv.buttonView2?.let { buttonViewList.add(parseButtonView(it)) }
    }
    return BButtonGroupView(buttonViewList)
}

private fun parseFooterComponent(footerComponent: FooterComponent): BComponent {
    val tag = footerComponent.tag
    val viewInfo = footerComponent.viewInfo?.let { parseViewInfo(it) }
    val uiInfo = footerComponent.uiInfo?.let {
        parseUIInfo(it) }
    val fc = footerComponent
    return when {
        fc.buttonGroupView != null -> BComponent(tag, uiInfo, viewInfo, ViewType.BUTTONGROUPVIEW, buttonGroupView = parseButtonGroupView(fc.buttonGroupView!!))
        fc.dividerView != null -> BComponent(tag, uiInfo, viewInfo, ViewType.DIVIDERVIEW, dividerView = parseDividerView(fc.dividerView!!))
        else -> BComponent(viewType = ViewType.UNKNOWNVIEW)
    }
}

private fun parseScreenComponents(uiComponents: UiComponents): BUIComponents {
    val headerComponents = mutableListOf<BComponent>()
    val contentComponents = mutableListOf<BComponent>()
    val footerComponents = mutableListOf<BComponent>()
    for (contentComponent in uiComponents.contentComponent1) {
        headerComponents.add(parseContentComponent(contentComponent))
    }
    for (contentComponent in uiComponents.contentComponent2) {
        contentComponents.add(parseContentComponent(contentComponent))
    }
    for (footerComponent in uiComponents.footerComponent) {
        footerComponents.add(parseFooterComponent(footerComponent))
    }

    return BUIComponents(headerComponents, contentComponents, footerComponents)
}

fun parsePurchaseResponse(
    acquireParams: AcquireParams,
    purchaseResponse: PurchaseResponse?
): Pair<Map<String, Any>, PurchaseItem?> {
    if (purchaseResponse == null) {
        return mapOf<String, Any>("RESPONSE_CODE" to 0, "DEBUG_MESSAGE" to "") to null
    }
    val resultMap = responseBundleToMap(purchaseResponse.responseBundle)
    val code = resultMap["RESPONSE_CODE"] as Int? ?: return mapOf<String, Any>(
        "RESPONSE_CODE" to 0,
        "DEBUG_MESSAGE" to ""
    ) to null
    val pd = resultMap["INAPP_PURCHASE_DATA"] as String? ?: return resultMap to null
    val ps = resultMap["INAPP_DATA_SIGNATURE"] as String? ?: return resultMap to null
    if (code != 0) return resultMap to null
    val pdj = JSONObject(pd)
    val packageName =
        pdj.optString("packageName").takeIf { it.isNotBlank() } ?: return resultMap to null
    val purchaseToken =
        pdj.optString("purchaseToken").takeIf { it.isNotBlank() } ?: return resultMap to null
    val purchaseState =
        pdj.optInt("purchaseState", -1).takeIf { it != -1 } ?: return resultMap to null
    return resultMap to PurchaseItem(
        acquireParams.buyFlowParams.skuType,
        acquireParams.buyFlowParams.sku,
        packageName,
        purchaseToken,
        purchaseState,
        pd, ps
    )
}

fun parseAcquireResponse(
    acquireParams: AcquireParams,
    acquireResponse: AcquireResponse
): AcquireParsedResult {
    val action = BAction(ActionType.UNKNOWN)
    parseAction(acquireResponse.action, action)
    val screenMap = parseScreenMap(acquireResponse.screen)
    val (primaryResult, primaryPurchaseItem) = parsePurchaseResponse(
        acquireParams,
        acquireResponse.acquireResult?.purchaseResponse
    )

    val (result, purchaseItem) = if (primaryPurchaseItem == null) {
        val (screenResult, screenItem) = extractPurchaseItemFromScreens(
            acquireParams,
            acquireResponse.screen
        )
        if (screenItem != null) {
            screenResult to screenItem
        } else {
            primaryResult to null
        }
    } else {
        primaryResult to primaryPurchaseItem
    }

    val purchaseItems = mutableSetOf<PurchaseItem>()
    acquireResponse.acquireResult?.ownedPurchase?.let { owned ->
        owned.purchaseItem.forEach {
            purchaseItems.addAll(parsePurchaseItem(it))
        }
    }
    if (purchaseItem != null) purchaseItems.add(purchaseItem)
    return AcquireParsedResult(action, result, purchaseItems.toList(), screenMap)
}
private fun extractPurchaseItemFromScreens(
    acquireParams: AcquireParams,
    screens: Map<String, Screen>
): Pair<Map<String, Any>, PurchaseItem?> {
    for ((screenId, screen) in screens) {
        val items = screen.actionState?.action?.finish?.purchaseResult?.items ?: continue
        if (items.isEmpty()) continue

        val kv = mutableMapOf<String, Any>()
        for (item in items) {
            val k = item.key ?: continue
            when {
                item.stringValue != null -> kv[k] = item.stringValue!!
                item.boolValue != null -> kv[k] = item.boolValue!!
                item.intValue != null -> kv[k] = item.intValue!!
            }
        }

        val purchaseData = (kv["INAPP_PURCHASE_DATA"] as? String)
            ?: (kv["FIRST_PARTY_PURCHASE_DATA"] as? String)
            ?: continue
        val signature = (kv["INAPP_DATA_SIGNATURE"] as? String) ?: ""
        val responseCode = when (val rc = kv["RESPONSE_CODE"]) {
            is Long -> rc.toInt()
            is Int -> rc
            null -> 0
            else -> 0
        }
        if (responseCode != 0) continue

        val pdj = try {
            JSONObject(purchaseData)
        } catch (e: Exception) {
            continue
        }
        val packageName = pdj.optString("packageName").takeIf { it.isNotBlank() } ?: continue
        val purchaseToken = pdj.optString("purchaseToken").takeIf { it.isNotBlank() } ?: continue
        val purchaseState = pdj.optInt("purchaseState", -1).takeIf { it != -1 } ?: continue

        val normalized = mapOf<String, Any>(
            "RESPONSE_CODE" to responseCode,
            "INAPP_PURCHASE_DATA" to purchaseData,
            "INAPP_DATA_SIGNATURE" to signature
        )
        return normalized to PurchaseItem(
            acquireParams.buyFlowParams.skuType,
            acquireParams.buyFlowParams.sku,
            packageName,
            purchaseToken,
            purchaseState,
            purchaseData,
            signature
        )
    }
    return mapOf<String, Any>("RESPONSE_CODE" to 0, "DEBUG_MESSAGE" to "") to null
}