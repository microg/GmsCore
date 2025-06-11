/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.games.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.google.android.gms.R

class GamePlayDataActivity : AppCompatActivity() {

    private val operationType: String?
        get() = runCatching {
            intent?.extras?.getString(OPERATION_TYPE)
        }.getOrNull()

    @SuppressLint("CommitTransaction")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: operationType: $operationType")
        if (operationType == null) {
            finish()
            return
        }

        var toolbarTitle = ""
        val fragment = if (TYPE_DELETED == operationType) {
            Log.d(TAG, "add GameDeletePlayAccountFragment")
            toolbarTitle = getString(R.string.pref_delete_game_account_data)
            GameDeletePlayAccountFragment.newInstance()
        } else {
            Log.d(TAG, "add GameChangeAccountFragment")
            toolbarTitle = getString(R.string.pref_change_default_game_player)
            GameChangeAccountFragment.newInstance()
        }

        val layout = RelativeLayout(this)
        val toolbar = Toolbar(this).apply {
            id = View.generateViewId()
            layoutParams = RelativeLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                addRule(RelativeLayout.ALIGN_PARENT_TOP)
            }
            navigationIcon = ContextCompat.getDrawable(context, R.drawable.ic_close_btn)
            setNavigationOnClickListener { finish() }
            title = toolbarTitle
        }
        val container = FrameLayout(this).apply {
            id = View.generateViewId()
            layoutParams = RelativeLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT).apply {
                addRule(RelativeLayout.BELOW, toolbar.id)
            }
        }
        layout.addView(toolbar)
        layout.addView(container)
        setContentView(layout)

        supportFragmentManager.beginTransaction().add(container.id, fragment, GameChangeAccountFragment.TAG).commitAllowingStateLoss()
    }

    companion object {
        private const val TAG = "GamePlayDataActivity"
        private const val OPERATION_TYPE = "operation_type"
        const val TYPE_CHANGED = "type_changed"
        const val TYPE_DELETED = "type_deleted"

        fun createIntent(context: Context, type: String, data: Bundle? = null) {
            Intent(context, GamePlayDataActivity::class.java).apply {
                putExtra(OPERATION_TYPE, type)
                data?.let { putExtras(it) }
            }.let {
                context.startActivity(it)
            }
        }
    }
}