package org.microg.gms.maps.mapbox

import android.graphics.Color
import android.os.Build.VERSION.SDK_INT
import android.util.Log
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.core.graphics.ColorUtils
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.annotations.SerializedName
import com.mapbox.mapboxsdk.maps.Style
import org.json.JSONArray
import org.json.JSONObject
import org.microg.gms.maps.mapbox.utils.MapContext
import java.io.File
import java.io.IOException
import java.lang.NumberFormatException
import kotlin.math.pow
import kotlin.math.roundToInt

const val TAG = "GmsMapStyles"
const val KEY_METADATA_FEATURE_TYPE = "microg:gms-type-feature"
const val KEY_METADATA_ELEMENT_TYPE = "microg:gms-type-element"

const val SELECTOR_ALL = "all"
const val SELECTOR_ELEMENT_LABEL_TEXT_FILL = "labels.text.fill"
const val SELECTOR_ELEMENT_LABEL_TEXT_OUTLINE = "labels.text.outline"
const val KEY_LAYER_METADATA = "metadata"
const val KEY_LAYER_PAINT = "paint"


fun getStyle(
    context: MapContext, mapType: Int, styleOptions: MapStyleOptions?, styleFromFileWorkaround: Boolean = false
): Style.Builder {

    // TODO: Serve map style resources locally
    val styleJson = JSONObject(
        context.assets.open(
            when (mapType) {
                GoogleMap.MAP_TYPE_SATELLITE, GoogleMap.MAP_TYPE_HYBRID -> "style-microg-satellite.json"
                GoogleMap.MAP_TYPE_TERRAIN -> "style-mapbox-outdoors-v12.json"
                //MAP_TYPE_NONE, MAP_TYPE_NORMAL,
                else -> "style-microg-normal.json"
            }
        ).bufferedReader().readText()
    )

    styleOptions?.apply(styleJson)

    return if (styleFromFileWorkaround) {
        val temporaryFile = File(context.cacheDir, styleJson.hashCode().toString())

        if (!temporaryFile.exists()) {
            temporaryFile.createNewFile()
        }

        try {
            temporaryFile.bufferedWriter().use {
                it.write(styleJson.toString())
            }
            Log.d(TAG, "file:/${temporaryFile.absolutePath}")
            Style.Builder().fromUri("file:/${temporaryFile.absolutePath}")
        } catch (e: IOException) {
            e.printStackTrace()
            Style.Builder().fromUri(getFallbackStyleOnlineUri(mapType))
        }
    } else {
        Style.Builder().fromJson(styleJson.toString())
    }
}


fun getFallbackStyleOnlineUri(mapType: Int) = when (mapType) {
    GoogleMap.MAP_TYPE_SATELLITE -> "mapbox://styles/microg/cjxgloted25ap1ct4uex7m6hi"
    GoogleMap.MAP_TYPE_TERRAIN -> "mapbox://styles/mapbox/outdoors-v12"
    GoogleMap.MAP_TYPE_HYBRID -> "mapbox://styles/microg/cjxgloted25ap1ct4uex7m6hi"
    //MAP_TYPE_NONE, MAP_TYPE_NORMAL,
    else -> "mapbox://styles/microg/cjui4020201oo1fmca7yuwbor"
}

fun MapStyleOptions.apply(style: JSONObject) {
    try {
        Gson().fromJson(json, Array<StyleOperation>::class.java).let { styleOperations ->

            val layerArray = style.getJSONArray("layers")

            // Apply operations in order
            operations@ for (operation in styleOperations.map { it.toNonNull() }) {

                // Reverse direction allows removing hidden layers
                layers@ for (i in layerArray.length() - 1 downTo 0) {

                    val layer = layerArray.getJSONObject(i)
                    if (layer.layerHasRequiredFields()) {

                        if (operation.isValid() && layer.matchesOperation(operation)) {
                            Log.v(TAG, "applying ${Gson().toJson(operation)} to $layer")

                            if (layer.layerShouldBeRemoved(operation)) {
                                Log.v(TAG, "removing $layer")
                                layerArray.removeCompat(i)
                            } else {
                                layer.applyOperation(operation)
                            }
                        }
                    }
                }
            }
        }


    } catch (e: JsonSyntaxException) {
        e.printStackTrace()
    }
}

data class StyleOperation(val featureType: String?, val elementType: String?, val stylers: Array<Styler>?)

data class NonNullStyleOperation(val featureType: String, val elementType: String, val stylers: Array<Styler>)

class Styler(
    val hue: String?,
    @FloatRange(from = -100.0, to = 100.0) val saturation: Float?,
    @FloatRange(from = -100.0, to = 100.0) val lightness: Float?,
    @FloatRange(from = 0.01, to = 10.0) val gamma: Float?,
    @SerializedName("invert_lightness") val invertLightness: Boolean?,
    val visibility: String?,
    val color: String?,
    //val weight: Int?
)

/**
 * Constructs a `NonNullStyleOperation` out of the `StyleOperation` while filling null fields with
 * default values.
 */
fun StyleOperation.toNonNull() =
    NonNullStyleOperation(featureType ?: SELECTOR_ALL, elementType ?: SELECTOR_ALL, stylers ?: emptyArray())

/**
 * Returns false iff the operation is invalid.
 *
 * There is one invalid selector that is tested for – per docs:
 * "`administrative` selects all administrative areas. Styling affects only
 * the labels of administrative areas, not the geographical borders or fill."
 */
fun NonNullStyleOperation.isValid() = !(featureType.startsWith("administrative") &&
        elementType.startsWith("geometry"))

/**
 * True iff the layer represented by the JSON object should be modified according to the stylers in the operation.
 *
 * Layer metadata always has the most concrete category, while operation applies to all subcategories as well.
 * Therefore, we test if the operation is a substring of the layer's metadata – i.e. the layer's metadata contains
 * (more concretely: starts with) the operation's selector.
 */
fun JSONObject.matchesOperation(operation: NonNullStyleOperation) =
    (getJSONObject(KEY_LAYER_METADATA).getString(KEY_METADATA_FEATURE_TYPE).startsWith(operation.featureType)
            || operation.featureType == "all")
    && (getJSONObject(KEY_LAYER_METADATA).getString(KEY_METADATA_ELEMENT_TYPE).startsWith(operation.elementType)
            || operation.elementType == "all")


/**
 * Layer has fields that allow applying style operations.
 */
fun JSONObject.layerHasRequiredFields() = has(KEY_LAYER_PAINT) && has(KEY_LAYER_METADATA) &&
        getJSONObject(KEY_LAYER_METADATA).let { it.has(KEY_METADATA_FEATURE_TYPE) && it.has(KEY_METADATA_ELEMENT_TYPE) }

/**
 * True iff the layer represented by the JSON object should be removed according to the provided style operation.
 *
 * Interpretation of visibility "simplified": hide labels, display geometry.
 */
fun JSONObject.layerShouldBeRemoved(operation: NonNullStyleOperation) =
    // A styler sets the layer to be invisible
    operation.stylers.any { it.visibility == "off" } ||
        // A styler sets the layer to simplified and we are working with a label
        (getJSONObject("metadata").getString(KEY_METADATA_ELEMENT_TYPE)
                .startsWith("labels") && operation.stylers.any { it.visibility == "simplified" })

/**
 * Applies the provided style operation to the layer represented by the JSON object.
 */
fun JSONObject.applyOperation(operation: NonNullStyleOperation) = operation.stylers.forEach { styler ->
    when (operation.elementType) {
        SELECTOR_ELEMENT_LABEL_TEXT_FILL -> styler.applyTextFill(getJSONObject(KEY_LAYER_PAINT))
        SELECTOR_ELEMENT_LABEL_TEXT_OUTLINE -> styler.applyTextOutline(getJSONObject(KEY_LAYER_PAINT))
        else -> styler.traverse(getJSONObject(KEY_LAYER_PAINT))
    }
}

/**
 * Returns true if string is likely to contain a color.
 */
fun String.isColor() = startsWith("hsl(") || startsWith("hsla(") || startsWith("#") || startsWith("rgba(")

/**
 * Can parse colors in the format '#rrggbb', '#aarrggbb', 'hsl(h, s, l)', and 'rgba(r, g, b, a)'
 * Returns 0 and prints to log if an invalid color is provided.
 */
@ColorInt
fun String.parseColor(): Int {
    if (startsWith("#") && length in listOf(7, 9)) {
        return Color.parseColor(this)
    } else if (startsWith("hsl(")) {
        val hslArray = replace("hsl(", "").replace(")", "").split(", ")
        if (hslArray.size != 3) {
            Log.w(TAG, "Invalid color `$this`")
            return 0
        }

        return try {
            ColorUtils.HSLToColor(
                floatArrayOf(
                    hslArray[0].toFloat(),
                    hslArray[1].parseFloat(),
                    hslArray[2].parseFloat()
                )
            )
        } catch (e: NumberFormatException) {
            Log.w(TAG, "Invalid color `$this`")
            0
        }
    } else if (startsWith("hsla(")) {
        val hslArray = replace("hsla(", "").replace(")", "").split(", ")
        if (hslArray.size != 4) {
            Log.w(TAG, "Invalid color `$this`")
            return 0
        }

        return try {
            ColorUtils.setAlphaComponent(
                ColorUtils.HSLToColor(
                    floatArrayOf(
                        hslArray[0].toFloat(), hslArray[1].parseFloat(), hslArray[2].parseFloat()
                    )
                ), (hslArray[3].parseFloat() * 255).roundToInt()
            )
        } catch (e: NumberFormatException) {
            Log.w(TAG, "Invalid color `$this`")
            0
        }

    } else if (startsWith("rgba(")) {
        return com.mapbox.mapboxsdk.utils.ColorUtils.rgbaToColor(this)
    }

    Log.w(TAG, "Invalid color `$this`")
    return 0
}

/**
 * Formats color int in such a format that it MapLibre's rendering engine understands it.
 */
fun Int.colorToString() = com.mapbox.mapboxsdk.utils.ColorUtils.colorToRgbaString(this)

/**
 * Can parse string values that contain '%'.
 */
fun String.parseFloat(): Float {
    return if (contains("%")) {
        replace("%", "").toFloat() / 100f
    } else {
        toFloat()
    }
}

/**
 * Applies operation specified by styler to the provided color int, and returns
 * a new, corresponding color int.
 */
@ColorInt
fun Styler.applyColorChanges(color: Int): Int {
    // There may only be one operation per styler per docs.

    hue?.let { hue ->
        // Extract hue from input color
        val hslResult = FloatArray(3)
        ColorUtils.colorToHSL(hue.parseColor(), hslResult)

        val hueDegree = hslResult[0]

        // Apply hue to layer color
        ColorUtils.colorToHSL(color, hslResult)
        hslResult[0] = hueDegree
        return ColorUtils.HSLToColor(hslResult)
    }

    lightness?.let { lightness ->
        // Apply lightness to layer color
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(color, hsl)
        hsl[2] = if (lightness < 0) {
            // Increase darkness. Percentage amount = relative reduction of is-lightness.
            (lightness / 100 + 1) * hsl[2]
        } else {
            // Increase brightness. Percentage amount = relative reduction of difference between is-lightness and 1.0.
            hsl[2] + (lightness / 100) * (1 - hsl[2])
        }
        return ColorUtils.HSLToColor(hsl)
    }

    saturation?.let { saturation ->
        // Apply saturation to layer color
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(color, hsl)
        hsl[1] = if (saturation < 0) {
            // Reduce intensity. Percentage amount = relative reduction of is-saturation.
            (saturation / 100 + 1) * hsl[1]
        } else {
            // Increase intensity. Percentage amount = relative reduction of difference between is-saturation and 1.0.
            hsl[1] + (saturation / 100) * (1 - hsl[1])
        }

        return ColorUtils.HSLToColor(hsl)
    }

    gamma?.let { gamma ->
        // Apply gamma to layer color
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(color, hsl)
        hsl[2] = hsl[2].toDouble().pow(gamma.toDouble()).toFloat()

        return ColorUtils.HSLToColor(hsl)
    }

    if (invertLightness == true) {
        // Invert layer color's lightness
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(color, hsl)
        hsl[2] = 1 - hsl[2]

        return ColorUtils.HSLToColor(hsl)
    }

    this.color?.let {
        return it.parseColor()
    }

    Log.w(TAG, "No applicable operation")
    return color
}

/**
 * Traverse JSON object and replace any color strings according to styler
 */
fun Styler.traverse(json: JSONObject) {
    // Traverse layer and replace any color strings
    json.keys().forEach { key ->
        json.get(key).let {
            when (it) {
                is JSONObject -> traverse(it)
                is JSONArray -> traverse(it)
                is String -> if (it.isColor()) {
                    json.put(key, applyColorChanges(it.parseColor()).colorToString())
                }
            }
        }
    }
}

/**
 * Traverse array and replace any color strings according to styler
 */
fun Styler.traverse(array: JSONArray) {
    for (i in 0 until array.length()) {
        array.get(i).let {
            when (it) {
                is JSONObject -> traverse(it)
                is JSONArray -> traverse(it)
                is String -> if (it.isColor()) {
                    array.put(i, applyColorChanges(it.parseColor()).colorToString())
                }
            }
        }
    }
}

fun Styler.applyTextFill(paint: JSONObject) {
    if (paint.has("text-color")) when (val textColor = paint.get("text-color")) {
        is JSONObject -> traverse(textColor)
        is JSONArray -> traverse(textColor)
        is String -> paint.put("text-color", applyColorChanges(textColor.parseColor()).colorToString())
    }
}

fun Styler.applyTextOutline(paint: JSONObject) {
    if (paint.has("text-halo-color")) when (val textOutline = paint.get("text-halo-color")) {
        is JSONObject -> traverse(textOutline)
        is JSONArray -> traverse(textOutline)
        is String -> paint.put("text-halo-color", applyColorChanges(textOutline.parseColor()).colorToString())
    }
}

fun JSONArray.removeCompat(index: Int) =
    if (SDK_INT >= 19) {
        remove(index)
        this
    } else {
        val field = JSONArray::class.java.getDeclaredField("values")
        field.isAccessible = true
        val list = field.get(this) as MutableList<*>
        list.removeAt(index)
        this
    }