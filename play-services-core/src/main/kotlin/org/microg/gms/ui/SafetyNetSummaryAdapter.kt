package org.microg.gms.ui

import android.graphics.Color
import android.text.format.DateUtils
import org.microg.gms.safetynet.SafetyNetSummary
import org.microg.gms.ui.SafetyNetSummaryAdapter.SafetyNetSummaryViewHolder
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.R
import org.json.JSONException
import org.json.JSONObject
import org.microg.gms.safetynet.SafetyNetRequestType

class SafetyNetSummaryAdapter(
    private val recentRequests: List<SafetyNetSummary>,
    var clickHandler: (SafetyNetSummary) -> Unit
) : ListAdapter<SafetyNetSummary, SafetyNetSummaryViewHolder>(DiffCallback) {

    init {
        submitList(recentRequests)
    }

    companion object DiffCallback : DiffUtil.ItemCallback<SafetyNetSummary>() {
        override fun areItemsTheSame(oldItem: SafetyNetSummary, newItem: SafetyNetSummary): Boolean {
            return oldItem.id==newItem.id
        }

        override fun areContentsTheSame(oldItem: SafetyNetSummary, newItem: SafetyNetSummary): Boolean {
            return oldItem==newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SafetyNetSummaryViewHolder {
        val view: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.safety_net_recent_card, parent, false)
        return SafetyNetSummaryViewHolder(view)
    }



    private fun getResponseStatus(summary: SafetyNetSummary) : Pair<Int?, String> {
        if (summary.responseStatus == null) return Pair(null, "Not completed yet")

        if (summary.requestType == SafetyNetRequestType.ATTESTATION) {
            if (!summary.responseStatus!!.isSuccess) return Pair(Color.RED, "Failed")

            val (basicIntegrity, ctsProfileMatch) = try {
                JSONObject(summary.responseData!!).let {
                    Pair(
                        it.optBoolean("basicIntegrity", false),
                        it.optBoolean("ctsProfileMatch", false)
                    )
                }
            } catch (e: JSONException) {
                return Pair(Color.RED, "Invalid JSON")
            }

            return when {
                basicIntegrity && ctsProfileMatch -> {
                    Pair(Color.GREEN, "Integrity and CTS passed")
                }
                basicIntegrity -> {
                    Pair(Color.RED, "CTS failed")
                }
                else -> {
                    Pair(Color.RED, "Integrity failed")
                }
            }


        } else {
            return if (summary.responseStatus!!.isSuccess) {
                Pair(Color.GREEN, "Success")
            } else {
                Pair(Color.RED, summary.responseStatus!!.statusMessage)
            }
        }
    }

    override fun onBindViewHolder(holder: SafetyNetSummaryViewHolder, position: Int) {
        val summary = getItem(position)
        val context = holder.packageName.context
        val pm = context.packageManager

        val appInfo = pm.getApplicationInfoIfExists(summary.packageName)
        if(appInfo==null){
            return Toast.makeText(context, "Application not installed", Toast.LENGTH_SHORT).show()
        }

        holder.appIcon.setImageDrawable(pm.getApplicationInfoIfExists(summary.packageName)?.loadIcon(pm))

        holder.requestType.text = summary.requestType.name
        holder.date.text = DateUtils.getRelativeDateTimeString(context, summary.timestamp, DateUtils.MINUTE_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, DateUtils.FORMAT_SHOW_TIME)


        holder.packageName.text = summary.packageName

        getResponseStatus(summary).let {
            it.first?.let{ color -> holder.infoMsg.setTextColor(color) }
            holder.infoMsg.text = it.second
        }

        holder.itemView.setOnClickListener { clickHandler(summary) }
    }

    fun clearList() {
        submitList(emptyList())
        notifyDataSetChanged()
    }

    class SafetyNetSummaryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val appIcon: ImageView = view.findViewById(R.id.snet_recent_appicon)
        val requestType: TextView = view.findViewById(R.id.snet_recent_type)
        val date: TextView = view.findViewById(R.id.snet_recent_date)
        val packageName: TextView = view.findViewById(R.id.snet_recent_package)
        val infoMsg: TextView = view.findViewById(R.id.snet_recent_infomsg)
    }
}
