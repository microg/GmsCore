package org.microg.vending.billing.core.ui

data class BAction(
    var type: ActionType,
    var delay: Int? = null,
    var result: Map<String, Any>? = null,
    var uiInfo: BUIInfo? = null,
    var srcScreenId: String? = null,
    var screenId: String? = null,
    var droidGuardMap: Map<String, String> = emptyMap(),
    var actionContext: MutableList<ByteArray> = mutableListOf()
)

enum class ActionType {
    DELAY,
    SHOW,
    UNKNOWN
}

data class BScreen(
    val uiInfo: BUIInfo? = null,
    val action: BAction? = null,
    val uiComponents: BUIComponents? = null
)

data class BUIInfo(
    val uiType: UIType
)

data class BAcquireResult(
    val action: BAction? = null,
    val screenMap: Map<String, BScreen> = emptyMap()
)

data class BUIComponents(
    val headerComponents: List<BComponent>,
    val contentComponents: List<BComponent>,
    val footerComponents: List<BComponent>
)

data class BComponent(
    val tag: String? = null,
    val uiInfo: BUIInfo? = null,
    val viewInfo: BViewInfo? = null,
    val viewType: ViewType,
    val clickableTextView: BClickableTextView? = null,
    val viewGroup: BViewGroup? = null,
    val dividerView: BDividerView? = null,
    val moduloImageView: BModuloImageView? = null,
    val iconTextCombinationView: BIconTextCombinationView? = null,
    val buttonGroupView: BButtonGroupView? = null,
    val instrumentItemView: BInstrumentItemView? = null
)

data class BClickableTextView(val playTextView: BPlayTextView? = null)

data class BViewGroup(
    val imageView1: BImageView? = null,
    val imageView2: BImageView? = null,
    val imageView3: BImageView? = null,
    val imageView4: BImageView? = null,
    val playTextView: BPlayTextView? = null
)

data class BImageGroup(
    val imageViews: List<BImageView>,
    val viewInfo: BViewInfo? = null
)

class BDividerView

data class BModuloImageView(
    val imageView: BImageView? = null
)

data class BIconTextCombinationView(
    val headerImageView: BImageView? = null,
    val playTextView: BPlayTextView? = null,
    val badgeTextView: BPlayTextView? = null,
    val middleTextViewList: List<BSingleLineTextView>? = null,
    val footerImageGroup: BImageGroup? = null,
    val viewInfo: BViewInfo? = null
)

data class BSingleLineTextView(
    val playTextView1: BPlayTextView? = null,
    val playTextView2: BPlayTextView? = null
)

data class BPlayTextView(
    val text: String,
    val isHtml: Boolean = true,
    val textInfo: BTextInfo? = null,
    val viewInfo: BViewInfo? = null,
    val textSpan: List<BTextSpan>,
)

data class BBulletSpan(
    val gapWidth: Int
)

data class BImageInfo(
    val colorFilterValue: Int? = null,
    val colorFilterType: Int? = null,
    val filterMode: Int? = null,
    val scaleType: Int? = null
)

data class BTextSpan(
    val textSpanType: TextSpanType,
    val bulletSpan: BBulletSpan? = null,
)

data class BImageView(
    val viewInfo: BViewInfo? = null,
    val imageInfo: BImageInfo? = null,
    val lightUrl: String? = null,
    val darkUrl: String? = null,
    val animation: BAnimation? = null,
    val iconView: BIconView? = null
)

data class BAnimation(
    val type: Int?,
    val repeatCount: Int?
)

data class BIconView(
    val type: Int?,
    val text: String?
)

data class BTextInfo(
    val colorType: ColorType? = null,
    val maxLines: Int? = null,
    val gravityList: List<BGravity>? = null,
    val textAlignmentType: TextAlignmentType? = null,
    val styleType: Int? = null
)

data class BButtonGroupView(
    val buttonViewList: List<BButtonView>
)

data class BInstrumentItemView(
    val icon: BImageView? = null,
    val text: BPlayTextView? = null,
    val tips: BPlayTextView? = null,
    val extraInfo: BPlayTextView? = null,
    val state: BImageView? = null,
    val action: BAction? = null,
)

data class BButtonView(
    val text: String,
    val viewInfo: BViewInfo? = null,
    val action: BAction? = null
)

data class BViewInfo(
    val tag: String? = null,
    val width: Float? = null,
    val height: Float? = null,
    val startMargin: Float? = null,
    val topMargin: Float? = null,
    val endMargin: Float? = null,
    val bottomMargin: Float? = null,
    val startPadding: Float? = null,
    val topPadding: Float? = null,
    val endPadding: Float? = null,
    val bottomPadding: Float? = null,
    val contentDescription: String? = null,
    val gravityList: List<BGravity>? = null,
    val backgroundColorType: ColorType? = null,
    val borderColorType: ColorType? = null,
    val action: BAction? = null,
    val visibilityType: Int? = null
)

enum class TextAlignmentType(val value: Int) {
    TEXT_ALIGNMENT_INHERIT(0),
    TEXT_ALIGNMENT_GRAVITY(1),
    TEXT_ALIGNMENT_CENTER(4),
    TEXT_ALIGNMENT_TEXT_START(2),
    TEXT_ALIGNMENT_TEXT_END(3),
    TEXT_ALIGNMENT_VIEW_START(5),
    TEXT_ALIGNMENT_VIEW_END(6)
}

enum class ColorType(val value: Int) {
    PHONESKY_SEMANTIC_COLOR_NAME_UNKNOWN(0),
    BACKGROUND_PRIMARY(1),
    BACKGROUND_SECONDARY(2),
    APPS_PRIMARY(3),
    APPS_2(4),
    APPS_3(5),
    BOOKS_PRIMARY(6),
    BOOKS_2(7),
    BOOKS_3(8),
    MOVIES_PRIMARY(9),
    MOVIES_2(10),
    MOVIES_3(11),
    MUSIC_PRIMARY(12),
    MUSIC_2(13),
    MUSIC_3(14),
    ENTERPRISE_PRIMARY(15),
    ENTERPRISE_2(16),
    ENTERPRISE_3(17),
    TEXT_PRIMARY(18),
    TEXT_SECONDARY(19),
    TEXT_TERTIARY(20),
    ICON_DEFAULT(21),
    ICON_HIGH_CONTRAST(22),
    PRIMARY_BUTTON_LABEL(23),
    PRIMARY_BUTTON_LABEL_DISABLED(24),
    PRIMARY_BUTTON_FILL_DISABLED(25),
    SECONDARY_BUTTON_FILL_DISABLED(26),
    CHIP_TEXT_SECONDARY(27),
    HAIR_LINE(28),
    ERROR_COLOR_PRIMARY(29),
    ERROR_COLOR_SECONDARY(30),
    BLANK_UNIFORM_ICON(31),
    PROGRESS_BAR_BACKGROUND(32),
    BRONZE_PRIMARY(33),
    BRONZE_2(34),
    BRONZE_3(35),
    SILVER_PRIMARY(36),
    SILVER_2(37),
    SILVER_3(38),
    GOLD_PRIMARY(39),
    GOLD_2(40),
    GOLD_3(41),
    PLATINUM_PRIMARY(42),
    PLATINUM_2(43),
    PLATINUM_3(44),
    DIAMOND_PRIMARY(45),
    DIAMOND_2(46),
    DIAMOND_3(0x2F),
    NEWS_PRIMARY(0x30),
    BACKGROUND_PRIMARY_INVERSE(49),
    TEXT_PRIMARY_DISABLED(50),
    TEXT_SECONDARY_DISABLED(51),
    RATING_STAR_OUTLINE(52),
    TOOLTIP_BACKGROUND(53),
    YOUTUBE_COMMERCE_NEUTRAL_BLUE(54),
    YOUTUBE_COMMERCE_NEUTRAL_BLUE_2(62),
    YOUTUBE_COMMERCE_NEUTRAL_BLUE_3(0x3F),
    YOUTUBE_COMMERCE_NEUTRAL_BLUE_RIPPLE(55),
    YOUTUBE_COMMERCE_DEEP_PURPLE(56),
    YOUTUBE_COMMERCE_DEEP_PURPLE_2(0x40),
    YOUTUBE_COMMERCE_DEEP_PURPLE_3(65),
    YOUTUBE_COMMERCE_DEEP_PURPLE_RIPPLE(57),
    YOUTUBE_COMMERCE_MAGENTA(58),
    YOUTUBE_COMMERCE_MAGENTA_2(66),
    YOUTUBE_COMMERCE_MAGENTA_3(67),
    YOUTUBE_COMMERCE_MAGENTA_RIPPLE(59),
    YOUTUBE_COMMERCE_TEAL(60),
    YOUTUBE_COMMERCE_TEAL_2(68),
    YOUTUBE_COMMERCE_TEAL_3(69),
    YOUTUBE_COMMERCE_TEAL_RIPPLE(61)
}

enum class BGravity(val value: Int) {
    NO_GRAVITY(0),
    BOTTOM(80),
    CENTER(17),
    CENTER_HORIZONTAL(1),
    CENTER_VERTICAL(16),
    CLIP_HORIZONTAL(8),
    CLIP_VERTICAL(0x80),
    END(0x800005),
    FILL(0x77),
    FILL_HORIZONTAL(7),
    FILL_VERTICAL(0x70),
    LEFT(0x3),
    RIGHT(0x5),
    START(0x800003),
    TOP(0x30)
}

enum class ViewType {
    CLICKABLETEXTVIEW,
    VIEWGROUP,
    DIVIDERVIEW,
    MODULOIMAGEVIEW,
    ICONTEXTCOMBINATIONVIEW,
    BUTTONGROUPVIEW,
    INSTRUMENTITEMVIEW,
    UNKNOWNVIEW
}


enum class TextSpanType {
    BULLETSPAN,
    UNKNOWNSPAN
}

enum class AnimationType(val value: Int) {
    CHECK_MARK(21),
    UNKNOWN(0)
}