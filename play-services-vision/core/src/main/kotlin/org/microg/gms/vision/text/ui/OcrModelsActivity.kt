/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.vision.text.ui

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.text.format.Formatter
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.microg.gms.vision.core.R
import org.microg.gms.vision.text.model.store.OcrModels

class OcrModelsActivity : AppCompatActivity() {
    private var pendingPack: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (android.os.Build.VERSION.SDK_INT < 26) {
            finish()
            return
        }
        setContentView(R.layout.ocr_models_activity)
        setSupportActionBar(findViewById(R.id.ocr_models_toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        pendingPack = savedInstanceState?.getString(EXTRA_OCR_PACK)
        if (savedInstanceState == null) {
            showModels(intent.getStringExtra(EXTRA_OCR_PACK))
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingPack = intent.getStringExtra(EXTRA_OCR_PACK)?.takeIf { packLabel(it) != null }
    }

    override fun onResumeFragments() {
        super.onResumeFragments()
        val pack = pendingPack ?: return
        pendingPack = null
        showModels(pack)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(EXTRA_OCR_PACK, pendingPack)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun showModels(pack: String?) {
        val fragment = OcrModelsFragment().apply {
            arguments = Bundle().apply { putString(EXTRA_OCR_PACK, pack) }
        }
        supportFragmentManager.beginTransaction().replace(R.id.ocr_models_content, fragment).commit()
    }
}

open class OcrModelsFragment : PreferenceFragmentCompat() {
    private val rows = mutableMapOf<String, Preference>()
    private val workByPack = mutableMapOf<String, List<WorkInfo>>()
    private lateinit var notificationSettings: Preference
    private var refreshJob: Job? = null
    private var requestedPack: String? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val context = requireContext()
        requestedPack = if (savedInstanceState == null) arguments?.getString(EXTRA_OCR_PACK) else savedInstanceState.getString(EXTRA_OCR_PACK)
        preferenceScreen = preferenceManager.createPreferenceScreen(context)
        notificationSettings = Preference(context).apply {
            setTitle(R.string.ocr_models_notifications_disabled)
            setSummary(R.string.ocr_models_notifications_settings)
            isPersistent = false
            isVisible = false
        }
        preferenceScreen.addPreference(notificationSettings)
        for (pack in OcrModels.packs) {
            val label = context.packLabel(pack) ?: continue
            val row = Preference(context).apply {
                key = pack
                title = label
                isPersistent = false
                isEnabled = false
            }
            rows[pack] = row
            preferenceScreen.addPreference(row)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        workByPack.clear()
        val manager = WorkManager.getInstance(requireContext())
        for (pack in rows.keys) {
            manager.getWorkInfosForUniqueWorkLiveData(OcrModels.workName(pack)).observe(viewLifecycleOwner) { work ->
                val current = work.orEmpty()
                val previousDownload = workByPack.put(pack, current)?.firstOrNull { !it.state.isFinished }
                val download = current.firstOrNull { !it.state.isFinished }
                if (download?.state == WorkInfo.State.RUNNING &&
                    previousDownload?.state == WorkInfo.State.RUNNING && download.id == previousDownload.id
                ) {
                    rows.getValue(pack).summary = downloadingSummary(download)
                } else {
                    refresh()
                }
            }
        }
        childFragmentManager.setFragmentResultListener(MODELS_CHANGED, viewLifecycleOwner) { _, _ -> refresh() }
    }

    override fun onResume() {
        super.onResume()
        val settingsIntent = OcrModelPrompt.blockedNotificationSettingsIntent(requireContext())
        notificationSettings.intent = settingsIntent
        notificationSettings.isVisible = settingsIntent != null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(EXTRA_OCR_PACK, requestedPack)
    }

    private fun refresh() {
        refreshJob?.cancel()
        val context = requireContext().applicationContext
        refreshJob = viewLifecycleOwner.lifecycleScope.launch {
            val readyPacks = withContext(Dispatchers.IO) {
                rows.keys.associateWith { pack ->
                    PackStatus(
                        OcrModels.isReady(context, pack),
                        OcrModels.remainingBytes(context, pack)
                    )
                }
            }
            for ((pack, status) in readyPacks) {
                val row = rows.getValue(pack)
                val work = workByPack[pack] ?: continue
                val active = work.firstOrNull { !it.state.isFinished }
                val action = when {
                    active != null -> ModelAction.CANCEL
                    status.ready -> ModelAction.DELETE
                    else -> ModelAction.DOWNLOAD
                }
                row.summary = when {
                    active?.state == WorkInfo.State.RUNNING -> downloadingSummary(active)
                    active != null -> getString(R.string.ocr_models_queued)
                    status.ready -> getString(R.string.ocr_models_ready)
                    work.any { it.state == WorkInfo.State.FAILED } -> getString(R.string.ocr_models_failed)
                    else -> getString(R.string.ocr_models_available, Formatter.formatFileSize(context, status.remainingBytes))
                }
                row.setOnPreferenceClickListener {
                    showAction(pack, action, status.remainingBytes)
                    true
                }
                row.isEnabled = true
                if (requestedPack == pack && !childFragmentManager.isStateSaved) {
                    requestedPack = null
                    if (action == ModelAction.DOWNLOAD) showAction(pack, action, status.remainingBytes)
                }
            }
        }
    }

    private fun downloadingSummary(info: WorkInfo): String {
        val total = info.progress.getLong("totalBytes", 0)
        if (total <= 0) return getString(R.string.ocr_models_downloading)
        val current = info.progress.getLong("downloadedBytes", 0).coerceIn(0, total)
        return getString(R.string.ocr_models_progress, (current * 100 / total).toInt())
    }

    private fun showAction(pack: String, action: ModelAction, remainingBytes: Long) {
        if (childFragmentManager.isStateSaved || childFragmentManager.findFragmentByTag(ACTION_DIALOG) != null) return
        val dialog = OcrModelActionDialog().apply {
            arguments = Bundle().apply {
                putString(EXTRA_OCR_PACK, pack)
                putString(ACTION_DIALOG, action.name)
                putLong(REMAINING_BYTES, remainingBytes)
            }
        }
        dialog.showNow(childFragmentManager, ACTION_DIALOG)
    }

    private data class PackStatus(val ready: Boolean, val remainingBytes: Long)
}

private const val MODELS_CHANGED = "ocrModelsChanged"
private const val ACTION_DIALOG = "ocrModelAction"
private const val REMAINING_BYTES = "remainingBytes"
private const val UNMETERED = "unmetered"

private enum class ModelAction { DOWNLOAD, CANCEL, DELETE }

open class OcrModelActionDialog : DialogFragment() {
    private var unmetered = true
    private val action: ModelAction?
        get() = ModelAction.entries.firstOrNull { it.name == arguments?.getString(ACTION_DIALOG) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (action == null) {
            showsDialog = false
            dismissAllowingStateLoss()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val args = requireArguments()
        val pack = requireNotNull(args.getString(EXTRA_OCR_PACK))
        val label = requireNotNull(context.packLabel(pack))
        val action = requireNotNull(this.action)
        unmetered = savedInstanceState?.getBoolean(UNMETERED, true) ?: true
        val builder = AlertDialog.Builder(context).setNegativeButton(android.R.string.cancel, null)
        when (action) {
            ModelAction.DOWNLOAD -> {
                val size = Formatter.formatFileSize(context, args.getLong(REMAINING_BYTES))
                builder.setTitle(getString(R.string.ocr_models_confirm, label, size))
                    .setSingleChoiceItems(
                        arrayOf(getString(R.string.ocr_models_unmetered), getString(R.string.ocr_models_any_network)),
                        if (unmetered) 0 else 1
                    ) { _, which -> unmetered = which == 0 }
                    .setPositiveButton(R.string.ocr_models_download) { _, _ -> applyAction(action, pack) }
            }

            ModelAction.CANCEL -> builder.setTitle(getString(R.string.ocr_models_cancel_confirm, label))
                .setPositiveButton(R.string.ocr_models_cancel_download) { _, _ -> applyAction(action, pack) }

            ModelAction.DELETE -> builder.setTitle(getString(R.string.ocr_models_delete_confirm, label))
                .setPositiveButton(R.string.ocr_models_delete) { _, _ -> applyAction(action, pack) }
        }
        return builder.create()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(UNMETERED, unmetered)
    }

    private fun applyAction(action: ModelAction, pack: String) {
        val owner = requireActivity()
        val results = parentFragmentManager
        val context = owner.applicationContext
        val useUnmeteredNetwork = unmetered
        owner.lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    when (action) {
                        ModelAction.DOWNLOAD -> OcrModels.start(context, pack, useUnmeteredNetwork)
                        ModelAction.CANCEL -> OcrModels.cancel(context, pack)
                        ModelAction.DELETE -> OcrModels.delete(context, pack)
                    }
                }
                results.setFragmentResult(MODELS_CHANGED, Bundle.EMPTY)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Toast.makeText(context, R.string.ocr_models_action_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
