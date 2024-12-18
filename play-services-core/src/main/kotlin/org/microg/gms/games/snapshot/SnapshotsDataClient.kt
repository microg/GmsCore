/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.games.snapshot

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.text.TextUtils
import android.util.Log
import com.android.volley.toolbox.Volley
import com.google.android.gms.common.util.IOUtils
import com.google.android.gms.drive.Contents
import com.google.android.gms.games.snapshot.SnapshotMetadataChangeEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.microg.gms.games.CommitSnapshotRevisionRequest
import org.microg.gms.games.PrepareSnapshotRevisionRequest
import org.microg.gms.games.ResolveSnapshotHeadRequest
import org.microg.gms.games.SnapshotContent
import org.microg.gms.games.SnapshotContentInfo
import org.microg.gms.games.SnapshotTimeInfo
import org.microg.gms.games.snapshot.SnapshotsApiClient.SNAPSHOT_UPLOAD_LINK_DATA
import org.microg.gms.games.snapshot.SnapshotsApiClient.SNAPSHOT_UPLOAD_LINK_IMAGE
import org.microg.gms.games.ukq
import org.microg.gms.profile.Build
import org.microg.gms.utils.BitmapUtils
import org.microg.gms.utils.singleInstanceOf
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.UUID

class SnapshotsDataClient(val context: Context) {

    private val requestQueue = singleInstanceOf { Volley.newRequestQueue(context.applicationContext) }

    suspend fun loadSnapshotData(oauthToken: String) = withContext(Dispatchers.IO) {
        val snapshots = arrayListOf<Snapshot>()
        val snapshotsResponse = runCatching { SnapshotsApiClient.requestSnapshotList(context, oauthToken) }.getOrNull()
        snapshotsResponse?.gameSnapshot?.forEach {
            snapshots.add(it.toSnapshot())
        }
        return@withContext snapshots
    }

    private suspend fun uploadDataByUrl(oauthToken: String, url: String, body: ByteArray) = withContext(Dispatchers.IO) {
        runCatching { SnapshotsApiClient.uploadDataByUrl(oauthToken, url, requestQueue, body) }
    }

    suspend fun getDataFromDrive(oauthToken: String, url: String) = withContext(Dispatchers.IO) {
        runCatching { SnapshotsApiClient.getDataFromDrive(oauthToken, url, requestQueue) }.getOrNull()
    }

    private suspend fun getRealUploadUrl(oauthToken: String, url: String) = withContext(Dispatchers.IO) {
        runCatching { SnapshotsApiClient.getRealUploadUrl(oauthToken, url, requestQueue) }.getOrNull()
    }

    suspend fun resolveSnapshotHead(oauthToken: String, resolveSnapshotHeadRequest: ResolveSnapshotHeadRequest) = withContext(Dispatchers.IO) {
        runCatching { SnapshotsApiClient.resolveSnapshotHead(context, oauthToken, resolveSnapshotHeadRequest) }.getOrNull()
    }

    private suspend fun prepareSnapshotRevision(oauthToken: String, prepareSnapshotRevisionRequest: PrepareSnapshotRevisionRequest) =
        withContext(Dispatchers.IO) {
            runCatching { SnapshotsApiClient.prepareSnapshotRevision(context, oauthToken, prepareSnapshotRevisionRequest) }.getOrNull()
        }

    private suspend fun commitSnapshotRevision(oauthToken: String, commitSnapshotRevisionRequest: CommitSnapshotRevisionRequest) = withContext(Dispatchers.IO) {
        runCatching { SnapshotsApiClient.commitSnapshotRevision(context, oauthToken, commitSnapshotRevisionRequest) }.getOrNull()
    }

    suspend fun deleteSnapshotData(oauthToken: String, snapshot: Snapshot) = withContext(Dispatchers.IO) {
        runCatching { SnapshotsApiClient.deleteSnapshot(context, oauthToken, snapshot) }.getOrNull()
    }

    suspend fun commitSnapshot(
        oauthToken: String, snapshotTitle: String?, change: SnapshotMetadataChangeEntity, contents: Contents, maxCoverImageSize: Int
    ) = withContext(Dispatchers.IO) {
        runCatching {
            if (TextUtils.isEmpty(snapshotTitle)) {
                return@runCatching false
            }

            //Get data upload link
            val ret = prepareSnapshotRevision(oauthToken, createPrepareSnapshotRevisionRequest(change, snapshotTitle!!))
            Log.d(TAG, "commitSnapshot ret: $ret")
            if (ret != null && ret.uploadLinkInfos.isNotEmpty()) {
                val imageUploadTempUrl = ret.uploadLinkInfos.firstOrNull { it.id == SNAPSHOT_UPLOAD_LINK_IMAGE }?.url

                val snapshotDataUploadTempUrl = ret.uploadLinkInfos.firstOrNull { it.id == SNAPSHOT_UPLOAD_LINK_DATA }?.url
                if (snapshotDataUploadTempUrl == null) {
                    Log.w(TAG, "commitSnapshot data upload temp url is null")
                    return@runCatching false
                }

                val deferredGetImageRealUploadUrl = async {
                    Log.w(TAG, "commitSnapshot image upload temp url is null")
                    if (imageUploadTempUrl == null) {
                        null
                    } else {
                        getRealUploadUrl(oauthToken, imageUploadTempUrl)
                    }
                }
                val deferredGetDataRealUploadUrl = async {
                    getRealUploadUrl(oauthToken, snapshotDataUploadTempUrl)
                }


                val deferredUploadSnapshotImage = async {
                    val snapshotImageUploadUrl = deferredGetImageRealUploadUrl.await()
                    if (snapshotImageUploadUrl == null) {
                        Log.w(TAG, "commitSnapshot image upload url is null")
                        Triple(0, 0, null)
                    } else {
                        uploadSnapshotImage(
                            change!!.coverImageTeleporter!!.createTargetBitmap(), maxCoverImageSize, oauthToken, snapshotImageUploadUrl
                        )
                    }

                }

                val deferredUploadSnapshotData = async {
                    val snapshotDataUploadUrl = deferredGetDataRealUploadUrl.await()
                    if (snapshotDataUploadUrl == null) {
                        Log.w(TAG, "commitSnapshot data upload url is null")
                        return@async null
                    }
                    uploadSnapshotData(contents, oauthToken, snapshotDataUploadUrl)
                }

                val deferredCommit = async {
                    val (imageWidth, imageHeight, snapShotImageResourceId: String?) = deferredUploadSnapshotImage.await()
                    val snapshotDataResourceId = deferredUploadSnapshotData.await()
                    if (snapshotDataResourceId != null) {
                        commitSnapshotRevision(
                            oauthToken, createCommitSnapshotRequest(
                                change, snapshotDataResourceId, snapShotImageResourceId, imageWidth, imageHeight, snapshotTitle
                            )
                        )
                    } else {
                        return@async false
                    }
                }
                deferredCommit.await()
                return@runCatching true
            } else {
                return@runCatching false
            }
        }.getOrNull()
    }

    private fun createPrepareSnapshotRevisionRequest(change: SnapshotMetadataChangeEntity, snapshotTitle: String): PrepareSnapshotRevisionRequest {
        val snapshotUpDateLink = mutableListOf<ukq>()
        snapshotUpDateLink.add(ukq.Builder().apply {
            unknownFileInt1 = SNAPSHOT_UPLOAD_LINK_DATA
            unknownFileInt2 = 1
        }.build())
        if (change.coverImageTeleporter != null) {
            snapshotUpDateLink.add(ukq.Builder().apply {
                unknownFileInt1 = SNAPSHOT_UPLOAD_LINK_IMAGE
                unknownFileInt2 = 1
            }.build())
        }
        val prepareSnapshotRevisionRequest = PrepareSnapshotRevisionRequest.Builder().apply {
            title = snapshotTitle
            c = snapshotUpDateLink
            randomUUID = UUID.randomUUID().toString()
        }.build()
        return prepareSnapshotRevisionRequest
    }

    private fun createCommitSnapshotRequest(
        change: SnapshotMetadataChangeEntity,
        snapshotDataResourceId: String,
        snapShotImageResourceId: String?,
        imageWidth: Int,
        imageHeight: Int,
        snapshotTitle: String
    ): CommitSnapshotRevisionRequest {
        Log.d(TAG, "createCommitSnapshotRequest: ")
        val snapshotTimeInfo = getSnapshotTimeInfo()
        val snapshotContent = SnapshotContent.Builder().apply {
            description = change.description
            this.snapshotTimeInfo = snapshotTimeInfo
            progressValue = change.progressValue ?: -1
            this.deviceName = Build.DEVICE
            duration = change.progressValue ?: -1
        }.build()
        val snapshotContentInfo = SnapshotContentInfo.Builder().apply {
            token = snapshotDataResourceId
        }.build()

        val snapshotImage: org.microg.gms.games.SnapshotImage? = if (!TextUtils.isEmpty(snapShotImageResourceId)) {
            org.microg.gms.games.SnapshotImage.Builder().apply {
                this.token = snapShotImageResourceId
                this.width = imageWidth
                this.height = imageHeight
            }.build()
        } else {
            null
        }

        val snapshotBuilder = org.microg.gms.games.Snapshot.Builder().apply {
            content = snapshotContent
            this.snapshotContentInfo = snapshotContentInfo
        }
        if (snapshotImage != null) {
            snapshotBuilder.coverImage = snapshotImage
        }
        val commitSnapshotRevisionRequest = CommitSnapshotRevisionRequest.Builder().apply {
            this.snapshotName = snapshotTitle
            this.snapshot = snapshotBuilder.build()
            this.randomUUID = UUID.randomUUID().toString()
            this.unknownFileInt7 = 3
        }.build()
        return commitSnapshotRevisionRequest
    }

    private suspend fun uploadSnapshotData(contents: Contents, oauthToken: String, snapshotDataUploadUrl: String): String? {
        Log.d(TAG, "uploadSnapshotData: $snapshotDataUploadUrl")
        val readInputStreamFully: ByteArray
        val fileInputStream = contents.inputStream
        val bufferedInputStream = BufferedInputStream(fileInputStream)
        var snapshotDataResourceId: String? = null
        try {
            fileInputStream.channel.position(0L)
            readInputStreamFully = IOUtils.readInputStreamFully(bufferedInputStream, false)
            fileInputStream.channel.position(0L)
            snapshotDataResourceId = uploadDataByUrl(oauthToken, snapshotDataUploadUrl, readInputStreamFully).getOrNull()
            fileInputStream.close()
        } catch (e: IOException) {
            Log.w("SnapshotContentsEntity", "Failed to read snapshot data", e)
        }
        return snapshotDataResourceId
    }

    private suspend fun uploadSnapshotImage(bitmap: Bitmap, maxCoverImageSize: Int, oauthToken: String, imageUploadUrl: String): Triple<Int, Int, String?> {
        Log.d(TAG, "uploadSnapshotImage imageUploadUrl: $imageUploadUrl")
        var snapshotBitmap = bitmap
        val bitmapSize = BitmapUtils.getBitmapSize(snapshotBitmap)
        if (bitmapSize > maxCoverImageSize) {
            Log.w(TAG, "commitSnapshot Snapshot cover image is too large. Currently at $bitmapSize bytes; max is $maxCoverImageSize Image will be scaled")
            snapshotBitmap = BitmapUtils.scaledBitmap(snapshotBitmap, maxCoverImageSize.toFloat())
            Log.d(TAG, "commitSnapshot scaledBitmap: ${snapshotBitmap.width} ${snapshotBitmap.height}")
        }
        Log.d(TAG, "commitSnapshot snapshotBitmap: $snapshotBitmap")
        val byteArrayOutputStream = ByteArrayOutputStream()
        snapshotBitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val snapShotImageResourceId = uploadDataByUrl(oauthToken, imageUploadUrl, byteArrayOutputStream.toByteArray()).getOrNull()
        snapshotBitmap.recycle()
        bitmap.recycle()
        withContext(Dispatchers.IO) {
            byteArrayOutputStream.close()
        }
        var imageWidth = snapshotBitmap.width
        var imageHeight = snapshotBitmap.height
        if (imageWidth > imageHeight) {
            val temp = imageWidth
            imageWidth = imageHeight
            imageHeight = temp
        }
        Log.d(TAG, "commitSnapshot $imageWidth: $imageHeight")
        return Triple(imageWidth, imageHeight, snapShotImageResourceId)
    }

    private fun getSnapshotTimeInfo(): SnapshotTimeInfo {
        val timestamp = System.currentTimeMillis()
        return SnapshotTimeInfo.Builder().apply {
            this.timestamp = timestamp / 1000
            this.playedTime = ((timestamp % 1000) * 1000000).toInt()
        }.build()
    }

    companion object {
        private const val TAG = "SnapshotsDataClient"

        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: SnapshotsDataClient? = null
        fun get(context: Context): SnapshotsDataClient = instance ?: synchronized(this) {
            instance ?: SnapshotsDataClient(context.applicationContext).also { instance = it }
        }
    }
}