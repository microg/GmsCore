/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.games.ui

import android.accounts.Account
import android.app.Activity.RESULT_OK
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.R
import com.google.android.gms.games.snapshot.SnapshotMetadataEntity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.microg.gms.auth.AuthManager
import org.microg.gms.auth.signin.SignInConfigurationService
import org.microg.gms.common.Constants
import org.microg.gms.games.ACTION_VIEW_ACHIEVEMENTS
import org.microg.gms.games.ACTION_VIEW_LEADERBOARDS
import org.microg.gms.games.ACTION_VIEW_LEADERBOARDS_SCORES
import org.microg.gms.games.ACTION_VIEW_SNAPSHOTS
import org.microg.gms.games.EXTRA_ACCOUNT_KEY
import org.microg.gms.games.EXTRA_ALLOW_CREATE_SNAPSHOT
import org.microg.gms.games.EXTRA_GAME_PACKAGE_NAME
import org.microg.gms.games.EXTRA_LEADERBOARD_ID
import org.microg.gms.games.EXTRA_MAX_SNAPSHOTS
import org.microg.gms.games.EXTRA_SNAPSHOT_METADATA
import org.microg.gms.games.EXTRA_SNAPSHOT_NEW
import org.microg.gms.games.EXTRA_TITLE
import org.microg.gms.games.GamesConfigurationService
import org.microg.gms.games.SERVICE_GAMES_LITE
import org.microg.gms.games.achievements.AchievementDefinition
import org.microg.gms.games.achievements.AchievementState
import org.microg.gms.games.achievements.AchievementsAdapter
import org.microg.gms.games.achievements.AchievementsApiClient
import org.microg.gms.games.achievements.PlayerAchievement
import org.microg.gms.games.achievements.getAchievementState
import org.microg.gms.games.leaderboards.LeaderboardDefinition
import org.microg.gms.games.leaderboards.LeaderboardEntry
import org.microg.gms.games.leaderboards.LeaderboardScoresAdapter
import org.microg.gms.games.leaderboards.LeaderboardsAdapter
import org.microg.gms.games.leaderboards.LeaderboardsApiClient
import org.microg.gms.games.snapshot.Snapshot
import org.microg.gms.games.snapshot.SnapshotsAdapter
import org.microg.gms.games.snapshot.SnapshotsDataClient
import org.microg.gms.people.PeopleManager
import org.microg.gms.profile.ProfileManager

private const val TAG = "GamesUiFragment"

class GamesUiFragment(
    private val clientPackageName: String,
    private val account: Account?,
    private val callerIntent: Intent,
) : BottomSheetDialogFragment() {

    private var playerLogo: ImageView? = null
    private var uiTitle: TextView? = null
    private var actionBtn: FloatingActionButton? = null
    private var refreshBtn: ImageView? = null
    private var cancelBtn: ImageView? = null
    private var recyclerView: RecyclerView? = null
    private var loadingView: FrameLayout? = null
    private var errorView: TextView? = null
    private var contentVb: ViewStub? = null

    private var currentAccount: Account? = null
    private var snapshotsAdapter: SnapshotsAdapter? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        ProfileManager.ensureInitialized(requireContext())
        lifecycleScope.launch {
            currentAccount =
                account ?: GamesConfigurationService.getDefaultAccount(requireContext(), clientPackageName) ?: SignInConfigurationService.getDefaultAccount(
                    requireContext(), clientPackageName
                )

            if (currentAccount == null) {
                showErrorMsg(requireContext().getString(R.string.games_api_access_denied))
                return@launch
            }

            withContext(Dispatchers.IO) {
                PeopleManager.getOwnerAvatarBitmap(
                    requireContext(), currentAccount!!.name, false
                )
            }?.also {
                playerLogo?.setImageBitmap(it)
            }

            val authResponse = withContext(Dispatchers.IO) {
                AuthManager(context, currentAccount!!.name, clientPackageName, SERVICE_GAMES_LITE).apply { isPermitted = true }.requestAuth(true)
            }
            var oauthToken: String? = null
            if (authResponse.auth?.let { oauthToken = it } == null) {
                showErrorMsg(requireContext().getString(R.string.games_achievements_empty_text))
                return@launch
            }

            runCatching {
                when (callerIntent.action) {
                    ACTION_VIEW_ACHIEVEMENTS -> loadLocalAchievements(oauthToken!!)
                    ACTION_VIEW_LEADERBOARDS -> loadLocalLeaderboards(oauthToken!!)
                    ACTION_VIEW_SNAPSHOTS -> loadSnapshots(oauthToken!!)
                    ACTION_VIEW_LEADERBOARDS_SCORES -> loadLocalLeaderboardScores(oauthToken!!)
                    else -> {
                        showErrorMsg("Not yet implemented")
                    }
                }
            }.onFailure {
                Log.d(TAG, "show error: ", it)
                activity?.finish()
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
            dialog.behavior.skipCollapsed = true
            dialog.setCanceledOnTouchOutside(false)
        }
        dialog.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                dialog.dismiss()
                return@setOnKeyListener true
            }
            return@setOnKeyListener false
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG, "onCreateView")
        return layoutInflater.inflate(R.layout.fragment_games_ui_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated")
        uiTitle = view.findViewById(R.id.games_ui_title)
        actionBtn = view.findViewById(R.id.games_ui_action_button)
        refreshBtn = view.findViewById(R.id.games_ui_refresh)
        cancelBtn = view.findViewById(R.id.games_ui_cancel)
        cancelBtn?.setOnClickListener { dismiss() }
        recyclerView = view.findViewById(R.id.games_ui_recyclerview)
        loadingView = view.findViewById(R.id.games_ui_loading)
        playerLogo = view.findViewById(R.id.games_ui_player_logo)
        errorView = view.findViewById(R.id.games_ui_error_tips)
        contentVb = view.findViewById(R.id.games_ui_achievements_vb)
    }

    override fun onDismiss(dialog: DialogInterface) {
        activity?.finish()
        super.onDismiss(dialog)
    }

    private fun showErrorMsg(error: String) {
        loadingView?.visibility = View.GONE
        recyclerView?.visibility = View.GONE
        errorView?.visibility = View.VISIBLE
        errorView?.text = error
    }

    private suspend fun loadSnapshots(oauthToken: String) {
        uiTitle?.text = callerIntent.getStringExtra(EXTRA_TITLE)
        refreshBtn?.visibility = View.VISIBLE
        refreshBtn?.setOnClickListener {
            errorView?.visibility = View.GONE
            loadingView?.visibility = View.VISIBLE
            lifecycleScope.launchWhenCreated {
                val snapshots = withContext(Dispatchers.IO) {
                    SnapshotsDataClient.get(requireContext()).loadSnapshotData(oauthToken)
                }
                if (snapshots.isEmpty()) {
                    showErrorMsg(requireContext().getString(R.string.games_snapshot_empty_text))
                } else {
                    snapshotsAdapter?.update(snapshots)
                }
                addSnapshotBtnDetail(snapshots)
            }
        }
        val snapshots = withContext(Dispatchers.IO) {
            SnapshotsDataClient.get(requireContext()).loadSnapshotData(oauthToken)
        }
        addSnapshotBtnDetail(snapshots)
        if (snapshots.isEmpty()) {
            showErrorMsg(requireContext().getString(R.string.games_snapshot_empty_text))
            return
        }
        recyclerView?.apply {
            layoutManager = LinearLayoutManager(requireContext())
            addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                errorView?.visibility = View.GONE
                loadingView?.visibility = View.GONE
            }
        }?.adapter = snapshotsAdapter ?: SnapshotsAdapter(requireContext(), callerIntent, snapshots) { snapshot, i ->
            lifecycleScope.launch {
                if (i == 0) {
                    val intent = Intent()
                    val snapshotMetadataEntity = SnapshotMetadataEntity(
                        null, null, snapshot.id, null,
                        snapshot.coverImage?.url, snapshot.title, snapshot.description, snapshot.lastModifiedMillis?.toLong() ?: 0,
                        0, 1f, snapshot.title, false, 0, ""
                    )
                    intent.putExtra(EXTRA_SNAPSHOT_METADATA, snapshotMetadataEntity)
                    activity?.setResult(RESULT_OK, intent)
                    activity?.finish()
                } else {
                    AlertDialog.Builder(requireContext()).apply {
                        setTitle(getString(R.string.games_delete_snapshot_dialog_title))
                        setMessage(getString(R.string.games_delete_snapshot_dialog_message))
                    }.setNegativeButton(getString(R.string.games_delete_snapshot_dialog_cancel)) { dialog, _ ->
                        dialog.dismiss()
                    }.setPositiveButton(getString(R.string.games_delete_snapshot_dialog_ok)) { dialog, _ ->
                        dialog.dismiss()
                        lifecycleScope.launchWhenCreated {
                            val snapshotData = SnapshotsDataClient.get(requireContext()).deleteSnapshotData(oauthToken, snapshot)
                            if (snapshotData != null) {
                                refreshBtn?.performClick()
                            } else {
                                Toast.makeText(requireContext(), getString(R.string.games_delete_snapshot_error), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }.show()
                }
            }
        }.also {
            snapshotsAdapter = it
        }
    }

    private fun addSnapshotBtnDetail(snapshots: List<Snapshot>) {
        val allowCreate = callerIntent.getBooleanExtra(EXTRA_ALLOW_CREATE_SNAPSHOT, true)
        val maxSnapshot = callerIntent.getIntExtra(EXTRA_MAX_SNAPSHOTS, -1)
        if (allowCreate && (maxSnapshot != -1 && snapshots.size < maxSnapshot)) {
            actionBtn?.visibility = View.VISIBLE
            actionBtn?.setOnClickListener {
                val resultIntent = Intent()
                resultIntent.putExtra(EXTRA_SNAPSHOT_NEW, true)
                activity?.setResult(RESULT_OK, resultIntent)
                activity?.finish()
            }
        } else {
            actionBtn?.visibility = View.INVISIBLE
        }
    }

    private suspend fun loadLocalLeaderboards(oauthToken: String) {
        uiTitle?.text = requireContext().getString(R.string.games_leaderboard_list_title)
        val loadLeaderboards = withContext(Dispatchers.IO) {
            ArrayList<LeaderboardDefinition>().apply {
                var playerPageToken: String? = null
                do {
                    val response = LeaderboardsApiClient.requestAllLeaderboards(requireContext(), oauthToken, playerPageToken)
                    addAll(response.items)
                    playerPageToken = response.nextPageToken
                } while (!playerPageToken.isNullOrEmpty())
            }
        }
        if (loadLeaderboards.isEmpty()) {
            showErrorMsg(requireContext().getString(R.string.games_leaderboard_empty_text))
            return
        }
        recyclerView?.apply {
            layoutManager = LinearLayoutManager(requireContext())
            addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                loadingView?.visibility = View.GONE
            }
        }?.adapter = LeaderboardsAdapter(requireContext(), loadLeaderboards) { leaderboard ->
            val intent = Intent(ACTION_VIEW_LEADERBOARDS_SCORES)
            intent.setPackage(Constants.GMS_PACKAGE_NAME)
            intent.putExtra(EXTRA_GAME_PACKAGE_NAME, clientPackageName)
            intent.putExtra(EXTRA_ACCOUNT_KEY, Integer.toHexString(currentAccount?.name.hashCode()))
            intent.putExtra(EXTRA_LEADERBOARD_ID, leaderboard.id)
            activity?.startActivity(intent)
        }
    }

    private suspend fun loadLocalLeaderboardScores(oauthToken: String) {
        val leaderboardId = callerIntent.getStringExtra(EXTRA_LEADERBOARD_ID)
        val leaderboardScores = withContext(Dispatchers.IO) {
            ArrayList<LeaderboardEntry>().apply {
                val response = LeaderboardsApiClient.requestLeaderboardScoresById(
                    requireContext(), oauthToken, leaderboardId!!, null
                )
                addAll(response.items)
            }
        }
        if (leaderboardScores.isEmpty()) {
            showErrorMsg(requireContext().getString(R.string.games_leaderboard_empty_text))
            return
        }
        val leaderboardDefinition = withContext(Dispatchers.IO) {
            LeaderboardsApiClient.getLeaderboardById(requireContext(), oauthToken, leaderboardId!!)
        }
        val leaderboardEntries = arrayListOf<LeaderboardEntry>()
        leaderboardEntries.add(LeaderboardEntry(leaderboardDefinition.name, leaderboardDefinition.iconUrl))
        leaderboardEntries.addAll(leaderboardScores)
        recyclerView?.apply {
            layoutManager = LinearLayoutManager(requireContext())
            addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                loadingView?.visibility = View.GONE
            }
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val layoutManager = recyclerView.layoutManager
                    if (layoutManager is LinearLayoutManager) {
                        val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                        uiTitle?.text = if (firstVisibleItemPosition != 0) leaderboardDefinition.name else ""
                    }
                }
            })
        }?.adapter = LeaderboardScoresAdapter(requireContext(), leaderboardEntries)
    }

    private suspend fun loadLocalAchievements(oauthToken: String) {
        uiTitle?.text = requireContext().getString(R.string.games_achievement_list_title)
        val allAchievements = withContext(Dispatchers.IO) {
            ArrayList<AchievementDefinition>().apply {
                val playerAchievements = ArrayList<PlayerAchievement>()
                var playerPageToken: String? = null
                do {
                    val response = AchievementsApiClient.requestPlayerAllAchievements(requireContext(), oauthToken, playerPageToken)
                    playerAchievements.addAll(response.items)
                    playerPageToken = response.nextPageToken
                } while (!playerPageToken.isNullOrEmpty())

                var pageToken: String? = null
                do {
                    val response = AchievementsApiClient.requestGameAllAchievements(requireContext(), oauthToken, pageToken)
                    response.items.forEach { item ->
                        if (playerAchievements.any { it.id == item.id }) {
                            item.initialState = getAchievementState(playerAchievements.find { it.id == item.id }?.achievementState)
                        }
                        add(item)
                    }
                    pageToken = response.nextPageToken
                } while (!pageToken.isNullOrEmpty())
            }
        }
        if (allAchievements.isEmpty()) {
            showErrorMsg(requireContext().getString(R.string.games_achievements_empty_text))
            return
        }
        val targetList = ArrayList<AchievementDefinition>()
        val unlockList = ArrayList<AchievementDefinition>()
        val revealedList = ArrayList<AchievementDefinition>()
        for (definition in allAchievements) {
            when (definition.initialState) {
                AchievementState.STATE_REVEALED -> {
                    revealedList.add(definition)
                }

                AchievementState.STATE_UNLOCKED -> {
                    unlockList.add(definition)
                }
            }
        }
        if (unlockList.isNotEmpty()) {
            targetList.add(AchievementDefinition(requireContext().getString(R.string.games_achievement_unlocked_content_description), -1))
            targetList.addAll(unlockList)
        }
        if (revealedList.isNotEmpty()) {
            targetList.add(AchievementDefinition(requireContext().getString(R.string.games_achievement_locked_content_description), -1))
            targetList.addAll(revealedList)
        }
        val inflatedView = contentVb?.inflate()
        inflatedView?.findViewById<TextView>(R.id.achievements_counter_text)?.text =
            String.format("${unlockList.size} / ${unlockList.size + revealedList.size}")
        recyclerView?.apply {
            layoutManager = LinearLayoutManager(requireContext())
            inflatedView?.id?.let {
                val layoutParams = layoutParams as RelativeLayout.LayoutParams
                layoutParams.addRule(RelativeLayout.BELOW, it)
                setLayoutParams(layoutParams)
            }
            addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                loadingView?.visibility = View.GONE
            }
        }?.adapter = AchievementsAdapter(requireContext(), targetList)
    }

}