/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.accountsettings.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.R
import com.google.android.gms.common.images.ImageManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import org.microg.gms.accountsettings.AccountSearchClient
import org.microg.gms.accountsettings.AccountSearchItem
import org.microg.gms.accountsettings.AccountSearchResponse
import org.microg.gms.accountsettings.NATIVE_ACTION_GOOGLE_HELP
import androidx.core.net.toUri

private const val TAG = "AccountSearch"
private const val KEY_SEARCH_QUERY = "query"
private const val KEY_SEARCH_RESPONSE = "response"
private const val KEY_SCROLL_Y = "scrollY"
private const val SEARCH_DEBOUNCE_DELAY_MS = 300L
private const val MAX_SAVED_RESPONSE_BYTES = 256 * 1024

class AccountSearchFragment : Fragment() {
    companion object {
        fun newInstance(accountName: String?, callingPackageName: String) = AccountSearchFragment().apply {
            arguments = Bundle().apply {
                putString(EXTRA_ACCOUNT_NAME, accountName)
                putString(EXTRA_CALLING_PACKAGE_NAME, callingPackageName)
            }
        }
    }

    private var query = ""
    private var response: AccountSearchResponse? = null
    private var searchJob: Job? = null
    private var searchView: AppCompatEditText? = null
    private var progressBar: ProgressBar? = null
    private var statusView: TextView? = null
    private var retryButton: Button? = null
    private var resultsView: LinearLayout? = null
    private var scrollView: ScrollView? = null

    private val backgroundColor get() = if (requireContext().isNightMode()) 0xff1b1b1f.toInt() else 0xffeaedf5.toInt()
    private val textColor get() = if (requireContext().isNightMode()) 0xffe3e3e3.toInt() else 0xff1f1f1f.toInt()
    private val secondaryTextColor get() = if (requireContext().isNightMode()) 0xffc4c7c5.toInt() else 0xff444746.toInt()
    private val accentColor get() = if (requireContext().isNightMode()) 0xffa8c7fa.toInt() else 0xff0b57d0.toInt()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        query = savedInstanceState?.getString(KEY_SEARCH_QUERY) ?: query
        response = savedInstanceState?.getByteArray(KEY_SEARCH_RESPONSE)?.let {
            runCatching { AccountSearchResponse.ADAPTER.decode(it) }.getOrNull()
        } ?: response
        setupSearchView()
        if (response == null && query.isNotBlank()) search(query, true) else renderResults()
        val scrollY = savedInstanceState?.getInt(KEY_SCROLL_Y) ?: 0
        scrollView?.post { scrollView?.scrollTo(0, scrollY) }
        if (savedInstanceState == null && query.isEmpty()) searchView?.post {
            searchView?.let {
                it.requestFocus()
                (it.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                    .showSoftInput(it, InputMethodManager.SHOW_IMPLICIT)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val context = requireContext()
        requireActivity().setTitle(R.string.account_settings_search_title)
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(backgroundColor)
        }
        if (SDK_INT >= 21) {
            requireActivity().window.apply {
                statusBarColor = if (SDK_INT >= 23 || context.isNightMode()) backgroundColor else Color.DKGRAY
                navigationBarColor = if (SDK_INT >= 26 || context.isNightMode()) backgroundColor else Color.BLACK
                WindowCompat.getInsetsController(this, layout).apply {
                    isAppearanceLightStatusBars = !context.isNightMode()
                    isAppearanceLightNavigationBars = !context.isNightMode()
                }
            }
        }
        val toolbar = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL }
        toolbar.addView(createToolbarButton(R.drawable.ic_arrow_back, androidx.appcompat.R.string.abc_action_bar_up_description) {
            requireActivity().finish()
        }, LinearLayout.LayoutParams(dp(48), MATCH_PARENT))
        searchView = AppCompatEditText(context).apply {
            setHint(R.string.account_settings_search_hint)
            setSingleLine()
            inputType = InputType.TYPE_CLASS_TEXT
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            textSize = 16f
            setTextColor(textColor)
            setHintTextColor(secondaryTextColor)
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            setPaddingRelative(dp(4), 0, dp(4), 0)
            background = null
            if (SDK_INT >= 29) textCursorDrawable = GradientDrawable().apply {
                setColor(accentColor)
                setSize(dp(2), dp(24))
            }
        }
        toolbar.addView(searchView, LinearLayout.LayoutParams(0, MATCH_PARENT, 1f))
        toolbar.addView(createToolbarButton(androidx.appcompat.R.drawable.abc_ic_clear_material, androidx.appcompat.R.string.abc_searchview_description_clear) {
            searchView?.text?.clear()
        }, LinearLayout.LayoutParams(dp(48), MATCH_PARENT))
        progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = false
            progress = max
            if (SDK_INT >= 21) {
                progressTintList = ColorStateList.valueOf(accentColor)
                indeterminateTintList = progressTintList
            }
        }
        layout.addView(FrameLayout(context).apply {
            addView(toolbar, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
            addView(progressBar, FrameLayout.LayoutParams(MATCH_PARENT, dp(2), Gravity.BOTTOM))
        }, LinearLayout.LayoutParams(MATCH_PARENT, dp(56)))
        statusView = TextView(context).apply {
            setTextColor(secondaryTextColor)
            textSize = 14f
            setPadding(dp(24), dp(24), dp(24), dp(24))
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
        layout.addView(statusView, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        retryButton = Button(context).apply {
            setText(R.string.account_settings_search_retry)
            isVisible = false
            setOnClickListener { search(query, true) }
        }
        layout.addView(retryButton, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        resultsView = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), 0, dp(16), dp(24))
        }
        scrollView = ScrollView(context).apply {
            isFillViewport = true
            addView(resultsView)
        }
        layout.addView(scrollView, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
        return layout
    }

    private fun createToolbarButton(icon: Int, description: Int, onClick: () -> Unit) =
        ImageButton(requireContext(), null, android.R.attr.borderlessButtonStyle).apply {
            setImageDrawable(ContextCompat.getDrawable(context, icon)?.mutate()?.also { DrawableCompat.setTint(it, textColor) })
            contentDescription = getString(description)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setOnClickListener { onClick() }
        }

    private fun setupSearchView() {
        searchView?.setText(query)
        searchView?.setSelection(query.length)
        searchView?.doAfterTextChanged {
            val text = it?.toString().orEmpty()
            if (text != query) search(text)
        }
        searchView?.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                (requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                    .hideSoftInputFromWindow(searchView?.windowToken, 0)
                search(query, true)
                true
            } else {
                false
            }
        }
    }

    private fun renderResults() {
        val results = resultsView ?: return
        val context = requireContext()
        results.removeAllViews()
        val sections = response?.sections.orEmpty().filter { it.items.isNotEmpty() || it.footer != null }
        statusView?.isVisible = sections.isEmpty() && query.isNotBlank()
        statusView?.setText(R.string.account_settings_search_empty)
        for (section in sections) {
            if (!section.title.isNullOrBlank()) results.addView(TextView(context).apply {
                text = section.title
                textSize = 16f
                setTextColor(textColor)
                typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                includeFontPadding = false
                setPadding(dp(8), dp(20), dp(8), dp(20))
                ViewCompat.setAccessibilityHeading(this, true)
            })
            val items = section.items + listOfNotNull(section.footer)
            for ((index, item) in items.withIndex()) {
                results.addView(createResultView(item, index == 0, index == items.lastIndex, index == section.items.size),
                    LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { if (index > 0) topMargin = dp(2) })
            }
        }
    }

    private fun createResultView(item: AccountSearchItem, first: Boolean, last: Boolean, footer: Boolean): View {
        val context = requireContext()
        val row = LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(if (footer) 56 else 64)
            setPaddingRelative(dp(16), dp(12), dp(16), dp(12))
            val topRadius = dp(if (first) 20 else 4).toFloat()
            val bottomRadius = dp(if (last) 20 else 4).toFloat()
            fun shape(color: Int) = GradientDrawable().apply {
                setColor(color)
                cornerRadii = floatArrayOf(topRadius, topRadius, topRadius, topRadius,
                    bottomRadius, bottomRadius, bottomRadius, bottomRadius)
            }
            background = StateListDrawable().apply {
                val pressed = shape(if (context.isNightMode()) 0xff3c4043.toInt() else 0xffdce3ef.toInt())
                addState(intArrayOf(android.R.attr.state_pressed), pressed)
                addState(intArrayOf(android.R.attr.state_focused), pressed)
                addState(intArrayOf(), shape(if (context.isNightMode()) 0xff28292a.toInt() else Color.WHITE))
            }
            isFocusable = true
            contentDescription = item.navigation?.ariaLabel?.takeIf(String::isNotBlank)
                ?: listOfNotNull(item.title, item.description?.takeIf(String::isNotBlank)).joinToString(", ")
            setOnClickListener { openResult(item) }
        }
        if (!footer) row.addView(ImageView(context).apply {
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            val horizontalPadding = dp((40 - (item.image?.widthDp ?: 40).coerceIn(1, 40)) / 2)
            val verticalPadding = dp((40 - (item.image?.heightDp ?: 40).coerceIn(1, 40)) / 2)
            setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
            val url = item.image?.let { context.getThemedUrl(it.url, it.themeUrls) }
            if (url != null && url.toUri().run { scheme == "https" && !host.isNullOrBlank() && userInfo == null }) {
                ImageManager.create(context).loadImage(url, this)
            }
        }, LinearLayout.LayoutParams(dp(40), dp(40)).apply { marginEnd = dp(12) })
        row.addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
            addView(TextView(context).apply {
                text = item.title
                textSize = 16f
                setTextColor(textColor)
                typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                includeFontPadding = false
                minHeight = dp(24)
                gravity = Gravity.CENTER_VERTICAL
            })
            if (!item.description.isNullOrBlank()) addView(TextView(context).apply {
                text = item.description
                textSize = 14f
                setTextColor(secondaryTextColor)
                includeFontPadding = false
                minHeight = dp(20)
                gravity = Gravity.CENTER_VERTICAL
            })
        }, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        return row
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun openResult(item: AccountSearchItem) {
        searchView?.clearFocus()
        val key = item.navigation?.resourceKey ?: item.resourceKey
        val context = requireContext()
        val navigation = key?.let { response?.additionalResources?.firstOrNull { it.key == key } }
            ?.content?.items.orEmpty().firstNotNullOfOrNull { target ->
                target.nativeAction?.takeIf { it.type == NATIVE_ACTION_GOOGLE_HELP }?.help
                    ?.let(context::getHelpUrl)?.takeIf(String::isBrowsableWebUrl)?.let { it to true }
                    ?: target.browser?.let { context.getThemedUrl(it.url, it.themeUrls) }
                        ?.takeIf(String::isBrowsableWebUrl)?.let { it to true }
                    ?: target.redirect?.url?.takeIf(String::isBrowsableWebUrl)?.let { it to false }
                    ?: target.webView?.let { context.getThemedUrl(it.url, it.themeUrls) }
                        ?.takeIf(String::isAllowedWebUrl)?.let { it to false }
            }
        parentFragmentManager.setFragmentResult(REQUEST_ACCOUNT_SEARCH_NAVIGATION, Bundle().apply {
            if (navigation != null) {
                putString(EXTRA_URL, navigation.first)
                putBoolean(EXTRA_OPEN_EXTERNALLY, navigation.second)
            } else {
                putInt(EXTRA_SCREEN_ID, key?.screenId ?: 0)
                key?.parameters?.forEach { (name, value) -> putString(EXTRA_SCREEN_OPTIONS_PREFIX + name, value) }
            }
        })
    }

    private fun search(text: String, immediately: Boolean = false) {
        searchJob?.cancel()
        query = text
        response = null
        retryButton?.isVisible = false
        progressBar?.isIndeterminate = false
        renderResults()
        if (text.isBlank()) return
        val accountName = requireArguments().getString(EXTRA_ACCOUNT_NAME)
        if (accountName == null) {
            statusView?.setText(R.string.account_settings_search_error)
            return
        }
        val context = requireContext().applicationContext
        val callingPackageName = requireArguments().getString(EXTRA_CALLING_PACKAGE_NAME).orEmpty()
        statusView?.isVisible = false
        progressBar?.isIndeterminate = true
        searchJob = viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            try {
                if (!immediately) delay(SEARCH_DEBOUNCE_DELAY_MS)
                response = AccountSearchClient.searchAccountSettings(context, accountName, callingPackageName, text)
                renderResults()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Account search failed: ${e.javaClass.simpleName}")
                statusView?.setText(R.string.account_settings_search_error)
                statusView?.isVisible = true
                retryButton?.isVisible = true
            }
            progressBar?.isIndeterminate = false
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_SEARCH_QUERY, query)
        response?.encode()?.takeIf { it.size <= MAX_SAVED_RESPONSE_BYTES }?.let { outState.putByteArray(KEY_SEARCH_RESPONSE, it) }
        outState.putInt(KEY_SCROLL_Y, scrollView?.scrollY ?: 0)
    }

    override fun onDestroyView() {
        searchJob = null
        searchView = null
        progressBar = null
        statusView = null
        retryButton = null
        resultsView = null
        scrollView = null
        super.onDestroyView()
    }
}
