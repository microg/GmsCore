package org.microg.gms.vision.barcode

import android.util.Log
import android.view.Surface

class RawBarcodeData(var bytes: ByteArray, var width: Int, var height: Int) {

    fun rotateDetail(rotate: Int){
        when (rotate) {
            Surface.ROTATION_90 -> rotateDegree90()
            Surface.ROTATION_180 -> rotateDegree180()
            Surface.ROTATION_270 -> rotateDegree270()
            else -> this
        }
    }

    private fun rotateDegree90(){
        val rotatedData = ByteArray(bytes.size)
        var index = 0

        // Rotate Y plane
        for (col in 0 until width) {
            for (row in height - 1 downTo 0) {
                rotatedData[index++] = bytes[row * width + col]
            }
        }

        // Rotate UV planes (UV interleaved)
        val uvHeight = height / 2
        for (col in 0 until width step 2) {
            for (row in uvHeight - 1 downTo 0) {
                rotatedData[index++] = bytes[width * height + row * width + col]
                rotatedData[index++] = bytes[width * height + row * width + col + 1]
            }
        }
        bytes = rotatedData
        val temp = width
        width = height
        height = temp
    }

    private fun rotateDegree180() {
        val rotatedData = ByteArray(bytes.size)
        var index = 0

        // Rotate Y plane
        for (row in height - 1 downTo 0) {
            for (col in width - 1 downTo 0) {
                rotatedData[index++] = bytes[row * width + col]
            }
        }

        // Rotate UV planes (UV interleaved)
        val uvHeight = height / 2
        val uvWidth = width / 2
        for (row in uvHeight - 1 downTo 0) {
            for (col in uvWidth - 1 downTo 0) {
                val offset = width * height + row * width + col * 2
                rotatedData[index++] = bytes[offset]
                rotatedData[index++] = bytes[offset + 1]
            }
        }
        bytes = rotatedData
    }


    private fun rotateDegree270(){
        val rotatedData = ByteArray(bytes.size)
        var index = 0

        // Rotate Y plane
        for (col in width - 1 downTo 0) {
            for (row in 0 until height) {
                rotatedData[index++] = bytes[row * width + col]
            }
        }

        // Rotate UV planes (UV interleaved)
        val uvHeight = height / 2
        for (col in width - 1 downTo 0 step 2) {
            for (row in 0 until uvHeight) {
                rotatedData[index++] = bytes[width * height + row * width + col - 1]
                rotatedData[index++] = bytes[width * height + row * width + col]
            }
        }
        bytes = rotatedData
        val temp = width
        width = height
        height = temp
    }

    override fun toString(): String {
        return "RawBarcodeData(bytes=${bytes.size}, width=$width, height=$height)"
    }
}