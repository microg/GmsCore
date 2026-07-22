/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.games.snapshot

import org.json.JSONArray
import org.json.JSONObject
import org.microg.gms.games.GameSnapshot

data class SnapshotsResponse(
    val kind: String?, val nextPageToken: String?, val items: List<Snapshot>?
) {
    override fun toString(): String {
        return "SnapshotsResponse(kind=$kind, nextPageToken=$nextPageToken, items=$items)"
    }
}

data class Snapshot(
    val id: String?,
    val driveId: String?,
    val kind: String?,
    val type: String?,
    val title: String?,
    val description: String?,
    val lastModifiedMillis: String?,
    val durationMillis: String?,
    val coverImage: SnapshotImage?,
    val uniqueName: String?,
    val progressValue: String?,
) {

    constructor(
        id: String?,
        title: String?,
        description: String?,
        lastModifiedMillis: String?,
        coverImage: SnapshotImage?
    ) : this(
        id, null, null, null, title, description, lastModifiedMillis, null, coverImage, null, null
    )

    override fun toString(): String {
        return "Snapshot(id=$id, driveId=$driveId, kind=$kind, type=$type, title=$title, description=$description, lastModifiedMillis=$lastModifiedMillis, durationMillis=$durationMillis, coverImage=$coverImage, uniqueName=$uniqueName, progressValue=$progressValue)"
    }
}

data class SnapshotImage(
    val width: Int?,
    val height: Int?,
    val mimeType: String?,
    val url: String?,
    val kind: String?,
) {
    override fun toString(): String {
        return "SnapshotImage(width=$width, height=$height, mimeType='$mimeType', url='$url', kind='$kind')"
    }
}

fun JSONObject.toSnapshotsResponse() = SnapshotsResponse(
    optString("kind"),
    optString("nextPageToken"),
    optJSONArray("items")?.toSnapshot()
)

fun JSONArray.toSnapshot(): List<Snapshot> {
    val snapshots = arrayListOf<Snapshot>()
    for (i in 0..<length()) {
        val jsonObject = optJSONObject(i)
        snapshots.add(
            Snapshot(
                jsonObject.optString("id"),
                jsonObject.optString("driveId"),
                jsonObject.optString("kind"),
                jsonObject.optString("type"),
                jsonObject.optString("title"),
                jsonObject.optString("description"),
                jsonObject.optString("lastModifiedMillis"),
                jsonObject.optString("durationMillis"),
                jsonObject.optJSONObject("coverImage")?.toSnapshotImage(),
                jsonObject.optString("uniqueName"),
                jsonObject.optString("progressValue"),
            )
        )
    }
    return snapshots
}

fun JSONObject.toSnapshotImage() = SnapshotImage(
    optInt("width"),
    optInt("height"),
    optString("mime_type"),
    optString("url"),
    optString("kind")
)

fun GameSnapshot.toSnapshot() = Snapshot(
    metadata?.snapshot?.snapshotId,
    metadata?.snapshotName,
    metadata?.snapshot?.content?.description,
    metadata?.snapshot?.content?.snapshotTimeInfo?.timestamp?.toString(),
    SnapshotImage(
        metadata?.snapshot?.coverImage?.width,
        metadata?.snapshot?.coverImage?.height,
        metadata?.snapshot?.coverImage?.mimeType,
        metadata?.snapshot?.coverImage?.imageUrl,
        null
    )
)