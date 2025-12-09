package com.aironet.wifiscanner

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class WifiGroupAdapter(
    private val wifiGroups: List<WifiGroup>,
    private val onNetworkClickListener: (WifiNetworkInfo) -> Unit
) : RecyclerView.Adapter<WifiGroupAdapter.WifiGroupViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):
            WifiGroupViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_wifi_group, parent, false)
        return WifiGroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: WifiGroupViewHolder, position: Int) {
        val wifiGroup = wifiGroups[position]
        holder.bind(wifiGroup)
    }

    override fun getItemCount(): Int {
        return wifiGroups.size
    }

    inner class WifiGroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ssidTextView: TextView = itemView.findViewById(R.id.ssidText)
        private val apCountTextView: TextView = itemView.findViewById(R.id.apCountText)
        private val expandIcon: ImageView = itemView.findViewById(R.id.expandIcon)
        private val nestedRecyclerView: RecyclerView = itemView.findViewById(R.id.nestedRecyclerView)

        fun bind(wifiGroup: WifiGroup) {
            ssidTextView.text = wifiGroup.ssid
            apCountTextView.text = "${wifiGroup.networks.size} APs"

            nestedRecyclerView.layoutManager = LinearLayoutManager(itemView.context)
            val wifiNetworkAdapter = WifiNetworkAdapter(wifiGroup.networks, onNetworkClickListener)
            nestedRecyclerView.adapter = wifiNetworkAdapter

            nestedRecyclerView.visibility = if (wifiGroup.isExpanded) View.VISIBLE else View.GONE
            expandIcon.setImageResource(if (wifiGroup.isExpanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more)

            itemView.setOnClickListener {
                wifiGroup.isExpanded = !wifiGroup.isExpanded
                notifyItemChanged(adapterPosition)
            }
        }
    }
}
