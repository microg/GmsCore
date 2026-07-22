/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.games.snapshot

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.R
import com.google.android.gms.common.images.ImageManager
import org.microg.gms.games.EXTRA_ALLOW_DELETE_SNAPSHOT
import java.text.SimpleDateFormat
import java.util.Date

class SnapshotsAdapter(
    private val mContext: Context,
    private val callIntent: Intent,
    snapshots: List<Snapshot>,
    private val dealClick: (Snapshot, Int) -> Unit
) :
    RecyclerView.Adapter<SnapshotHolder>() {

    private val snapshotDataList = arrayListOf<Snapshot>()
    private val allowDelete = callIntent.getBooleanExtra(EXTRA_ALLOW_DELETE_SNAPSHOT, false)

    init {
        snapshotDataList.addAll(snapshots)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SnapshotHolder {
        val view = LayoutInflater.from(mContext).inflate(R.layout.item_snapshot_data_layout, parent, false)
        return SnapshotHolder(view)
    }

    override fun getItemCount(): Int {
        return snapshotDataList.size
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: SnapshotHolder, position: Int) {
        val snapshot = snapshotDataList[position]
        val imageUrl = snapshot.coverImage?.url
        if (imageUrl != null) {
            ImageManager.create(mContext).loadImage(imageUrl, holder.snapshotImage)
        } else {
            holder.snapshotImage?.setImageResource(R.drawable.ic_snapshot_load_error_image)
        }
        holder.snapshotTime?.text = snapshot.lastModifiedMillis?.let {
            var timestamp = it.toLong()
            //Check if the timestamp is greater than 10-digit threshold, indicating milliseconds
            timestamp = if (timestamp > 10000000000L) timestamp else timestamp * 1000
            SimpleDateFormat("yyyy/MM/dd HH:mm").format(Date(timestamp))
        }
        holder.snapshotDesc?.text = snapshot.description
        if (allowDelete) {
            holder.snapshotDeleteBtn?.visibility = View.VISIBLE
            holder.snapshotDeleteBtn?.setOnClickListener { dealClick(snapshot, 1) }
        } else{
            holder.snapshotDeleteBtn?.visibility = View.GONE
        }
        holder.snapshotChooseBtn?.setOnClickListener { dealClick(snapshot, 0) }
    }

    fun update(snapshots: List<Snapshot>) {
        snapshotDataList.clear()
        snapshotDataList.addAll(snapshots)
        notifyDataSetChanged()
    }

}

class SnapshotHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    var snapshotImage: ImageView? = null
    var snapshotTime: TextView? = null
    var snapshotDesc: TextView? = null
    var snapshotChooseBtn: TextView? = null
    var snapshotDeleteBtn: TextView? = null

    init {
        snapshotImage = itemView.findViewById(R.id.snapshot_image)
        snapshotTime = itemView.findViewById(R.id.snapshot_time)
        snapshotDesc = itemView.findViewById(R.id.snapshot_desc)
        snapshotChooseBtn = itemView.findViewById(R.id.snapshot_choose_btn)
        snapshotDeleteBtn = itemView.findViewById(R.id.snapshot_delete_btn)
    }

}