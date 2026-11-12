package org.microg.gms.ui

import android.content.Context
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.google.android.gms.wearable.ConnectionConfiguration

class WearConfigPreference: Preference {
    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context) : super(context)

    init {
        isPersistent = false
    }

    private var configField: ConnectionConfiguration? = null

    var connectionConfig: ConnectionConfiguration?
        get() = configField
        set(value) {
            if (value == null && configField != null) {
                title = null
                icon = null
            } else if (value != null) {
                val pm = context.packageManager
                val applicationInfo = pm.getApplicationInfoIfExists(value.packageName)

                title = value.name
                summary = value.packageName
                icon = applicationInfo?.loadIcon(pm) ?: AppCompatResources.getDrawable(context, android.R.mipmap.sym_def_app_icon)
            }
            configField = value
        }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val icon = holder.findViewById(android.R.id.icon)
        if (icon is ImageView) {
            icon.adjustViewBounds = true
            icon.scaleType = ImageView.ScaleType.CENTER_INSIDE
            icon.maxHeight = (32.0 * context.resources.displayMetrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT).toInt()
        }
    }
}