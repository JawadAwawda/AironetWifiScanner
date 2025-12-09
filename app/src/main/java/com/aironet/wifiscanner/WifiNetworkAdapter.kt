package com.aironet.wifiscanner

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class WifiNetworkAdapter(
    private val networks: List<WifiNetworkInfo>,
    private val onNetworkClickListener: (WifiNetworkInfo) -> Unit
) : RecyclerView.Adapter<WifiNetworkAdapter.WifiNetworkViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):
            WifiNetworkViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_wifi_network, parent, false)
        return WifiNetworkViewHolder(view)
    }

    override fun onBindViewHolder(holder: WifiNetworkViewHolder, position: Int) {
        val network = networks[position]
        holder.bind(network)
    }

    override fun getItemCount(): Int {
        return networks.size
    }

    inner class WifiNetworkViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val signalIndicator: View = itemView.findViewById(R.id.signalIndicator)
        private val bssidTextView: TextView = itemView.findViewById(R.id.bssidText)
        private val apNameTextView: TextView = itemView.findViewById(R.id.apNameText)
        private val channelTextView: TextView = itemView.findViewById(R.id.channelText)
        private val connectedStatusTextView: TextView = itemView.findViewById(R.id.connectedStatus)
        private val signalStrengthTextView: TextView = itemView.findViewById(R.id.signalStrengthText)
        private val frequencyBandTextView: TextView = itemView.findViewById(R.id.frequencyBandText)

        fun bind(network: WifiNetworkInfo) {
            bssidTextView.text = network.bssid
            signalStrengthTextView.text = "${network.rssi} dBm"
            channelTextView.text = "Channel: ${network.channel}"
            frequencyBandTextView.text = network.frequencyBand

            if (network.apName != null) {
                apNameTextView.text = network.apName
                apNameTextView.visibility = View.VISIBLE
            } else {
                apNameTextView.visibility = View.GONE
            }

            if (network.isConnected) {
                connectedStatusTextView.visibility = View.VISIBLE
            } else {
                connectedStatusTextView.visibility = View.GONE
            }

            val signalColor = when {
                network.rssi >= -50 -> R.color.signal_excellent
                network.rssi >= -67 -> R.color.signal_good
                network.rssi >= -70 -> R.color.signal_fair
                else -> R.color.signal_weak
            }
            signalIndicator.setBackgroundColor(ContextCompat.getColor(itemView.context, signalColor))

            itemView.setOnClickListener {
                onNetworkClickListener(network)
            }
        }
    }
}
