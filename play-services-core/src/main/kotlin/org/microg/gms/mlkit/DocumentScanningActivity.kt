/**
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.mlkit

import android.Manifest.permission
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.microg.gms.vision.document.DocumentCaptureView
import org.microg.gms.vision.document.DocumentCropView
import org.microg.gms.vision.document.cropPage
import org.microg.gms.vision.document.detectDocumentCorners
import org.microg.gms.vision.document.importPage
import org.microg.gms.vision.document.normalizePage
import org.microg.gms.vision.document.writePdf
import java.io.File

private const val TAG = "DocumentScanning"

private const val KEY_CALLING_APP_NAME = "string_extra_calling_app_name"
private const val KEY_PAGE_LIMIT_MAX = "int_extra_page_limit_max"
private const val KEY_RESULT_FORMATS = "int_array_extra_result_formats"
private const val KEY_GALLERY_IMPORT_ALLOWED = "boolean_extra_gallery_import_allowed"
private const val KEY_RESULT_IMAGE_URIS = "uri_array_extra_result_image_uris"
private const val KEY_RESULT_PDF_URI = "uri_extra_result_pdf_uri"
private const val KEY_RESULT_PAGE_COUNT = "int_extra_result_page_count"

private const val RESULT_FORMAT_JPEG = 101
private const val RESULT_FORMAT_PDF = 102

private const val SCAN_DIR = "mlkit_docscan"

class DocumentScanningActivity : AppCompatActivity() {

    private val pages = mutableListOf<File>()
    private val pendingReview = ArrayDeque<File>()
    private var reviewing: File? = null
    private var fileCounter = 0
    private var busy = false
    private lateinit var scanDir: File

    private val callingAppName: String?
        get() = runCatching { intent?.getStringExtra(KEY_CALLING_APP_NAME) }.getOrNull()

    private val pageLimit: Int
        get() = runCatching { intent?.getIntExtra(KEY_PAGE_LIMIT_MAX, -1) }.getOrNull() ?: -1

    private val resultFormats: IntArray
        get() = runCatching { intent?.getIntArrayExtra(KEY_RESULT_FORMATS) }.getOrNull()?.takeIf { it.isNotEmpty() }
            ?: intArrayOf(RESULT_FORMAT_JPEG, RESULT_FORMAT_PDF)

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                startCamera()
            } else {
                showPermissionDialog()
            }
        }

    private val importLauncher =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
            if (uris.isNotEmpty()) importPages(uris)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (SDK_INT < 21) {
            finish()
            return
        }
        scanDir = File(cacheDir, SCAN_DIR)
        scanDir.deleteRecursively()
        scanDir.mkdirs()

        setContentView(R.layout.activity_document_scanning)
        findViewById<ImageView>(R.id.document_scanning_cancel).setOnClickListener { finish() }
        findViewById<View>(R.id.document_scanning_capture).setOnClickListener { capturePage() }
        findViewById<Button>(R.id.document_scanning_done).setOnClickListener { finishScanning() }
        findViewById<Button>(R.id.document_scanning_keep).setOnClickListener { keepReviewedPage() }
        findViewById<Button>(R.id.document_scanning_retake).setOnClickListener { retakeReviewedPage() }
        findViewById<Button>(R.id.document_scanning_import).apply {
            visibility = if (intent.getBooleanExtra(KEY_GALLERY_IMPORT_ALLOWED, false)) View.VISIBLE else View.GONE
            setOnClickListener { if (!busy) importLauncher.launch("image/*") }
        }
        callingAppName?.let {
            findViewById<TextView>(R.id.document_scanning_tips).text = getString(R.string.barcode_scanner_brand, it)
        }
        updateControls()

        if (ContextCompat.checkSelfPermission(this, permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(permission.CAMERA)
        } else {
            startCamera()
        }
    }

    private fun startCamera() {
        if (SDK_INT >= 21) {
            findViewById<DocumentCaptureView>(R.id.document_scanning_camera).startCamera(this)
        }
    }

    private val pageCount: Int
        get() = pages.size + pendingReview.size + (if (reviewing != null) 1 else 0)

    private val isPageLimitReached: Boolean
        get() = pageLimit > 0 && pageCount >= pageLimit

    private fun nextPageFile() = File(scanDir, "page_${++fileCounter}.jpg")

    private fun capturePage() {
        if (SDK_INT < 21 || busy || isPageLimitReached) return
        busy = true
        updateControls()
        val file = nextPageFile()
        findViewById<DocumentCaptureView>(R.id.document_scanning_camera).capture(file) { success ->
            lifecycleScope.launch {
                val added = success && runCatching { withContext(Dispatchers.IO) { normalizePage(file) } }
                    .onFailure { Log.w(TAG, "Failed to process captured page", it) }.isSuccess
                if (!added) Toast.makeText(this@DocumentScanningActivity, R.string.document_scanner_capture_failed, Toast.LENGTH_SHORT).show()
                busy = false
                if (added) queueForReview(file) else updateControls()
            }
        }
    }

    private fun importPages(uris: List<Uri>) {
        busy = true
        updateControls()
        lifecycleScope.launch {
            val imported = mutableListOf<File>()
            for (uri in uris) {
                if (pageLimit > 0 && pageCount + imported.size >= pageLimit) break
                val file = nextPageFile()
                val added = runCatching { withContext(Dispatchers.IO) { importPage({ contentResolver.openInputStream(uri)!! }, file) } }
                    .onFailure { Log.w(TAG, "Failed to import $uri", it) }.isSuccess
                if (added) imported.add(file)
            }
            busy = false
            imported.forEach { queueForReview(it) }
            updateControls()
        }
    }

    private fun queueForReview(file: File) {
        pendingReview.addLast(file)
        if (reviewing == null) showNextReview() else updateControls()
    }

    private fun showNextReview() {
        val file = pendingReview.removeFirstOrNull()
        reviewing = file
        if (file == null) {
            findViewById<DocumentCropView>(R.id.document_scanning_crop).clear()
            findViewById<View>(R.id.document_scanning_review).visibility = View.GONE
            updateControls()
            if (pageLimit > 0 && pages.size >= pageLimit) finishScanning()
            return
        }
        busy = true
        updateControls()
        lifecycleScope.launch {
            val corners = runCatching { withContext(Dispatchers.IO) { detectDocumentCorners(file) } }
                .onFailure { Log.w(TAG, "Failed to detect document edges", it) }.getOrNull()
            findViewById<DocumentCropView>(R.id.document_scanning_crop).setPage(file, corners)
            findViewById<View>(R.id.document_scanning_review).visibility = View.VISIBLE
            busy = false
            updateControls()
        }
    }

    private fun keepReviewedPage() {
        val file = reviewing ?: return
        if (busy) return
        busy = true
        updateControls()
        val corners = findViewById<DocumentCropView>(R.id.document_scanning_crop).corners.copyOf()
        lifecycleScope.launch {
            runCatching { withContext(Dispatchers.IO) { cropPage(file, corners) } }
                .onFailure { Log.w(TAG, "Failed to crop page", it) }
            pages.add(file)
            busy = false
            showNextReview()
        }
    }

    private fun retakeReviewedPage() {
        val file = reviewing ?: return
        if (busy) return
        file.delete()
        showNextReview()
    }

    private fun updateControls() {
        val canAdd = !busy && reviewing == null && !isPageLimitReached
        findViewById<View>(R.id.document_scanning_capture).isEnabled = canAdd
        findViewById<Button>(R.id.document_scanning_import).isEnabled = canAdd
        findViewById<Button>(R.id.document_scanning_done).apply {
            isEnabled = !busy && reviewing == null && pages.isNotEmpty()
            text = getString(R.string.document_scanner_done, pages.size)
        }
        findViewById<Button>(R.id.document_scanning_keep).isEnabled = !busy && reviewing != null
        findViewById<Button>(R.id.document_scanning_retake).isEnabled = !busy && reviewing != null
    }

    private fun finishScanning() {
        if (pages.isEmpty()) return
        busy = true
        updateControls()
        lifecycleScope.launch {
            val result = runCatching { withContext(Dispatchers.IO) { buildResult() } }
                .onFailure { Log.w(TAG, "Failed to create scan result", it) }.getOrNull()
            if (result != null) {
                setResult(RESULT_OK, result)
                finish()
            } else {
                Toast.makeText(this@DocumentScanningActivity, R.string.document_scanner_capture_failed, Toast.LENGTH_SHORT).show()
                busy = false
                updateControls()
            }
        }
    }

    private fun buildResult(): Intent {
        val authority = "$packageName.fileprovider"
        val uris = arrayListOf<Uri>()
        val result = Intent()
        if (RESULT_FORMAT_JPEG in resultFormats) {
            pages.mapTo(uris) { FileProvider.getUriForFile(this, authority, it) }
            result.putParcelableArrayListExtra(KEY_RESULT_IMAGE_URIS, ArrayList(uris))
        }
        if (RESULT_FORMAT_PDF in resultFormats) {
            val pdf = File(scanDir, "scan.pdf")
            writePdf(pages, pdf)
            val pdfUri = FileProvider.getUriForFile(this, authority, pdf)
            uris.add(pdfUri)
            result.putExtra(KEY_RESULT_PDF_URI, pdfUri)
            result.putExtra(KEY_RESULT_PAGE_COUNT, pages.size)
        }
        // URI permissions are only granted for data and clip data, not extras
        if (uris.isNotEmpty()) {
            result.clipData = ClipData.newRawUri(null, uris[0]).apply { uris.drop(1).forEach { addItem(ClipData.Item(it)) } }
            result.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return result
    }

    private fun showPermissionDialog() {
        AlertDialog.Builder(this).apply {
            setTitle(getString(R.string.camera_permission_dialog_title))
            setMessage(getString(R.string.document_scanner_camera_permission_message, callingAppName))
            setPositiveButton(getString(R.string.camera_permission_dialog_button)) { dialog, _ ->
                dialog.dismiss()
                finish()
            }
        }.show()
    }
}
