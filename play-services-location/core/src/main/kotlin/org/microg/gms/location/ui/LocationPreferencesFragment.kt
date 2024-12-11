/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.ui

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.location.LocationManager
import android.net.Uri
import android.os.*
import android.os.Build.VERSION.SDK_INT
import android.text.Html
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.*
import android.view.Menu.NONE
import android.widget.*
import androidx.core.content.getSystemService
import androidx.core.os.bundleOf
import androidx.core.view.setPadding
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.TwoStatePreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.microg.gms.location.*
import org.microg.gms.location.core.R
import org.microg.gms.location.manager.LocationAppsDatabase
import org.microg.gms.location.network.OnlineSource
import org.microg.gms.location.network.effectiveEndpoint
import org.microg.gms.location.network.onlineSource
import org.microg.gms.ui.AppIconPreference
import org.microg.gms.ui.buildAlertDialog
import org.microg.gms.ui.getApplicationInfoIfExists
import org.microg.gms.ui.navigate

private const val REQUEST_CODE_IMPORT_FILE = 5715515

class LocationPreferencesFragment : PreferenceFragmentCompat() {
    private lateinit var locationApps: PreferenceCategory
    private lateinit var locationAppsAll: Preference
    private lateinit var locationAppsNone: Preference
    private lateinit var networkProviderCategory: PreferenceCategory
    private lateinit var wifiIchnaea: TwoStatePreference
    private lateinit var wifiMoving: TwoStatePreference
    private lateinit var wifiLearning: TwoStatePreference
    private lateinit var cellIchnaea: TwoStatePreference
    private lateinit var cellLearning: TwoStatePreference
    private lateinit var nominatim: TwoStatePreference
    private lateinit var database: LocationAppsDatabase

    init {
        setHasOptionsMenu(true)
    }

    companion object {
        private const val MENU_ICHNAEA_URL = Menu.FIRST
        private const val MENU_IMPORT_EXPORT = Menu.FIRST + 1
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (requireContext().hasNetworkLocationServiceBuiltIn()) {
            menu.add(NONE, MENU_ICHNAEA_URL, NONE, R.string.pref_location_source_title)
            menu.add(NONE, MENU_IMPORT_EXPORT, NONE, R.string.pref_location_import_export_title)
        }
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == MENU_ICHNAEA_URL) {
            openOnlineSourceSelector()
            return true
        }
        if (item.itemId == MENU_IMPORT_EXPORT) {
            openImportExportDialog()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private val messenger by lazy {
        Messenger(object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                try {
                    when (msg.data.getString(EXTRA_DIRECTION)) {
                        DIRECTION_EXPORT -> {
                            val name = msg.data.getString(EXTRA_NAME)
                            val fileUri = msg.data.getParcelable<Uri>(EXTRA_URI)
                            if (fileUri != null) {
                                val sendIntent: Intent = Intent(Intent.ACTION_SEND).apply {
                                    putExtra(Intent.EXTRA_STREAM, fileUri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    type = "application/vnd.microg.location.$name+csv+gzip"
                                }

                                startActivity(Intent.createChooser(sendIntent, null))
                            }
                            currentDialog?.dismiss()
                        }

                        DIRECTION_IMPORT -> {
                            val counter = msg.arg1
                            Toast.makeText(requireContext(), getString(R.string.location_data_import_result_toast, counter), Toast.LENGTH_SHORT).show()
                            currentDialog?.dismiss()
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, e)
                }
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_IMPORT_FILE) {
            if (resultCode == Activity.RESULT_OK && data?.data != null) {
                val intent = Intent(ACTION_NETWORK_IMPORT_EXPORT)
                intent.`package` = requireContext().packageName
                intent.putExtra(EXTRA_DIRECTION, DIRECTION_IMPORT)
                intent.putExtra(EXTRA_MESSENGER, messenger)
                intent.putExtra(EXTRA_URI, data.data)
                requireContext().startService(intent)
            } else {
                currentDialog?.dismiss()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private val Int.dp
        get() =  (this * resources.displayMetrics.density).toInt()

    private var currentDialog: Dialog? = null

    private fun openImportExportDialog() {
        val listView = ListView(requireContext()).apply {
            setPadding(8.dp, 16.dp, 8.dp, 16.dp)
            adapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_list_item_1).apply {
                add(requireContext().getString(R.string.location_data_export_wifi_title))
                add(requireContext().getString(R.string.location_data_export_cell_title))
                add(requireContext().getString(R.string.location_data_import_title))
            }
        }
        val progress = ProgressBar(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setPadding(20.dp)
            isIndeterminate = true
            visibility = View.GONE
        }
        val view = FrameLayout(requireContext()).apply {
            addView(listView)
            addView(progress)
        }
        currentDialog = requireContext().buildAlertDialog()
            .setTitle(R.string.pref_location_import_export_title)
            .setView(view)
            .show()
        listView.setOnItemClickListener { _, _, position, _ ->
            if (position == 0 || position == 1) {
                val intent = Intent(ACTION_NETWORK_IMPORT_EXPORT)
                intent.`package` = requireContext().packageName
                intent.putExtra(EXTRA_DIRECTION, DIRECTION_EXPORT)
                intent.putExtra(EXTRA_NAME, if (position == 0) NAME_WIFI else NAME_CELL)
                intent.putExtra(EXTRA_MESSENGER, messenger)
                requireContext().startService(intent)
            } else if (position == 2) {
                val openIntent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                }
                startActivityForResult(openIntent, REQUEST_CODE_IMPORT_FILE)
            }
            listView.visibility = View.INVISIBLE
            progress.visibility = View.VISIBLE
            currentDialog?.setCancelable(false)
        }
    }

    private fun openOnlineSourceSelector(callback: () -> Unit = {}) {
        val view = LinearLayout(requireContext())
        view.setPadding(0, 16.dp, 0, 0)
        view.orientation = LinearLayout.VERTICAL
        val settings = LocationSettings(requireContext())
        val currentSourceId = settings.onlineSource?.id
        val unselectHandlerMap = mutableMapOf<String, () -> Unit>()
        var selectedSourceId = currentSourceId
        val customView = layoutInflater.inflate(R.layout.preference_location_custom_url, null)
        customView.findViewById<EditText>(android.R.id.edit).setText(settings.customEndpoint)
        customView.visibility = View.GONE
        for (source in OnlineSource.ALL) {
            val title = when {
                source.name != null -> source.name
                source.id == OnlineSource.ID_CUSTOM -> getText(R.string.pref_location_custom_source_title)
                else -> source.id
            }
            val sourceDescription = source.host.takeIf { source.name != it }
            val sourceTerms = source.terms?.let { Html.fromHtml("<a href=\"${it}\">${getText(R.string.pref_location_source_terms)}</a>") }
            val description = when {
                sourceDescription != null && sourceTerms != null -> SpannableStringBuilder().append(sourceDescription).append(" · ").append(sourceTerms)
                sourceDescription != null -> sourceDescription
                sourceTerms != null -> sourceTerms
                else -> null
            }
            val subView = layoutInflater.inflate(R.layout.preference_location_online_source, null)
            subView.findViewById<TextView>(android.R.id.title).text = title
            if (description != null) {
                subView.findViewById<TextView>(android.R.id.text1).text = description
                if (sourceTerms != null) subView.findViewById<TextView>(android.R.id.text1).movementMethod = LinkMovementMethod.getInstance()
            } else {
                subView.findViewById<TextView>(android.R.id.text1).visibility = View.GONE
            }
            if (source.suggested) subView.findViewById<View>(R.id.suggested_tag).visibility = View.VISIBLE
            unselectHandlerMap[source.id] = {
                subView.findViewById<ImageView>(android.R.id.button1).setImageResource(org.microg.gms.base.core.R.drawable.ic_radio_unchecked)
                if (source.id == OnlineSource.ID_CUSTOM) customView.visibility = View.GONE
            }
            val selectedHandler = {
                for (entry in unselectHandlerMap) {
                    if (entry.key != source.id) {
                        entry.value.invoke()
                    }
                }
                if (source.id == OnlineSource.ID_CUSTOM) customView.visibility = View.VISIBLE
                subView.findViewById<ImageView>(android.R.id.button1).setImageResource(org.microg.gms.base.core.R.drawable.ic_radio_checked)
                selectedSourceId = source.id
            }
            if (currentSourceId == source.id) selectedHandler.invoke()
            subView.setOnClickListener { selectedHandler.invoke() }
            view.addView(subView)
        }
        view.addView(customView)

        requireContext().buildAlertDialog()
            .setTitle(R.string.pref_location_source_title)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                if (selectedSourceId == OnlineSource.ID_CUSTOM) {
                    settings.customEndpoint = customView.findViewById<EditText>(android.R.id.edit).text.toString()
                }
                settings.onlineSourceId = selectedSourceId
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->  }
            .setOnDismissListener { callback() }
            .setView(view)
            .show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = LocationAppsDatabase(requireContext())
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_location)

        locationApps = preferenceScreen.findPreference("prefcat_location_apps") ?: locationApps
        locationAppsAll = preferenceScreen.findPreference("pref_location_apps_all") ?: locationAppsAll
        locationAppsNone = preferenceScreen.findPreference("pref_location_apps_none") ?: locationAppsNone
        networkProviderCategory = preferenceScreen.findPreference("prefcat_location_network_provider") ?: networkProviderCategory
        wifiIchnaea = preferenceScreen.findPreference("pref_location_wifi_mls_enabled") ?: wifiIchnaea
        wifiMoving = preferenceScreen.findPreference("pref_location_wifi_moving_enabled") ?: wifiMoving
        wifiLearning = preferenceScreen.findPreference("pref_location_wifi_learning_enabled") ?: wifiLearning
        cellIchnaea = preferenceScreen.findPreference("pref_location_cell_mls_enabled") ?: cellIchnaea
        cellLearning = preferenceScreen.findPreference("pref_location_cell_learning_enabled") ?: cellLearning
        nominatim = preferenceScreen.findPreference("pref_geocoder_nominatim_enabled") ?: nominatim

        locationAppsAll.setOnPreferenceClickListener {
            findNavController().navigate(requireContext(), R.id.openAllLocationApps)
            true
        }
        fun configureChangeListener(preference: TwoStatePreference, listener: (Boolean) -> Unit) {
            preference.setOnPreferenceChangeListener { _, newValue ->
                listener(newValue as Boolean)
                true
            }
        }
        configureChangeListener(wifiIchnaea) {
            val settings = LocationSettings(requireContext())
            if (!it || settings.effectiveEndpoint != null) {
                settings.wifiIchnaea = it
            } else {
                openOnlineSourceSelector {
                    if (settings.effectiveEndpoint != null) {
                        settings.wifiIchnaea = true
                    } else {
                        wifiIchnaea.isChecked = false
                    }
                }
            }
        }
        configureChangeListener(wifiMoving) { LocationSettings(requireContext()).wifiMoving = it }
        configureChangeListener(wifiLearning) { LocationSettings(requireContext()).wifiLearning = it }
        configureChangeListener(cellIchnaea) {
            val settings = LocationSettings(requireContext())
            if (!it || settings.effectiveEndpoint != null) {
                settings.cellIchnaea = it
            } else {
                openOnlineSourceSelector {
                    if (settings.effectiveEndpoint != null) {
                        settings.cellIchnaea = true
                    } else {
                        cellIchnaea.isChecked = false
                    }
                }
            }
        }
        configureChangeListener(cellLearning) { LocationSettings(requireContext()).cellLearning = it }
        configureChangeListener(nominatim) { LocationSettings(requireContext()).geocoderNominatim = it }

        networkProviderCategory.isVisible = requireContext().hasNetworkLocationServiceBuiltIn()
        wifiLearning.isVisible =
            SDK_INT >= 17 && requireContext().getSystemService<LocationManager>()?.allProviders.orEmpty().contains(LocationManager.GPS_PROVIDER)
        cellLearning.isVisible =
            SDK_INT >= 17 && requireContext().getSystemService<LocationManager>()?.allProviders.orEmpty().contains(LocationManager.GPS_PROVIDER)
    }

    override fun onResume() {
        super.onResume()
        runCatching { updateContent() }.onFailure { database.close() }
        arguments?.let {
            if (it.containsKey(NavController.KEY_DEEP_LINK_INTENT)) {
                val intent = it.getParcelable<Intent>(NavController.KEY_DEEP_LINK_INTENT)
                when (intent?.getStringExtra(EXTRA_CONFIGURATION)) {
                    CONFIGURATION_FIELD_ONLINE_SOURCE -> openOnlineSourceSelector()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        database.close()
    }

    private fun updateContent() {
        lifecycleScope.launchWhenResumed {
            val context = requireContext()
            wifiIchnaea.isChecked = LocationSettings(context).wifiIchnaea
            wifiMoving.isChecked = LocationSettings(context).wifiMoving
            wifiLearning.isChecked = LocationSettings(context).wifiLearning
            cellIchnaea.isChecked = LocationSettings(context).cellIchnaea
            cellLearning.isChecked = LocationSettings(context).cellLearning
            nominatim.isChecked = LocationSettings(context).geocoderNominatim
            val (apps, showAll) = withContext(Dispatchers.IO) {
                val apps = database.listAppsByAccessTime()
                val res = apps.map { app ->
                    app to context.packageManager.getApplicationInfoIfExists(app.first)
                }.mapNotNull { (app, info) ->
                    if (info == null) null else app to info
                }.take(3).mapIndexed { idx, (app, applicationInfo) ->
                    val pref = AppIconPreference(context)
                    pref.order = idx
                    pref.applicationInfo = applicationInfo
                    pref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                        findNavController().navigate(requireContext(), R.id.openLocationAppDetails, bundleOf("package" to app.first))
                        true
                    }
                    pref.key = "pref_location_app_" + app.first
                    pref
                }.let { it to (it.size < apps.size) }
                database.close()
                res
            }
            locationAppsAll.isVisible = showAll
            locationApps.removeAll()
            for (app in apps) {
                locationApps.addPreference(app)
            }
            if (showAll) {
                locationApps.addPreference(locationAppsAll)
            } else if (apps.isEmpty()) {
                locationApps.addPreference(locationAppsNone)
            }
        }
    }
}