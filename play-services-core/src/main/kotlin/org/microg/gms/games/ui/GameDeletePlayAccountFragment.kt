/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.games.ui

import android.accounts.Account
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.R
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.Scope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.squareup.wire.GrpcClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.microg.gms.games.ApplicationsFirstPartyClient
import org.microg.gms.games.DeletePlayerRequest
import org.microg.gms.games.GamesConfigurationService
import org.microg.gms.games.HeaderInterceptor
import org.microg.gms.games.ListApplicationsWithUserDataRequest
import org.microg.gms.games.PlayersFirstPartyClient
import org.microg.gms.games.requestGameToken
import java.lang.RuntimeException
import android.os.Build.VERSION.SDK_INT
import androidx.recyclerview.widget.LinearLayoutManager
import org.microg.gms.games.DeleteApplicationDataRequest
import org.microg.gms.games.GAMES_PACKAGE_NAME
import org.microg.gms.games.fetchAllSelfPlayers
import java.util.Locale

class GameDeletePlayAccountFragment : Fragment() {

    companion object {
        const val TAG = "GameDeletePlayAccount"

        fun newInstance(): GameDeletePlayAccountFragment {
            val fragment = GameDeletePlayAccountFragment()
            return fragment
        }
    }

    private var lastChoosePlayer: Pair<Account, String>? = null
    private var isDeleteRefreshState = false
    private lateinit var currentAccountText: TextView
    private lateinit var currentChooseBtn: Button
    private lateinit var currentDeleteBtn: Button
    private lateinit var loadingBar: ProgressBar
    private lateinit var individualGameRecyclerView: RecyclerView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return layoutInflater.inflate(R.layout.fragment_game_delete_data, container, false);
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        currentAccountText = view.findViewById<TextView>(R.id.delete_current_account) ?: currentAccountText
        currentChooseBtn = view.findViewById<Button>(R.id.btn_choose) ?: currentChooseBtn
        currentDeleteBtn = view.findViewById<Button>(R.id.btn_delete) ?: currentDeleteBtn
        loadingBar = view.findViewById<ProgressBar>(R.id.loading_progress) ?: loadingBar
        individualGameRecyclerView = view.findViewById<RecyclerView>(R.id.recycler_view_games) ?: individualGameRecyclerView

        lifecycleScope.launchWhenStarted {
            initPlayers()
            deletePlayer()
            loadIndividualGames()
        }
    }

    private fun loadIndividualGames() {
        lifecycleScope.launchWhenStarted {
            val response = withContext(Dispatchers.IO) {
                val scopes = arrayListOf(Scope(Scopes.GAMES_LITE), Scope(Scopes.GAMES_FIRSTPARTY))
                val authToken = requestGameToken(requireContext(), lastChoosePlayer!!.first, scopes) ?: throw RuntimeException("authToken is null")
                getApplicationsFirstPartyClient(requireContext(), authToken).ListApplicationsWithUserDataFirstParty().execute(ListApplicationsWithUserDataRequest.build {
                    locale = Locale.getDefault().language
                    androidSdk = "android:${SDK_INT}"
                })
            }
            val dataList = response.firstPartyApplication.map {
                GameDataDeleteItem(
                    iconUrl = it.application?.gameIcon?.url,
                    gameName = it.application?.gameName,
                    gameId = it.application?.gameId,
                    tips = "${it.unlockAchievementsNum} / ${it.application?.achievementsNum} ${requireContext().getString(R.string.games_achievement_list_title)}"
                )
            }
            individualGameRecyclerView.layoutManager = LinearLayoutManager(requireContext())
            individualGameRecyclerView.adapter = GameDataDeleteAdapter(dataList) { item ->
                showDeleteConfirmationDialog(item)
            }
            individualGameRecyclerView.visibility = View.VISIBLE
            loadingBar.visibility = View.GONE
        }
    }

    private fun deleteGameDataByGameId(itemGameId: String?) {
        try {
            if (lastChoosePlayer == null) throw RuntimeException("player is null")
            val applicationId = itemGameId ?: throw RuntimeException("gameId is null")
            lifecycleScope.launchWhenStarted {
                individualGameRecyclerView.visibility = View.INVISIBLE
                loadingBar.visibility = View.VISIBLE
                withContext(Dispatchers.IO) {
                    val scopes = arrayListOf(Scope(Scopes.GAMES_LITE), Scope(Scopes.GAMES_FIRSTPARTY))
                    val authToken = requestGameToken(requireContext(), lastChoosePlayer!!.first, scopes) ?: throw RuntimeException("authToken is null")
                    getPlayersFirstPartyClient(requireContext(), authToken).DeleteApplicationDataFirstParty().execute(DeleteApplicationDataRequest.build {
                        gameId = applicationId
                        status = 0
                    })
                }
                loadIndividualGames()
            }
        } catch (e: Exception) {
            Log.d(TAG, "deleteGameDataByGameId: ", e)
            Toast.makeText(requireContext(), requireContext().getString(R.string.games_delete_profile_fail), Toast.LENGTH_SHORT).show()
        }
    }

    private fun deletePlayer() {
        currentDeleteBtn.setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }

    private fun showDeleteConfirmationDialog(item: GameDataDeleteItem? = null) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_game_account_delete_confirmation, null)

        val dialog = BottomSheetDialog(requireContext())
        dialog.setContentView(dialogView)
        dialog.show()

        if (item != null) {
            dialogView.findViewById<TextView>(R.id.textDeleteProfileTitle).text = String.format(
                requireContext().getString(R.string.games_delete_game_data_confirm_dialog_title), item.gameName
            )
            dialogView.findViewById<TextView>(R.id.textDeleteProfileExplanation).text = String.format(
                requireContext().getString(R.string.games_delete_game_data_confirm_dialog_message), item.gameName
            )
        } else if (lastChoosePlayer != null) {
            dialogView.findViewById<TextView>(R.id.textDeleteProfileExplanation).text = HtmlCompat.fromHtml(
                String.format(
                    requireContext().getString(R.string.games_delete_profile_explanation_with_gamer_name), lastChoosePlayer?.second, lastChoosePlayer?.first?.name
                ), HtmlCompat.FROM_HTML_MODE_LEGACY
            )
        }

        dialogView.findViewById<Button>(R.id.btnConfirmDelete).setOnClickListener {
            if (item != null) {
                deleteGameDataByGameId(item.gameId)
            } else if (lastChoosePlayer != null) {
                deleteAccountData(lastChoosePlayer?.first!!)
            }
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun deleteAccountData(account: Account) {
        lifecycleScope.launchWhenStarted {
            try {
                withContext(Dispatchers.IO) {
                    val scopes = arrayListOf(Scope(Scopes.GAMES_LITE), Scope(Scopes.GAMES_FIRSTPARTY))
                    val authToken = requestGameToken(requireContext(), account, scopes) ?: throw RuntimeException("authToken is null")
                    getPlayersFirstPartyClient(requireContext(), authToken).DeletePlayerFirstParty().execute(DeletePlayerRequest())
                }
                val defaultAccount = GamesConfigurationService.getDefaultAccount(requireContext(), GAMES_PACKAGE_NAME)
                if (account.name == defaultAccount?.name) {
                    GamesConfigurationService.setDefaultAccount(requireContext(), GAMES_PACKAGE_NAME, null)
                }
                GamesConfigurationService.setPlayer(requireContext(), account, null)
                isDeleteRefreshState = true
                initPlayers()
                loadIndividualGames()
            } catch (e: Exception) {
                Log.d(TAG, "deleteAccountData: ", e)
                Toast.makeText(requireContext(), requireContext().getString(R.string.games_delete_profile_fail), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun initPlayers() {
        val players = fetchAllSelfPlayers(requireContext())
        if (players.isEmpty()) {
            requireActivity().finish()
            return
        } else if (players.size > 1) {
            handleMultiPlayer(players)
        } else {
            currentChooseBtn.visibility = View.GONE
        }
        val currentPlayer = players.find { it.first.name == lastChoosePlayer?.first?.name } ?: players.first()
        currentAccountText.visibility = View.VISIBLE
        currentAccountText.text = String.format(
            requireContext().getString(R.string.games_account_display_content), "${currentPlayer.first.name} (${currentPlayer.second})"
        )
        lastChoosePlayer = currentPlayer
        isDeleteRefreshState = false
    }

    private fun handleMultiPlayer(players: List<Pair<Account, String>>) {
        currentChooseBtn.visibility = View.VISIBLE
        currentChooseBtn.setOnClickListener {
            AlertDialog.Builder(requireContext()).setTitle(requireContext().getString(R.string.credentials_assisted_choose_account_label)).setItems(players.map {
                "${it.first.name} (${it.second})"
            }.toTypedArray()) { _, which ->
                loadingBar.visibility = View.VISIBLE
                individualGameRecyclerView.visibility = View.INVISIBLE
                lastChoosePlayer = players[which]
                val (account, playName) = players[which]
                currentAccountText.text = String.format(requireContext().getString(R.string.games_account_display_content), "${account.name} (${playName})")
                loadIndividualGames()
            }.setNegativeButton(requireContext().getString(R.string.games_delete_snapshot_dialog_cancel), null).show()
        }
    }

    private fun getPlayersFirstPartyClient(context: Context, oauthToken: String): PlayersFirstPartyClient {
        val client = OkHttpClient().newBuilder().addInterceptor(
            HeaderInterceptor(context, oauthToken)
        ).build()
        val grpcClient = GrpcClient.Builder().client(client).baseUrl("https://gameswhitelisted.googleapis.com").build()
        return grpcClient.create(PlayersFirstPartyClient::class)
    }

    private fun getApplicationsFirstPartyClient(context: Context, oauthToken: String): ApplicationsFirstPartyClient {
        val client = OkHttpClient().newBuilder().addInterceptor(
            HeaderInterceptor(context, oauthToken)
        ).build()
        val grpcClient = GrpcClient.Builder().client(client).baseUrl("https://gameswhitelisted.googleapis.com").build()
        return grpcClient.create(ApplicationsFirstPartyClient::class)
    }
}