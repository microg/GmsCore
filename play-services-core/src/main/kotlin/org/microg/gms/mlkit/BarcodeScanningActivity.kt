/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.mlkit

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.R
import com.google.android.gms.common.internal.safeparcel.SafeParcelableSerializer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.microg.gms.auth.phone.AskPermissionActivity
import org.microg.gms.auth.phone.EXTRA_GRANT_RESULTS
import org.microg.gms.auth.phone.EXTRA_MESSENGER
import org.microg.gms.auth.phone.EXTRA_PERMISSIONS
import org.microg.gms.vision.barcode.QRCodeScannerView

private const val KEY_CALLING_APP_NAME = "extra_calling_app_name"
private const val KEY_BARCODE_RESULT = "extra_barcode_result"

class BarcodeScanningActivity : AppCompatActivity() {

    private val activePermissionRequestLock = Mutex()
    private var activePermissionRequest: Deferred<Boolean>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setContentView(R.layout.activity_barcode_scanning)
            lifecycleScope.launchWhenCreated {
                val callingApp = intent.getStringExtra(KEY_CALLING_APP_NAME)
                if (!ensurePermission(this@BarcodeScanningActivity, arrayOf(android.Manifest.permission.CAMERA))) {
                    showPermissionDialog(callingApp)
                    return@launchWhenCreated
                }
                findViewById<ImageView>(R.id.barcode_scanning_cancel).setOnClickListener {
                    finish()
                }
                if (callingApp != null) {
                    findViewById<TextView>(R.id.barcode_scanning_tips).text = String.format(getString(R.string.barcode_scanner_brand), callingApp)
                }
                val scannerView = findViewById<QRCodeScannerView>(R.id.scannerView)
                scannerView.startScanner { result ->
                    if (result != null) {
                        val resultIntent = Intent().apply {
                            putExtra(KEY_BARCODE_RESULT, SafeParcelableSerializer.serializeToBytes(result))
                        }
                        setResult(RESULT_OK, resultIntent)
                        finish()
                    }
                }
            }
        } else {
            finish()
        }
    }

    private fun showPermissionDialog(callingApp: String?) {
        AlertDialog.Builder(this).apply {
            setTitle(getString(R.string.camera_permission_dialog_title))
            setMessage(String.format(getString(R.string.camera_permission_dialog_message), callingApp))
            setPositiveButton(getString(R.string.camera_permission_dialog_button)){ dialog, _ ->
                dialog.dismiss()
                finish()
            }
        }.show()
    }

    private suspend fun ensurePermission(context:Context, permissions: Array<String>): Boolean {
        if (SDK_INT < 23)
            return true

        if (permissions.all { ContextCompat.checkSelfPermission(context, it) == PERMISSION_GRANTED })
            return true

        val (completable, deferred) = activePermissionRequestLock.withLock {
            if (activePermissionRequest == null) {
                val completable = CompletableDeferred<Boolean>()
                activePermissionRequest = completable
                completable to activePermissionRequest!!
            } else {
                null to activePermissionRequest!!
            }
        }
        if (completable != null) {
            val intent = Intent(context, AskPermissionActivity::class.java)
            intent.putExtra(EXTRA_MESSENGER, Messenger(object : Handler(Looper.getMainLooper()) {
                override fun handleMessage(msg: Message) {
                    if (msg.what == RESULT_OK) {
                        val grantResults = msg.data?.getIntArray(EXTRA_GRANT_RESULTS) ?: IntArray(0)
                        completable.complete(grantResults.size == permissions.size && grantResults.all { it == PERMISSION_GRANTED })
                    } else {
                        completable.complete(false)
                    }
                }
            }))
            intent.putExtra(EXTRA_PERMISSIONS, permissions)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }
        return deferred.await()
    }
}