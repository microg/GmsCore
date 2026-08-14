package org.microg.gms.account

import android.content.Context
import android.graphics.drawable.Drawable
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.google.android.gms.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.listitem.ListItemCardView
import com.google.android.material.listitem.ListItemLayout
import com.google.android.material.listitem.SwipeableListItem

class AccountPreference(context: Context) : Preference(context) {

    var accountAvatar: Drawable? = null
        set(value) {
            field = value; notifyChanged()
        }

    var position: Int = 0
    var itemCount: Int = 0
    var onRemoveListener: (() -> Unit)? = null

    init {
        layoutResource = R.layout.account_item_list
        isSelectable = false
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        val layout = holder.itemView as? ListItemLayout ?: return
        val card = holder.findViewById(R.id.account_list_card) as? ListItemCardView
        val avatarView = holder.findViewById(R.id.account_avatar) as? ShapeableImageView
        val nameView = holder.findViewById(R.id.account_name) as? TextView
        val emailView = holder.findViewById(R.id.account_email) as? TextView
        val actionRemove = holder.findViewById(R.id.account_remove) as? MaterialButton

        layout.updateAppearance(position, itemCount)

        nameView?.text = title
        emailView?.text = summary
        avatarView?.setImageDrawable(accountAvatar)

        card?.setOnClickListener {
            layout.swipeState = if (layout.swipeState == SwipeableListItem.STATE_CLOSED) {
                SwipeableListItem.STATE_OPEN
            } else {
                SwipeableListItem.STATE_CLOSED
            }
        }

        actionRemove?.setOnClickListener {
            onRemoveListener?.invoke()
            layout.swipeState = SwipeableListItem.STATE_CLOSED
        }
    }
}
