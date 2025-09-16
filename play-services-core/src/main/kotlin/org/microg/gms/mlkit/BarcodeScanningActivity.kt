/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.mlkit

import android.Manifest.permission
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.R
import com.google.android.gms.common.internal.safeparcel.SafeParcelableSerializer
import org.microg.gms.vision.barcode.QRCodeScannerView

private const val KEY_CALLING_APP_NAME = "extra_calling_app_name"
private const val KEY_BARCODE_RESULT = "extra_barcode_result"

class BarcodeScanningActivity : AppCompatActivity() {

    private val clientPackageName: String?
        get() = runCatching {
            intent?.extras?.takeIf { it.containsKey(KEY_CALLING_APP_NAME) }?.getString(KEY_CALLING_APP_NAME)
        }.getOrNull()

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                startScanning()
            } else {
                showPermissionDialog(clientPackageName)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (SDK_INT < 21) {
            finish()
            return
        }
        setContentView(R.layout.activity_barcode_scanning)
        findViewById<ImageView>(R.id.barcode_scanning_cancel).setOnClickListener {
            finish()
        }
        if (clientPackageName != null) {
            findViewById<TextView>(R.id.barcode_scanning_tips).text = String.format(getString(R.string.barcode_scanner_brand), clientPackageName)
        }
        if (ContextCompat.checkSelfPermission(this, permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(permission.CAMERA)
        } else {
            startScanning()
        }
    }

    private fun startScanning(){
        lifecycleScope.launchWhenCreated {
            if (SDK_INT >= 21) {
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

}