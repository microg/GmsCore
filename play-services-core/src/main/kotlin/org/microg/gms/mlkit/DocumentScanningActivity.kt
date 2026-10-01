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
import android.widget.LinearLayout
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
import org.microg.gms.vision.document.DocumentFilter
import org.microg.gms.vision.document.applyFilter
import org.microg.gms.vision.document.cropPage
import org.microg.gms.vision.document.detectDocumentCorners
import org.microg.gms.vision.document.importPage
import org.microg.gms.vision.document.loadPagePreview
import org.microg.gms.vision.document.normalizePage
import org.microg.gms.vision.document.rotatePage
import org.microg.gms.vision.document.scaleCorners
import org.microg.gms.vision.document.writePdf
import java.io.File

private const val TAG = "DocumentScanning"

private const val KEY_CALLING_APP_NAME = "string_extra_calling_app_name"
private const val KEY_PAGE_LIMIT_MAX = "int_extra_page_limit_max"
private const val KEY_RESULT_FORMATS = "int_array_extra_result_formats"
private const val KEY_GALLERY_IMPORT_ALLOWED = "boolean_extra_gallery_import_allowed"
private const val KEY_FLASH_MODE_CHANGE_ALLOWED = "boolean_extra_flash_mode_change_allowed"
private const val KEY_DEFAULT_CAPTURE_MODE = "int_extra_default_capture_mode"
private const val KEY_CAMERA_ID = "string_extra_camera_id"
private const val KEY_FILTER_ALLOWED = "boolean_extra_filter_allowed"
private const val KEY_AUTO_ENHANCEMENTS = "boolean_extra_enable_auto_enhancements"
private const val KEY_SHADOW_REMOVAL_ALLOWED = "boolean_extra_shadow_removal_allowed"
private const val KEY_STAIN_REMOVAL_ALLOWED = "boolean_extra_stain_removal_allowed"
private const val KEY_RESULT_IMAGE_URIS = "uri_array_extra_result_image_uris"
private const val KEY_RESULT_PDF_URI = "uri_extra_result_pdf_uri"
private const val KEY_RESULT_PAGE_COUNT = "int_extra_result_page_count"

private const val RESULT_FORMAT_JPEG = 101
private const val RESULT_FORMAT_PDF = 102

private const val CAPTURE_MODE_AUTO = 1

private const val SCAN_DIR = "mlkit_docscan"
private const val PREVIEW_SIZE = 1280
private const val THUMBNAIL_SIZE = 192

private class ScanPage(val source: File, val cropped: File, val output: File, var corners: FloatArray?, var detected: FloatArray?, var filter: DocumentFilter)

private class Review(val source: File, val corners: FloatArray?, val detected: FloatArray?, val page: ScanPage?)

class DocumentScanningActivity : AppCompatActivity() {

    private val pages = mutableListOf<ScanPage>()
    private val pendingReview = ArrayDeque<Pair<File, FloatArray?>>()
    private var reviewing: Review? = null
    private var insertIndex: Int? = null
    private var selectedPage = 0
    private var fileCounter = 0
    private var torchEnabled = false
    private var autoCapture = false
    private var busy = false
    private lateinit var scanDir: File

    private val callingAppName: String?
        get() = runCatching { intent?.getStringExtra(KEY_CALLING_APP_NAME) }.getOrNull()

    private val pageLimit: Int
        get() = runCatching { intent?.getIntExtra(KEY_PAGE_LIMIT_MAX, -1) }.getOrNull() ?: -1

    private val removeShadows: Boolean
        get() = intent.getBooleanExtra(KEY_SHADOW_REMOVAL_ALLOWED, true) || intent.getBooleanExtra(KEY_STAIN_REMOVAL_ALLOWED, true)

    private val defaultFilter: DocumentFilter
        get() = if (intent.getBooleanExtra(KEY_AUTO_ENHANCEMENTS, false)) DocumentFilter.AUTO else DocumentFilter.ORIGINAL

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
        findViewById<Button>(R.id.document_scanning_done).setOnClickListener { showPreview() }
        findViewById<Button>(R.id.document_scanning_keep).setOnClickListener { keepReviewedPage() }
        findViewById<Button>(R.id.document_scanning_retake).setOnClickListener { discardReview() }
        findViewById<Button>(R.id.document_scanning_auto_crop).setOnClickListener { findViewById<DocumentCropView>(R.id.document_scanning_crop).selectDetected() }
        findViewById<Button>(R.id.document_scanning_no_crop).setOnClickListener { findViewById<DocumentCropView>(R.id.document_scanning_crop).selectFullPage() }
        findViewById<Button>(R.id.document_scanning_rotate).setOnClickListener { rotateReviewedPage() }
        findViewById<Button>(R.id.document_scanning_mode_manual).setOnClickListener { setAutoCapture(false) }
        findViewById<Button>(R.id.document_scanning_mode_auto).setOnClickListener { setAutoCapture(true) }
        findViewById<ImageView>(R.id.document_scanning_preview_cancel).setOnClickListener { finish() }
        findViewById<Button>(R.id.document_scanning_preview_done).setOnClickListener { finishScanning() }
        findViewById<View>(R.id.document_scanning_preview_add).setOnClickListener { showCamera(null) }
        findViewById<Button>(R.id.document_scanning_preview_crop).setOnClickListener { editSelectedPage() }
        findViewById<Button>(R.id.document_scanning_preview_retake).setOnClickListener { retakeSelectedPage() }
        findViewById<Button>(R.id.document_scanning_preview_delete).setOnClickListener { deleteSelectedPage() }
        findViewById<Button>(R.id.document_scanning_preview_filter).apply {
            visibility = if (intent.getBooleanExtra(KEY_FILTER_ALLOWED, true)) View.VISIBLE else View.GONE
            setOnClickListener { chooseFilter() }
        }
        setAutoCapture(intent.getIntExtra(KEY_DEFAULT_CAPTURE_MODE, -1) == CAPTURE_MODE_AUTO)
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
            val camera = findViewById<DocumentCaptureView>(R.id.document_scanning_camera)
            camera.onDocumentStable = { if (autoCapture) capturePage() }
            camera.startCamera(this, intent.getStringExtra(KEY_CAMERA_ID)) {
                if (intent.getBooleanExtra(KEY_FLASH_MODE_CHANGE_ALLOWED, true) && camera.hasFlash) {
                    findViewById<ImageView>(R.id.document_scanning_flash).apply {
                        visibility = View.VISIBLE
                        setOnClickListener { toggleTorch() }
                    }
                }
            }
        }
    }

    private fun setAutoCapture(enabled: Boolean) {
        autoCapture = enabled
        findViewById<Button>(R.id.document_scanning_mode_manual).alpha = if (enabled) 0.5f else 1f
        findViewById<Button>(R.id.document_scanning_mode_auto).alpha = if (enabled) 1f else 0.5f
        findViewById<View>(R.id.document_scanning_hint).visibility = if (enabled) View.VISIBLE else View.GONE
        if (SDK_INT >= 21) findViewById<DocumentCaptureView>(R.id.document_scanning_camera).resetStability()
    }

    private fun toggleTorch() {
        if (SDK_INT < 21) return
        torchEnabled = !torchEnabled
        findViewById<DocumentCaptureView>(R.id.document_scanning_camera).setTorch(torchEnabled)
        findViewById<ImageView>(R.id.document_scanning_flash)
            .setImageResource(if (torchEnabled) R.drawable.ic_document_scanner_flash_on else R.drawable.ic_document_scanner_flash_off)
    }

    private val isPreviewVisible: Boolean
        get() = findViewById<View>(R.id.document_scanning_preview).visibility == View.VISIBLE

    private val pageCount: Int
        get() = pages.size + pendingReview.size + (if (reviewing?.page == null && reviewing != null) 1 else 0)

    private val isPageLimitReached: Boolean
        get() = pageLimit > 0 && pageCount >= pageLimit

    private fun nextPageFile(suffix: String) = File(scanDir, "page_${++fileCounter}_$suffix.jpg")

    private fun capturePage() {
        if (SDK_INT < 21 || busy || reviewing != null || isPreviewVisible || isPageLimitReached) return
        busy = true
        updateControls()
        val file = nextPageFile("source")
        findViewById<DocumentCaptureView>(R.id.document_scanning_camera).capture(file) { success, previewCorners ->
            lifecycleScope.launch {
                val added = success && runCatching { withContext(Dispatchers.IO) { normalizePage(file) } }
                    .onFailure { Log.w(TAG, "Failed to process captured page", it) }.isSuccess
                if (!added) Toast.makeText(this@DocumentScanningActivity, R.string.document_scanner_capture_failed, Toast.LENGTH_SHORT).show()
                busy = false
                if (added) queueForReview(file, previewCorners) else updateControls()
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
                val file = nextPageFile("source")
                val added = runCatching { withContext(Dispatchers.IO) { importPage({ contentResolver.openInputStream(uri)!! }, file) } }
                    .onFailure { Log.w(TAG, "Failed to import $uri", it) }.isSuccess
                if (added) imported.add(file)
            }
            busy = false
            imported.forEach { queueForReview(it, null) }
            updateControls()
        }
    }

    private fun queueForReview(file: File, previewCorners: FloatArray?) {
        pendingReview.addLast(file to previewCorners)
        if (reviewing == null) showNextReview() else updateControls()
    }

    private fun showNextReview() {
        val (file, previewCorners) = pendingReview.removeFirstOrNull() ?: (null to null)
        if (file == null) {
            closeReview()
            if (pages.isNotEmpty()) showPreview() else showCamera(insertIndex)
            return
        }
        busy = true
        updateControls()
        lifecycleScope.launch {
            val corners = runCatching { withContext(Dispatchers.IO) { previewCorners?.let { scaleCorners(file, it) } ?: detectDocumentCorners(file) } }
                .onFailure { Log.w(TAG, "Failed to detect document edges", it) }.getOrNull()
            openReview(Review(file, corners, corners, null))
        }
    }

    private fun openReview(review: Review) {
        reviewing = review
        findViewById<DocumentCropView>(R.id.document_scanning_crop).setPage(review.source, review.detected, review.corners)
        findViewById<View>(R.id.document_scanning_review).visibility = View.VISIBLE
        busy = false
        updateControls()
    }

    private fun closeReview() {
        reviewing = null
        findViewById<DocumentCropView>(R.id.document_scanning_crop).clear()
        findViewById<View>(R.id.document_scanning_review).visibility = View.GONE
        updateControls()
    }

    private fun keepReviewedPage() {
        val review = reviewing ?: return
        if (busy) return
        busy = true
        updateControls()
        val cropView = findViewById<DocumentCropView>(R.id.document_scanning_crop)
        val corners = cropView.corners.copyOf()
        val detected = cropView.detectedCorners
        val page = review.page ?: ScanPage(review.source, nextPageFile("crop"), nextPageFile("page"), corners, detected, defaultFilter)
        page.corners = corners
        page.detected = detected
        lifecycleScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    cropPage(page.source, corners, page.cropped)
                    applyFilter(page.cropped, page.filter, page.output, removeShadows)
                }
            }.onFailure { Log.w(TAG, "Failed to crop page", it) }
            busy = false
            if (review.page == null) {
                val index = (insertIndex ?: pages.size).coerceIn(0, pages.size)
                pages.add(index, page)
                selectedPage = index
                insertIndex = null
                if (pageLimit > 0 && pages.size >= pageLimit && pendingReview.isEmpty()) {
                    closeReview()
                    finishScanning()
                    return@launch
                }
                showNextReview()
            } else {
                closeReview()
                showPreview()
            }
        }
    }

    private fun discardReview() {
        val review = reviewing ?: return
        if (busy) return
        if (review.page != null) {
            closeReview()
            showPreview()
            return
        }
        review.source.delete()
        showNextReview()
    }

    private fun rotateReviewedPage() {
        val review = reviewing ?: return
        if (busy) return
        busy = true
        updateControls()
        val cropView = findViewById<DocumentCropView>(R.id.document_scanning_crop)
        val (corners, detected) = cropView.rotatedClockwise()
        lifecycleScope.launch {
            runCatching { withContext(Dispatchers.IO) { rotatePage(review.source) } }
                .onSuccess { cropView.setPage(review.source, detected, corners) }
                .onFailure { Log.w(TAG, "Failed to rotate page", it) }
            busy = false
            updateControls()
        }
    }

    private fun showCamera(insertAt: Int?) {
        insertIndex = insertAt
        findViewById<View>(R.id.document_scanning_preview).visibility = View.GONE
        if (SDK_INT >= 21) findViewById<DocumentCaptureView>(R.id.document_scanning_camera).resetStability()
        updateControls()
    }

    private fun showPreview() {
        if (pages.isEmpty()) return showCamera(null)
        selectedPage = selectedPage.coerceIn(0, pages.size - 1)
        findViewById<View>(R.id.document_scanning_preview).visibility = View.VISIBLE
        val thumbnails = findViewById<LinearLayout>(R.id.document_scanning_preview_thumbnails)
        val addButton = findViewById<View>(R.id.document_scanning_preview_add)
        thumbnails.removeAllViews()
        val density = resources.displayMetrics.density
        pages.forEachIndexed { index, page ->
            thumbnails.addView(ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams((48 * density).toInt(), (64 * density).toInt()).apply { marginEnd = (8 * density).toInt() }
                scaleType = ImageView.ScaleType.CENTER_CROP
                alpha = if (index == selectedPage) 1f else 0.5f
                setImageBitmap(loadPagePreview(page.output, THUMBNAIL_SIZE))
                setOnClickListener {
                    selectedPage = index
                    showPreview()
                }
            })
        }
        thumbnails.addView(addButton)
        addButton.visibility = if (isPageLimitReached) View.GONE else View.VISIBLE
        findViewById<ImageView>(R.id.document_scanning_preview_image).setImageBitmap(loadPagePreview(pages[selectedPage].output, PREVIEW_SIZE))
        findViewById<TextView>(R.id.document_scanning_preview_counter).text = getString(R.string.document_scanner_page_counter, selectedPage + 1, pages.size)
        updateControls()
    }

    private fun editSelectedPage() {
        val page = pages.getOrNull(selectedPage) ?: return
        if (busy) return
        openReview(Review(page.source, page.corners, page.detected, page))
    }

    private fun chooseFilter() {
        val page = pages.getOrNull(selectedPage) ?: return
        if (busy) return
        val labels = arrayOf(
            getString(R.string.document_scanner_filter_original),
            getString(R.string.document_scanner_filter_auto),
            getString(R.string.document_scanner_filter_grayscale),
            getString(R.string.document_scanner_filter_black_white)
        )
        AlertDialog.Builder(this)
            .setTitle(R.string.document_scanner_filter)
            .setSingleChoiceItems(labels, page.filter.ordinal) { dialog, which ->
                dialog.dismiss()
                setFilter(page, DocumentFilter.values()[which])
            }
            .show()
    }

    private fun setFilter(page: ScanPage, filter: DocumentFilter) {
        if (filter == page.filter) return
        busy = true
        updateControls()
        lifecycleScope.launch {
            runCatching { withContext(Dispatchers.IO) { applyFilter(page.cropped, filter, page.output, removeShadows) } }
                .onSuccess { page.filter = filter }
                .onFailure { Log.w(TAG, "Failed to apply filter", it) }
            busy = false
            showPreview()
        }
    }

    private fun deletePageFiles(page: ScanPage) {
        page.source.delete()
        page.cropped.delete()
        page.output.delete()
    }

    private fun retakeSelectedPage() {
        val page = pages.getOrNull(selectedPage) ?: return
        if (busy) return
        pages.removeAt(selectedPage)
        deletePageFiles(page)
        showCamera(selectedPage)
    }

    private fun deleteSelectedPage() {
        val page = pages.getOrNull(selectedPage) ?: return
        if (busy) return
        pages.removeAt(selectedPage)
        deletePageFiles(page)
        if (selectedPage >= pages.size) selectedPage = pages.size - 1
        showPreview()
    }

    private fun updateControls() {
        val canAdd = !busy && reviewing == null && !isPageLimitReached
        findViewById<View>(R.id.document_scanning_capture).isEnabled = canAdd
        findViewById<Button>(R.id.document_scanning_import).isEnabled = canAdd
        findViewById<Button>(R.id.document_scanning_done).apply {
            visibility = if (pages.isNotEmpty()) View.VISIBLE else View.INVISIBLE
            isEnabled = !busy && reviewing == null
            text = getString(R.string.document_scanner_done, pages.size)
        }
        val reviewEnabled = !busy && reviewing != null
        for (id in intArrayOf(R.id.document_scanning_keep, R.id.document_scanning_retake, R.id.document_scanning_auto_crop, R.id.document_scanning_no_crop, R.id.document_scanning_rotate)) {
            findViewById<Button>(id).isEnabled = reviewEnabled
        }
        val previewEnabled = !busy && reviewing == null && pages.isNotEmpty()
        for (id in intArrayOf(R.id.document_scanning_preview_done, R.id.document_scanning_preview_crop, R.id.document_scanning_preview_filter, R.id.document_scanning_preview_retake, R.id.document_scanning_preview_delete)) {
            findViewById<Button>(id).isEnabled = previewEnabled
        }
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
        val outputs = pages.map { it.output }
        val uris = arrayListOf<Uri>()
        val result = Intent()
        if (RESULT_FORMAT_JPEG in resultFormats) {
            outputs.mapTo(uris) { FileProvider.getUriForFile(this, authority, it) }
            result.putParcelableArrayListExtra(KEY_RESULT_IMAGE_URIS, ArrayList(uris))
        }
        if (RESULT_FORMAT_PDF in resultFormats) {
            val pdf = File(scanDir, "scan.pdf")
            writePdf(outputs, pdf)
            val pdfUri = FileProvider.getUriForFile(this, authority, pdf)
            uris.add(pdfUri)
            result.putExtra(KEY_RESULT_PDF_URI, pdfUri)
            result.putExtra(KEY_RESULT_PAGE_COUNT, outputs.size)
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
