package org.microg.gms.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.R
import org.microg.gms.safetynet.SafetyNetDatabase

class SafetyNetRecentListFragment : Fragment(R.layout.safety_net_recents_list_fragment){

    private lateinit var adapter: SafetyNetSummaryAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        val db = SafetyNetDatabase(requireContext())
        val recentRequests = db.recentRequests
        db.close()

        val recyclerView: RecyclerView = view.findViewById(R.id.snet_recent_recyclerview)
        if(recentRequests.isEmpty()){
            recyclerView.isVisible = false
        }else{
            recyclerView.layoutManager = LinearLayoutManager(context)
            adapter = SafetyNetSummaryAdapter(recentRequests) {
                findNavController().navigate(requireContext(), R.id.openSafetyNetRecent, bundleOf(
                    "summary" to it
                ))
            }
            recyclerView.adapter = adapter
        }
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.add(0, MENU_CLEAR_REQUESTS, 0, R.string.menu_clear_recent_requests)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            MENU_CLEAR_REQUESTS -> {
                val db = SafetyNetDatabase(requireContext())
                db.clearAllRequests()
                db.close()
                adapter.clearList()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        private const val MENU_CLEAR_REQUESTS = Menu.FIRST
    }
}
