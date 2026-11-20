/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.mlkit.vision.documentscanner

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
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.tabs.TabLayout
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.CAPTURE_MODE_AUTO
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_PDF
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.microg.gms.utils.getApplicationLabel
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
import org.microg.mlkit.vision.documentscanner.core.R
import org.microg.mlkit.vision.documentscanner.GmsDocumentScannerConstants.EXTRA_CALLING_APP_NAME
import org.microg.mlkit.vision.documentscanner.GmsDocumentScannerConstants.EXTRA_CAMERA_ID
import org.microg.mlkit.vision.documentscanner.GmsDocumentScannerConstants.EXTRA_DEFAULT_CAPTURE_MODE
import org.microg.mlkit.vision.documentscanner.GmsDocumentScannerConstants.EXTRA_ENABLE_AUTO_ENHANCEMENTS
import org.microg.mlkit.vision.documentscanner.GmsDocumentScannerConstants.EXTRA_FILTER_ALLOWED
import org.microg.mlkit.vision.documentscanner.GmsDocumentScannerConstants.EXTRA_FLASH_MODE_CHANGE_ALLOWED
import org.microg.mlkit.vision.documentscanner.GmsDocumentScannerConstants.EXTRA_GALLERY_IMPORT_ALLOWED
import org.microg.mlkit.vision.documentscanner.GmsDocumentScannerConstants.EXTRA_PAGE_LIMIT_MAX
import org.microg.mlkit.vision.documentscanner.GmsDocumentScannerConstants.EXTRA_RESULT_FORMATS
import org.microg.mlkit.vision.documentscanner.GmsDocumentScannerConstants.EXTRA_RESULT_IMAGE_URIS
import org.microg.mlkit.vision.documentscanner.GmsDocumentScannerConstants.EXTRA_RESULT_PAGE_COUNT
import org.microg.mlkit.vision.documentscanner.GmsDocumentScannerConstants.EXTRA_RESULT_PDF_URI
import org.microg.mlkit.vision.documentscanner.GmsDocumentScannerConstants.EXTRA_SHADOW_REMOVAL_ALLOWED
import org.microg.mlkit.vision.documentscanner.GmsDocumentScannerConstants.EXTRA_STAIN_REMOVAL_ALLOWED
import java.io.File

private const val TAG = "DocumentScanning"

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
    private var discardDialog: AlertDialog? = null
    private var infoDialog: AlertDialog? = null
    private lateinit var scanDir: File

    private val callingAppName: String?
        get() {
            val appProvidedName = runCatching { intent?.getStringExtra(EXTRA_CALLING_APP_NAME) }.getOrNull()
            val callingName = runCatching { callingActivity?.packageName?.let { packageManager.getApplicationLabel(it).toString() } }.getOrNull()
            if (appProvidedName != null && callingName != null && appProvidedName != callingName) {
                return "$appProvidedName / $callingName"
            }
            return callingName ?: appProvidedName
        }

    private val pageLimit: Int
        get() = runCatching { intent?.getIntExtra(EXTRA_PAGE_LIMIT_MAX, -1) }.getOrNull() ?: -1

    private val removeShadows: Boolean
        get() = intent.getBooleanExtra(EXTRA_SHADOW_REMOVAL_ALLOWED, true) || intent.getBooleanExtra(EXTRA_STAIN_REMOVAL_ALLOWED, true)

    private val defaultFilter: DocumentFilter
        get() = if (intent.getBooleanExtra(EXTRA_ENABLE_AUTO_ENHANCEMENTS, false)) DocumentFilter.AUTO else DocumentFilter.ORIGINAL

    private val resultFormats: IntArray
        get() = runCatching { intent?.getIntArrayExtra(EXTRA_RESULT_FORMATS) }.getOrNull()?.takeIf { it.isNotEmpty() }
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
        onBackPressedDispatcher.addCallback(this) { navigateBack() }
        findViewById<ImageView>(R.id.document_scanning_cancel).setOnClickListener { confirmDiscard() }
        findViewById<View>(R.id.document_scanning_capture).setOnClickListener { capturePage() }
        findViewById<View>(R.id.document_scanning_done).setOnClickListener { showPreview() }
        findViewById<Button>(R.id.document_scanning_keep).setOnClickListener { keepReviewedPage() }
        findViewById<MaterialToolbar>(R.id.document_scanning_crop_rotate_top_bar).setNavigationOnClickListener { discardReview() }
        findViewById<Button>(R.id.document_scanning_auto_crop).setOnClickListener { findViewById<DocumentCropView>(R.id.document_scanning_crop).selectDetected() }
        findViewById<Button>(R.id.document_scanning_no_crop).setOnClickListener { findViewById<DocumentCropView>(R.id.document_scanning_crop).selectFullPage() }
        findViewById<Button>(R.id.document_scanning_rotate).setOnClickListener { rotateReviewedPage() }
        findViewById<TabLayout>(R.id.document_scanning_mode_bar).addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                setAutoCapture(tab.position == 1)
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
        findViewById<MaterialToolbar>(R.id.document_scanning_preview_top_bar).setNavigationOnClickListener { confirmDiscard() }
        findViewById<Button>(R.id.document_scanning_preview_done).setOnClickListener { finishScanning() }
        findViewById<View>(R.id.document_scanning_preview_add).setOnClickListener { showCamera(null) }
        findViewById<Button>(R.id.document_scanning_preview_crop).setOnClickListener { editSelectedPage() }
        findViewById<Button>(R.id.document_scanning_preview_retake).setOnClickListener { retakeSelectedPage() }
        findViewById<Button>(R.id.document_scanning_preview_delete).setOnClickListener { deleteSelectedPage() }
        findViewById<Button>(R.id.document_scanning_preview_filter).apply {
            visibility = if (intent.getBooleanExtra(EXTRA_FILTER_ALLOWED, true)) View.VISIBLE else View.GONE
            setOnClickListener { chooseFilter() }
        }
        setAutoCapture(intent.getIntExtra(EXTRA_DEFAULT_CAPTURE_MODE, -1) == CAPTURE_MODE_AUTO)
        findViewById<TabLayout>(R.id.document_scanning_mode_bar).getTabAt(if (autoCapture) 1 else 0)?.select()
        findViewById<Button>(R.id.document_scanning_import).apply {
            visibility = if (intent.getBooleanExtra(EXTRA_GALLERY_IMPORT_ALLOWED, false)) View.VISIBLE else View.GONE
            setOnClickListener { if (!busy) importLauncher.launch("image/*") }
        }
        callingAppName.let { name ->
            if (name == null) {
                findViewById<TextView>(R.id.document_scanning_brand).visibility = View.GONE
                findViewById<ImageView>(R.id.document_scanning_info_button).visibility = View.GONE
            } else {
                findViewById<TextView>(R.id.document_scanning_brand).text =
                    getString(R.string.document_scanner_brand, name)
                findViewById<ImageView>(R.id.document_scanning_info_button).setOnClickListener {
                    infoDialog = AlertDialog.Builder(this)
                        .setMessage(getString(R.string.document_scanner_info_content, name))
                        .setPositiveButton(android.R.string.ok) { _, _ ->  }
                        .show()
                }
            }
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
            camera.onDocumentStable = { if (autoCapture && discardDialog?.isShowing != true && infoDialog?.isShowing != true) capturePage() }
            camera.startCamera(this, intent.getStringExtra(EXTRA_CAMERA_ID)) {
                if (intent.getBooleanExtra(EXTRA_FLASH_MODE_CHANGE_ALLOWED, true) && camera.hasFlash) {
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
        findViewById<View>(R.id.document_scanning_hint).visibility = if (enabled) View.VISIBLE else View.GONE
        if (SDK_INT >= 21) findViewById<DocumentCaptureView>(R.id.document_scanning_camera).resetStability()
    }

    private fun toggleTorch() {
        if (SDK_INT < 21) return
        torchEnabled = !torchEnabled
        setCameraTorch(torchEnabled)
        findViewById<ImageView>(R.id.document_scanning_flash)
            .setImageResource(if (torchEnabled) R.drawable.ic_document_scanner_flash_on else R.drawable.ic_document_scanner_flash_off)
    }

    private fun setCameraTorch(enabled: Boolean) {
        if (SDK_INT >= 21) findViewById<DocumentCaptureView>(R.id.document_scanning_camera).setTorch(enabled)
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
            val adjustedPreviewCorners = runCatching { withContext(Dispatchers.IO) { previewCorners?.let { scaleCorners(file, it) } } }
                .onFailure { Log.w(TAG, "Failed to translate preview edges", it) }.getOrNull()
            val newlyDetectedCorners = runCatching { withContext(Dispatchers.IO) {  detectDocumentCorners(file, adjustedPreviewCorners) } }
                .onFailure { Log.w(TAG, "Failed to detect document edges", it) }.getOrNull()
            val corners = newlyDetectedCorners ?: adjustedPreviewCorners
            openReview(Review(file, corners, corners, null))
        }
    }

    private fun openReview(review: Review) {
        reviewing = review
        setCameraTorch(false)
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
        setCameraTorch(torchEnabled)
        updateControls()
    }

    private fun showPreview() {
        if (pages.isEmpty()) return showCamera(null)
        selectedPage = selectedPage.coerceIn(0, pages.size - 1)
        setCameraTorch(false)
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
        findViewById<View>(R.id.document_scanning_done).apply {
            isEnabled = !busy && reviewing == null
            visibility = if (pages.isNotEmpty()) View.VISIBLE else View.INVISIBLE
        }
        findViewById<TextView>(R.id.document_scanning_done_count).text = pages.size.toString()
        findViewById<ImageView>(R.id.document_scanning_done_thumbnail)
            .setImageBitmap(pages.firstOrNull()?.output?.let { loadPagePreview(it, PREVIEW_SIZE) })
        val reviewEnabled = !busy && reviewing != null
        for (id in intArrayOf(R.id.document_scanning_keep, R.id.document_scanning_auto_crop, R.id.document_scanning_no_crop, R.id.document_scanning_rotate)) {
            findViewById<Button>(id).isEnabled = reviewEnabled
        }
        val previewEnabled = !busy && reviewing == null && pages.isNotEmpty()
        for (id in intArrayOf(R.id.document_scanning_preview_done, R.id.document_scanning_preview_crop, R.id.document_scanning_preview_filter, R.id.document_scanning_preview_retake, R.id.document_scanning_preview_delete)) {
            findViewById<Button>(id).isEnabled = previewEnabled
        }
    }

    private fun navigateBack() {
        when {
            reviewing != null -> discardReview()
            isPreviewVisible -> if (!busy) showCamera(null)
            else -> confirmDiscard()
        }
    }

    private fun confirmDiscard() {
        if (pages.isEmpty() && pendingReview.isEmpty() && reviewing == null) return finish()
        discardDialog = AlertDialog.Builder(this)
            .setTitle(R.string.document_scanner_discard_title)
            .setMessage(R.string.document_scanner_discard_message)
            .setNegativeButton(R.string.document_scanner_continue_scanning, null)
            .setPositiveButton(R.string.document_scanner_discard) { _, _ -> finish() }
            .show()
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
            result.putParcelableArrayListExtra(EXTRA_RESULT_IMAGE_URIS, ArrayList(uris))
        }
        if (RESULT_FORMAT_PDF in resultFormats) {
            val pdf = File(scanDir, "scan.pdf")
            writePdf(outputs, pdf)
            val pdfUri = FileProvider.getUriForFile(this, authority, pdf)
            uris.add(pdfUri)
            result.putExtra(EXTRA_RESULT_PDF_URI, pdfUri)
            result.putExtra(EXTRA_RESULT_PAGE_COUNT, outputs.size)
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
            setTitle(getString(R.string.document_scanner_camera_permission_title))
            setMessage(getString(R.string.document_scanner_camera_permission_message, callingAppName))
            setPositiveButton(getString(android.R.string.ok)) { dialog, _ ->
                dialog.dismiss()
                finish()
            }
        }.show()
    }
}
