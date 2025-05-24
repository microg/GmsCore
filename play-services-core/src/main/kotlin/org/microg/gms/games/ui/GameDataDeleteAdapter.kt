/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.games.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.common.images.ImageManager
import com.google.android.gms.R

class GameDataDeleteAdapter(
    private val items: List<GameDataDeleteItem>, private val onChangeClick: (GameDataDeleteItem) -> Unit
) : RecyclerView.Adapter<GameDataDeleteAdapter.GameDataDeleteViewHolder>() {

    inner class GameDataDeleteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.img_game_icon)
        val name: TextView = view.findViewById(R.id.tv_game_name)
        val tips: TextView = view.findViewById(R.id.tv_game_tips)
        val btn: Button = view.findViewById(R.id.btn_click)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GameDataDeleteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_game_account_data, parent, false)
        return GameDataDeleteViewHolder(view)
    }

    override fun onBindViewHolder(holder: GameDataDeleteViewHolder, position: Int) {
        val item = items[position]
        ImageManager.create(holder.icon.context).loadImage(item.iconUrl, holder.icon)
        holder.name.text = item.gameName
        holder.tips.text = item.tips
        holder.btn.text = holder.itemView.context.getString(R.string.games_state_description_delete)
        holder.btn.setOnClickListener { onChangeClick(item) }
    }

    override fun getItemCount(): Int = items.size
}

data class GameDataDeleteItem(
    val iconUrl: String?, val gameName: String?, val gameId: String?, val tips: String?
)