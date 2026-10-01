/**
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.vision.document

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.os.Build.VERSION.SDK_INT
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import kotlin.math.max
import kotlin.math.roundToInt

private const val MAX_PAGE_PIXELS = 2480
private const val JPEG_QUALITY = 90
private const val PDF_PAGE_LONG_SIDE_POINTS = 842

fun normalizePage(file: File) {
    val rotation = exifRotation(ExifInterface(file.absolutePath))
    val bitmap = decodeScaled({ file.inputStream() }) ?: throw IllegalArgumentException("Failed to decode $file")
    writePage(bitmap, rotation, file)
}

fun importPage(open: () -> InputStream, file: File) {
    val rotation = if (SDK_INT >= 24) runCatching { open().use { exifRotation(ExifInterface(it)) } }.getOrDefault(0f) else 0f
    val bitmap = decodeScaled(open) ?: throw IllegalArgumentException("Failed to decode image")
    writePage(bitmap, rotation, file)
}

fun rotatePage(file: File) {
    val bitmap = decodeScaled({ file.inputStream() }) ?: throw IllegalArgumentException("Failed to decode $file")
    writePage(bitmap, 90f, file)
}

private fun exifRotation(exif: ExifInterface) = when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
    else -> 0f
}

fun writePdf(pages: List<File>, file: File) {
    file.outputStream().buffered().use { stream ->
        val pdf = PdfWriter(stream)
        val pageIds = pages.indices.map { 3 + it * 3 }
        pdf.write("%PDF-1.4\n")
        pdf.startObject(1)
        pdf.write("<< /Type /Catalog /Pages 2 0 R >>\nendobj\n")
        pdf.startObject(2)
        pdf.write("<< /Type /Pages /Kids [${pageIds.joinToString(" ") { "$it 0 R" }}] /Count ${pages.size} >>\nendobj\n")
        pages.forEachIndexed { index, page ->
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(page.absolutePath, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IllegalArgumentException("Failed to decode $page")
            val scale = PDF_PAGE_LONG_SIDE_POINTS.toFloat() / max(bounds.outWidth, bounds.outHeight)
            val width = (bounds.outWidth * scale).roundToInt()
            val height = (bounds.outHeight * scale).roundToInt()
            val pageId = pageIds[index]
            val content = "q $width 0 0 $height 0 0 cm /Im0 Do Q\n"
            pdf.startObject(pageId)
            pdf.write("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 $width $height] /Resources << /XObject << /Im0 ${pageId + 2} 0 R >> >> /Contents ${pageId + 1} 0 R >>\nendobj\n")
            pdf.startObject(pageId + 1)
            pdf.write("<< /Length ${content.length} >>\nstream\n${content}endstream\nendobj\n")
            pdf.startObject(pageId + 2)
            pdf.write("<< /Type /XObject /Subtype /Image /Width ${bounds.outWidth} /Height ${bounds.outHeight} /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length ${page.length()} >>\nstream\n")
            pdf.write(page)
            pdf.write("\nendstream\nendobj\n")
        }
        pdf.finish(rootId = 1)
    }
}

private class PdfWriter(private val out: OutputStream) {
    private var position = 0L
    private val offsets = sortedMapOf<Int, Long>()

    fun write(text: String) {
        val bytes = text.toByteArray(Charsets.ISO_8859_1)
        out.write(bytes)
        position += bytes.size
    }

    fun write(file: File) {
        file.inputStream().use { position += it.copyTo(out) }
    }

    fun startObject(id: Int) {
        offsets[id] = position
        write("$id 0 obj\n")
    }

    fun finish(rootId: Int) {
        val xref = position
        val size = offsets.lastKey() + 1
        write("xref\n0 $size\n0000000000 65535 f \n")
        for (id in 1 until size) write("%010d 00000 n \n".format(offsets[id] ?: 0L))
        write("trailer\n<< /Size $size /Root $rootId 0 R >>\nstartxref\n$xref\n%%EOF\n")
    }
}

internal fun decodeScaled(open: () -> InputStream, maxSize: Int = MAX_PAGE_PIXELS): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    open().use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sampleSize = 1
    while (max(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >= maxSize) sampleSize *= 2
    val bitmap = open().use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sampleSize }) } ?: return null
    val longSide = max(bitmap.width, bitmap.height)
    if (longSide <= maxSize) return bitmap
    val scale = maxSize.toFloat() / longSide
    val scaled = Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).roundToInt(), (bitmap.height * scale).roundToInt(), true)
    if (scaled != bitmap) bitmap.recycle()
    return scaled
}

internal fun writePage(bitmap: Bitmap, rotation: Float, file: File) {
    val upright = if (rotation == 0f) bitmap else {
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(rotation) }, true).also {
            if (it != bitmap) bitmap.recycle()
        }
    }
    file.outputStream().use { upright.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
    upright.recycle()
}
