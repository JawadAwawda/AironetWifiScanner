package com.aironet.wifiscanner

data class WifiGroup(
    val ssid: String,
    val networks: MutableList<WifiNetworkInfo>,
    var isExpanded: Boolean = false
)
