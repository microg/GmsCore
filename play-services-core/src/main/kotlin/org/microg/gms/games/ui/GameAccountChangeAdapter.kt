/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.games.ui

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.R

class GameAccountChangeAdapter(
    private val items: List<GameItem>, private val onChangeClick: (GameItem) -> Unit
) : RecyclerView.Adapter<GameAccountChangeAdapter.GameAccountChangeViewHolder>() {

    inner class GameAccountChangeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.img_game_icon)
        val name: TextView = view.findViewById(R.id.tv_game_name)
        val tips: TextView = view.findViewById(R.id.tv_game_tips)
        val btn: Button = view.findViewById(R.id.btn_click)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GameAccountChangeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_game_account_data, parent, false)
        return GameAccountChangeViewHolder(view)
    }

    override fun onBindViewHolder(holder: GameAccountChangeViewHolder, position: Int) {
        val item = items[position]
        holder.icon.setImageDrawable(item.icon)
        holder.name.text = item.gameName
        holder.tips.text = item.defaultAccount ?: holder.itemView.context.getString(R.string.games_state_description_signed_out)
        holder.btn.text = holder.itemView.context.getString(R.string.games_change_button_text)
        holder.btn.setOnClickListener { onChangeClick(item) }
    }

    override fun getItemCount(): Int = items.size
}

data class GameItem(
    val icon: Drawable?, val gameName: String?, val defaultAccount: String?, val gamePackageName: String? = null
)
