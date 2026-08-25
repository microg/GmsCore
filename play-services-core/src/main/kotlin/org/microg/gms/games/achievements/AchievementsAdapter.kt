/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.games.achievements

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

class AchievementsAdapter(
    private val mContext: Context,
    private val achievements: List<AchievementDefinition>
) :
    RecyclerView.Adapter<AchievementsHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AchievementsHolder {
        val itemView = if (viewType == -1) {
            LayoutInflater.from(mContext).inflate(R.layout.item_achievement_header_layout, null)
        } else {
            LayoutInflater.from(mContext).inflate(R.layout.item_achievement_data_layout, null)
        }
        return AchievementsHolder(itemView, viewType)
    }

    override fun getItemCount(): Int {
        return achievements.size
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: AchievementsHolder, position: Int) {
        val definition = achievements[position]
        if (definition.achievementType == -1) {
            holder.headerView?.text = definition.name
        } else {
            holder.achievementTitle?.text = definition.name
            holder.achievementDesc?.text = definition.description
            holder.achievementContent?.text = mContext.getString(
                R.string.games_achievement_extra_text,
                definition.experiencePoints
            )
            val imageUrl =
                if (definition.initialState == AchievementState.STATE_UNLOCKED) {
                    definition.unlockedIconUrl
                } else {
                    definition.revealedIconUrl
                }
            if (imageUrl != null) {
                ImageManager.create(mContext).loadImage(imageUrl, holder.achievementLogo)
            } else {
                val logoId = if (definition.initialState == AchievementState.STATE_UNLOCKED) {
                    R.drawable.ic_achievement_unlocked
                } else {
                    R.drawable.ic_achievement_locked
                }
                holder.achievementLogo?.setImageResource(logoId)
            }
        }
    }

    /**
     * There are two display types.
     * Now only the unlock display type is displayed, and the progress display type is not displayed yet.
     */
    override fun getItemViewType(position: Int): Int {
        val definition = achievements[position]
        return definition.achievementType
    }

}

class AchievementsHolder(itemView: View, viewType: Int) : RecyclerView.ViewHolder(itemView) {

    var headerView: TextView? = null

    var achievementLogo: ImageView? = null
    var achievementTitle: TextView? = null
    var achievementContent: TextView? = null
    var achievementDesc: TextView? = null

    init {
        if (viewType == -1) {
            headerView = itemView.findViewById(R.id.achievements_header_title)
        } else {
            achievementLogo = itemView.findViewById(R.id.achievement_logo)
            achievementTitle = itemView.findViewById(R.id.achievement_title)
            achievementContent = itemView.findViewById(R.id.achievement_content)
            achievementDesc = itemView.findViewById(R.id.achievement_desc)
        }
    }

}