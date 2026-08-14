/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.gms.moduleinstall.dynamicmodule

import android.content.ClipData
import android.app.PendingIntent
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.chimera.component.ModuleDownloadActivity
import com.google.android.chimera.component.ModuleImportCompletionReceiver
import com.google.android.chimera.config.DynamicModuleSettings
import com.google.android.gms.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Imports a module from an externally opened `.mods` file, an Android share, or a Uri returned by
 * the in-app SAF picker. Only the content-validated `.mods` container format is supported.
 *
 * Transparent activity: the import runs in the background and the result is reported only via Toast. Trust is
 * established per-apk by the importer (each module apk must be Google-signed), so no confirmation dialog is
 * shown here. The activity declares configChanges in the manifest so a rotation does not recreate it and
 * cancel the in-flight install mid-loop.
 */
class ModuleImportActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!DynamicModuleSettings.isAvailable(this)) {
            val message = if (DynamicModuleSettings.isRuntimeSupported()) {
                R.string.dynamicmodule_import_disabled
            } else {
                R.string.dynamicmodule_unsupported_android_version
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            setResult(RESULT_CANCELED)
            finish()
            return
        }
        val uri = resolveInputUri()
        if (uri == null) {
            Toast.makeText(this, R.string.dynamicmodule_import_no_file, Toast.LENGTH_SHORT).show()
            setResult(RESULT_CANCELED)
            finish()
            return
        }
        startImport(uri)
    }

    /** ACTION_VIEW carries the Uri in data; ACTION_SEND carries it in EXTRA_STREAM. */
    @Suppress("DEPRECATION")
    private fun resolveInputUri(): Uri? = when (intent?.action) {
        Intent.ACTION_SEND -> intent?.extractFileUri()
        else -> intent?.extractFileUri() ?: intent?.data
    }

    private fun Intent.extractFileUri(): Uri? = getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
        ?: (clipData?.extractFirstUri() ?: data)

    private fun ClipData.extractFirstUri(): Uri? = when {
        itemCount > 0 -> getItemAt(0).uri
        else -> null
    }

    @SuppressLint("StringFormatInvalid")
    private fun startImport(uri: Uri) {
        lifecycleScope.launch {
            val importResult = withContext(Dispatchers.IO) {
                ModsImporter.importFrom(this@ModuleImportActivity, uri)
            }
            val result = if (importResult.accepted) RESULT_OK else RESULT_CANCELED
            val message = when (importResult.failure) {
                ModsImportFailure.UNAVAILABLE -> if (DynamicModuleSettings.isRuntimeSupported()) {
                    getString(R.string.dynamicmodule_import_disabled)
                } else {
                    getString(R.string.dynamicmodule_unsupported_android_version)
                }

                ModsImportFailure.SOURCE_UNAVAILABLE,
                ModsImportFailure.INVALID_CONTAINER -> getString(R.string.dynamicmodule_import_no_file)

                else -> getString(
                    R.string.dynamicmodule_import_bundle_summary,
                    importResult.installed,
                    importResult.skipped,
                    importResult.rejected,
                )
            }
            Toast.makeText(this@ModuleImportActivity, message, Toast.LENGTH_LONG).show()
            sendImportResultCallback(result, importResult)
            if (result == RESULT_OK) {
                // This is only a wake-up hint for a pending module request. Its waiting page reloads
                // Chimera configuration and verifies its own requested feature before continuing.
                sendBroadcast(
                    Intent(this@ModuleImportActivity, ModuleImportCompletionReceiver::class.java)
                        .setAction(ModuleDownloadActivity.ACTION_MODULE_IMPORT_COMPLETED)
                )
            }
            setResult(result)
            finish()
        }
    }

    @Suppress("DEPRECATION")
    private fun sendImportResultCallback(resultCode: Int, importResult: ModsImportResult) {
        val callback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_IMPORT_RESULT_CALLBACK, PendingIntent::class.java)
        } else {
            intent.getParcelableExtra(EXTRA_IMPORT_RESULT_CALLBACK)
        } ?: return
        val resultIntent = Intent().apply {
            putExtra(EXTRA_IMPORT_ACCEPTED, importResult.accepted)
            putExtra(EXTRA_IMPORT_FAILURE, importResult.failure?.name)
        }
        runCatching {
            callback.send(this, resultCode, resultIntent)
        }.onSuccess {
            Log.i(TAG, "Import result callback sent accepted=${importResult.accepted}")
        }.onFailure {
            Log.w(TAG, "Unable to send import result callback", it)
        }
    }

    private companion object {
        const val TAG = "GmsModule/Import"
        const val EXTRA_IMPORT_RESULT_CALLBACK = "MODULE_IMPORT_RESULT_CALLBACK"
        const val EXTRA_IMPORT_ACCEPTED = "MODULE_IMPORT_ACCEPTED"
        const val EXTRA_IMPORT_FAILURE = "MODULE_IMPORT_FAILURE"
    }

}
