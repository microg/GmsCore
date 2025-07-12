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
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatRadioButton
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.R
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import org.json.JSONObject
import org.microg.gms.games.GAMES_PACKAGE_NAME
import org.microg.gms.games.GamesConfigurationService
import org.microg.gms.games.fetchAllSelfPlayers
import org.microg.gms.games.toPlayer

class GameChangeAccountFragment : Fragment() {

    companion object {
        const val TAG = "GameChangeAccount"

        fun newInstance(): GameChangeAccountFragment {
            val fragment = GameChangeAccountFragment()
            return fragment
        }
    }

    private lateinit var defaultAccountText: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var changeDefaultBtn: Button

    private var currentPlayer: Pair<Account, String>? = null
    private var allPlayers: List<Pair<Account, String>> = emptyList()
    private var playedGames: List<String> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return layoutInflater.inflate(R.layout.fragment_game_change_account, container, false);
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        defaultAccountText = view.findViewById(R.id.change_default_account) ?: defaultAccountText
        recyclerView = view.findViewById(R.id.recycler_view_games) ?: recyclerView
        changeDefaultBtn = view.findViewById(R.id.btn_change_default) ?: changeDefaultBtn

        lifecycleScope.launchWhenCreated {
            allPlayers = fetchAllSelfPlayers(requireContext())
            if (allPlayers.isEmpty()) {
                requireActivity().finish()
                return@launchWhenCreated
            }
            recyclerView.layoutManager = LinearLayoutManager(requireContext())
            changeDefaultBtn.setOnClickListener { showChangeCurrentConfirmationDialog() }
            initDefaultAccount()
            loadPlayedGames()
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launchWhenResumed {
            loadPlayedGames()
        }
    }

    private suspend fun loadPlayedGames() {
        GamesConfigurationService.loadPlayedGames(requireContext())?.let { playedGames = ArrayList(it) }
        Log.d(TAG, "loadPlayedGames: $playedGames")
        fun getGameInfo(context: Context, packageName: String) = runCatching {
            val packageManager = context.packageManager
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            val appName = packageManager.getApplicationLabel(appInfo).toString()
            val appIcon = packageManager.getApplicationIcon(appInfo)
            Pair(appName, appIcon)
        }.getOrNull()

        val gameItems = playedGames.mapNotNull {
            val defaultAccount = GamesConfigurationService.getDefaultAccount(requireContext(), it)
            val gameInfo = getGameInfo(requireContext(), it)
            val playerName = allPlayers.find { it.first.name == defaultAccount?.name }?.second
            val name = if (defaultAccount != null && playerName != null) "${defaultAccount.name} (${playerName})" else null
            if (gameInfo != null) {
                GameItem(gameInfo.second, gameInfo.first, name, it)
            } else null
        }
        if (gameItems.isEmpty()) {
            return
        }
        recyclerView.adapter = GameAccountChangeAdapter(gameItems.sortedBy { it.gameName }) { item ->
            showChangeCurrentConfirmationDialog(item)
        }
    }

    private suspend fun initDefaultAccount() {
        var defaultAccount = GamesConfigurationService.getDefaultAccount(requireContext(), GAMES_PACKAGE_NAME)
        currentPlayer = if (defaultAccount == null) {
            (allPlayers.find { it.first.name == currentPlayer?.first?.name } ?: allPlayers.first()).also {
                GamesConfigurationService.setDefaultAccount(requireContext(), GAMES_PACKAGE_NAME, it.first)
            }
        } else {
            val player = GamesConfigurationService.getPlayer(requireContext(), defaultAccount)
            JSONObject(player).toPlayer().displayName?.let { Pair(defaultAccount, it) }
        }
        defaultAccountText.text = String.format(
            requireContext().getString(R.string.games_change_default_account_description), "${currentPlayer?.first?.name} (${currentPlayer?.second})"
        )
        defaultAccountText.visibility = View.VISIBLE
    }

    private fun showChangeCurrentConfirmationDialog(gameItem: GameItem? = null) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_game_account_change_confirmation, null)

        val dialog = BottomSheetDialog(requireContext())
        dialog.setContentView(dialogView)
        dialog.setOnDismissListener {
            lifecycleScope.launchWhenStarted { loadPlayedGames() }
        }
        dialog.show()

        val gameInfoContainer = dialog.findViewById<LinearLayout>(R.id.game_info_container)
        val dialogSecondTitle = dialogView.findViewById<TextView>(R.id.tv_dialog_second_title)
        val dialogTitle = dialogView.findViewById<TextView>(R.id.tv_dialog_title)
        val radioChooseContainer = dialogView.findViewById<LinearLayout>(R.id.llt_radio_choose_container)

        if (gameItem != null) {
            gameInfoContainer?.visibility = View.VISIBLE
            dialogView.findViewById<TextView>(R.id.tv_game_name).text = gameItem.gameName
            dialogView.findViewById<TextView>(R.id.tv_game_account).text = gameItem.defaultAccount ?: requireContext().getString(R.string.games_state_description_signed_out)
            dialogView.findViewById<ImageView>(R.id.img_game_icon).setImageDrawable(gameItem.icon)

            dialogTitle.text = requireContext().getString(R.string.games_change_per_game_dialog_title)
            dialogSecondTitle.text = requireContext().getString(R.string.games_change_per_game_dialog_description)

            allPlayers.filter { gameItem.defaultAccount == null || gameItem.defaultAccount.contains(it.first.name) == false }.forEach {
                buildRadioItem(
                    String.format(requireContext().getString(R.string.games_change_per_game_dialog_option_use_player), "${it.first.name} (${it.second})"),
                ) {
                    lifecycleScope.launchWhenStarted {
                        GamesConfigurationService.setDefaultAccount(requireContext(), gameItem.gamePackageName, it.first)
                        dialog.dismiss()
                        Snackbar.make(
                            requireView(),
                            String.format(requireContext().getString(R.string.games_change_per_game_dialog_option_use_player_snackbar_success_message), "${it.first.name} (${it.second})"),
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                }.also { radioChooseContainer.addView(it) }
            }

            if (gameItem.defaultAccount != null) {
                buildRadioItem(
                    requireContext().getString(R.string.games_change_per_game_dialog_option_sign_out),
                ) {
                    lifecycleScope.launchWhenStarted {
                        GamesConfigurationService.setDefaultAccount(requireContext(), gameItem.gamePackageName, null)
                        dialog.dismiss()
                        Snackbar.make(
                            requireView(), requireContext().getString(R.string.games_change_per_game_dialog_option_sign_out_snackbar_success_message), Snackbar.LENGTH_SHORT
                        ).show()
                    }
                }.also { radioChooseContainer.addView(it) }
            }
        } else {
            gameInfoContainer?.visibility = View.GONE
            dialogTitle.text = requireContext().getString(R.string.games_change_default_account_for_all_games_dialog_title)
            dialogSecondTitle.text = String.format(
                requireContext().getString(R.string.games_change_default_account_for_all_games_dialog_description), "${currentPlayer?.first?.name} (${currentPlayer?.second})"
            )
            buildRadioItem(
                requireContext().getString(R.string.games_change_default_account_for_all_games_dialog_option_only_for_new_games_title),
                requireContext().getString(R.string.games_change_default_account_for_all_games_dialog_option_only_for_new_games_description)
            ) {
                dialog.dismiss()
                chooseAccountDialog(false)
            }.also { radioChooseContainer.addView(it) }
            buildRadioItem(
                requireContext().getString(R.string.games_change_default_account_for_all_games_dialog_option_for_all_games_title),
                requireContext().getString(R.string.games_change_default_account_for_all_games_dialog_option_for_all_games_description)
            ) {
                dialog.dismiss()
                chooseAccountDialog(true)
            }.also { radioChooseContainer.addView(it) }
            buildRadioItem(
                requireContext().getString(R.string.games_change_default_account_for_all_games_dialog_option_sign_out_of_all_games_title),
                requireContext().getString(R.string.games_change_default_account_for_all_games_dialog_option_sign_out_of_all_games_description)
            ) {
                lifecycleScope.launchWhenStarted {
                    playedGames.forEach { GamesConfigurationService.setDefaultAccount(requireContext(), it, null) }
                    dialog.dismiss()
                    Snackbar.make(
                        requireView(), requireContext().getString(R.string.games_change_sign_out_of_all_games_snackbar_success_message), Snackbar.LENGTH_SHORT
                    ).show()
                }
            }.also { radioChooseContainer.addView(it) }
        }

        dialogView.findViewById<Button>(R.id.btn_change_cancel).setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun chooseAccountDialog(changeAllGames: Boolean) {
        AlertDialog.Builder(requireContext()).setTitle(requireContext().getString(R.string.credentials_assisted_choose_account_label)).setItems(allPlayers.map {
            "${it.first.name} (${it.second})"
        }.toTypedArray()) { _, which ->
            lifecycleScope.launchWhenStarted {
                currentPlayer = allPlayers[which]
                GamesConfigurationService.setDefaultAccount(requireContext(), GAMES_PACKAGE_NAME, currentPlayer?.first)
                val tips = if (changeAllGames) {
                    playedGames.forEach { GamesConfigurationService.setDefaultAccount(requireContext(), it, currentPlayer?.first) }
                    String.format(requireContext().getString(R.string.games_change_default_account_for_all_games_snackbar_success_message), "${currentPlayer?.first?.name} (${currentPlayer?.second})")
                } else {
                    String.format(requireContext().getString(R.string.games_change_default_account_for_new_games_snackbar_success_message), "${currentPlayer?.first?.name} (${currentPlayer?.second})")
                }
                Snackbar.make(requireView(), tips, Snackbar.LENGTH_SHORT).show()
                initDefaultAccount()
                loadPlayedGames()
            }
        }.setNegativeButton(requireContext().getString(R.string.games_delete_snapshot_dialog_cancel), null).show()
    }

    private fun buildRadioItem(btnTitle: String, btnDescription: String? = null, onItemClick: () -> Unit): View {
        val radioRoot = LayoutInflater.from(requireContext()).inflate(R.layout.item_game_account_radio_root, null)
        radioRoot.findViewById<AppCompatRadioButton>(R.id.game_radio_button_title).text = btnTitle
        radioRoot.findViewById<TextView>(R.id.game_radio_button_tips).apply {
            visibility = if (btnDescription.isNullOrEmpty()) View.GONE else View.VISIBLE
            text = btnDescription
        }
        radioRoot.setOnClickListener { onItemClick() }
        return radioRoot
    }
}
