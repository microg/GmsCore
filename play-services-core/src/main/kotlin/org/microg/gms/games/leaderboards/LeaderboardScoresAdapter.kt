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

class LeaderboardScoresAdapter(
    private val mContext: Context,
    private val leaderboards: List<LeaderboardEntry>,
) : RecyclerView.Adapter<LeaderboardScoresHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeaderboardScoresHolder {
        val view = if (viewType == -1) {
            LayoutInflater.from(mContext).inflate(R.layout.item_leaderboard_score_header_layout, parent, false)
        } else {
            LayoutInflater.from(mContext).inflate(R.layout.item_leaderboard_score_data_layout, parent, false)
        }
        return LeaderboardScoresHolder(view, viewType)
    }

    override fun getItemCount(): Int {
        return leaderboards.size
    }

    override fun getItemViewType(position: Int): Int {
        val leaderboardEntry = leaderboards[position]
        return if (leaderboardEntry.kind == null) -1 else 0
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: LeaderboardScoresHolder, position: Int) {
        val leaderboardEntry = leaderboards[position]
        if (leaderboardEntry.kind == null) {
            val scoreTag = leaderboardEntry.scoreTag
            if (scoreTag != null) {
                ImageManager.create(mContext).loadImage(scoreTag, holder.leaderboardLogo)
            } else {
                holder.leaderboardLogo?.setImageResource(R.drawable.ic_leaderboard_placeholder)
            }
            holder.leaderboardTitle?.text = leaderboardEntry.scoreValue
            return
        }
        val player = leaderboardEntry.player
        val iconUrl = leaderboardEntry.player?.iconImageUrl
        if (iconUrl != null) {
            ImageManager.create(mContext).loadImage(iconUrl, holder.leaderboardPlayerLogo)
        } else {
            holder.leaderboardPlayerLogo?.setImageResource(R.drawable.ic_leaderboard_placeholder)
        }
        holder.leaderboardPlayerName?.text = player?.displayName
        holder.leaderboardPlayerScore?.text = mContext.getString(R.string.games_leaderboards_score_label, leaderboardEntry.formattedScore)
        holder.leaderboardPlayerRank?.text = leaderboardEntry.formattedScoreRank
        if (position == leaderboards.size - 1) {
            holder.leaderboardScoreLine?.visibility = View.INVISIBLE
        }
    }
}

class LeaderboardScoresHolder(itemView: View, viewType: Int) : RecyclerView.ViewHolder(itemView) {

    var leaderboardPlayerLogo: ImageView? = null
    var leaderboardPlayerName: TextView? = null
    var leaderboardPlayerScore: TextView? = null
    var leaderboardPlayerRank: TextView? = null
    var leaderboardScoreLine: View? = null

    var leaderboardTitle: TextView? = null
    var leaderboardLogo: ImageView? = null

    init {
        if (viewType == -1) {
            leaderboardTitle = itemView.findViewById(R.id.leaderboard_header_title)
            leaderboardLogo = itemView.findViewById(R.id.leaderboard_header_logo)
        } else {
            leaderboardPlayerLogo = itemView.findViewById(R.id.leaderboard_player_logo)
            leaderboardPlayerName = itemView.findViewById(R.id.leaderboard_player_name)
            leaderboardPlayerScore = itemView.findViewById(R.id.leaderboard_player_score)
            leaderboardPlayerRank = itemView.findViewById(R.id.leaderboard_player_rank)
            leaderboardScoreLine = itemView.findViewById(R.id.leaderboard_score_line)
        }
    }

}