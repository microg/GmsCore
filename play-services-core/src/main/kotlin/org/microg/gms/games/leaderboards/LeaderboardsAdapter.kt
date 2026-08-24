/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.games.leaderboards

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.R
import com.google.android.gms.common.images.ImageManager

class LeaderboardsAdapter(
    private val mContext: Context, private val leaderboards: List<LeaderboardDefinition>, private val dealClick: (LeaderboardDefinition) -> Unit
) : RecyclerView.Adapter<LeaderboardHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeaderboardHolder {
        val view = LayoutInflater.from(mContext).inflate(R.layout.item_leaderboard_data_layout, parent, false)
        return LeaderboardHolder(view)
    }

    override fun getItemCount(): Int {
        return leaderboards.size
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: LeaderboardHolder, position: Int) {
        val leaderboardDefinition = leaderboards[position]
        val iconUrl = leaderboardDefinition.iconUrl
        if (iconUrl != null) {
            ImageManager.create(mContext).loadImage(iconUrl, holder.leaderboardLogo)
        } else {
            holder.leaderboardLogo?.setImageResource(R.drawable.ic_leaderboard_placeholder)
        }
        holder.leaderboardTitle?.text = leaderboardDefinition.name
        if (position == leaderboards.size - 1) {
            holder.leaderboardLine?.visibility = View.INVISIBLE
        }
        holder.itemView.setOnClickListener { dealClick(leaderboardDefinition) }
    }
}

class LeaderboardHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    var leaderboardLogo: ImageView? = null
    var leaderboardTitle: TextView? = null
    var leaderboardLine: View? = null

    init {
        leaderboardLogo = itemView.findViewById(R.id.leaderboard_logo)
        leaderboardTitle = itemView.findViewById(R.id.leaderboard_title)
        leaderboardLine = itemView.findViewById(R.id.leaderboard_line)
    }

}