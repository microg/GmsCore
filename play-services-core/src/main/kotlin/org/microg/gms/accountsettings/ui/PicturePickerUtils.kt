/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.accountsettings.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File

private const val CAMERA_TEMP_DIR = "octa_camera_temp"
private const val TAG = "PicturePickerUtils"

class PicturePickerUtils(private val activity: MainActivity, private val resultCallback: (Uri?) -> Unit, private val errorCallback: (ResultStatus) -> Unit) {
    private lateinit var fileChooserLauncher: ActivityResultLauncher<Intent>
    private var currentPhotoUri: Uri? = null

    init {
        initializeChooserLauncher()
    }

    private fun initializeChooserLauncher() {
        fileChooserLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data
                if (uri != null) {
                    resultCallback(uri)
                } else if (currentPhotoUri != null) {
                    resultCallback(currentPhotoUri)
                } else {
                    errorCallback(ResultStatus.FAILED)
                }
            } else {
                errorCallback(ResultStatus.USER_CANCEL)
            }
        }
    }

    fun launchChooser(mimeType: String) {
        if (mimeType.startsWith("image/") || mimeType == "image/*" || mimeType == "*/*") {
            when {
                ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                    launchChooserInternal(mimeType)
                }

                else -> {
                    launchFilePickerOnly(mimeType)
                }
            }
        } else {
            launchFilePickerOnly(mimeType)
        }
    }

    private fun launchFilePickerOnly(mimeType: String) {
        val getContentIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = mimeType
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        fileChooserLauncher.launch(getContentIntent)
    }

    private fun launchChooserInternal(mimeType: String) {
        val getContentIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = mimeType
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(activity.packageManager) != null) {
            currentPhotoUri = createImageUri()
            if (currentPhotoUri != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri)
                val chooserIntent = Intent.createChooser(getContentIntent, "Choose")
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(takePictureIntent))
                fileChooserLauncher.launch(chooserIntent)
            } else {
                fileChooserLauncher.launch(getContentIntent)
            }
        } else {
            fileChooserLauncher.launch(getContentIntent)
        }
    }

    private fun createImageUri(): Uri? {
        try {
            val cacheDir = activity.cacheDir
            val cameraDir = File(cacheDir, CAMERA_TEMP_DIR)
            if (!cameraDir.exists()) {
                cameraDir.mkdirs()
            }
            val photoFile = File(cameraDir, "camera_temp.jpg")
            if (photoFile.exists()) {
                photoFile.delete()
            }
            photoFile.createNewFile()
            return FileProvider.getUriForFile(activity, "${activity.packageName}.fileprovider", photoFile)
        } catch (e: Exception) {
            Log.w(TAG, "createImageUri: ", e)
            return null
        }
    }

}