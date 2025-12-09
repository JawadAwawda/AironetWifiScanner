package com.aironet.wifiscanner;

public class WifiNetworkInfo {
    private String ssid;
    private String bssid;
    private int rssi;
    private int frequency;
    private int channel;
    private boolean isCiscoAP;
    private boolean isConnected;

    // Cisco Specific
    private String apName;
    private String deviceType;
    private String radioType;
    private int clientCount = -1;
    private int apLoad = -1;
    private int channelUtilization = -1;

    // Raw IE Data for Debugging
    private String rawAironetData = "";
    private String rawAironetDataAscii = "";

    // Protocol Info
    private int wifiStandard;
    private int channelWidth;
    private int centerFreq0;
    private int centerFreq1;

    public WifiNetworkInfo(String ssid, String bssid, int rssi, int frequency) {
        this.ssid = ssid;
        this.bssid = bssid;
        this.rssi = rssi;
        this.frequency = frequency;
        this.channel = getChannelFromFrequency(frequency);
        this.isConnected = false;
    }

    // --- Getters ---
    public String getSsid() { return ssid; }
    public String getBssid() { return bssid; }
    public int getRssi() { return rssi; }
    public int getFrequency() { return frequency; }
    public int getChannel() { return channel; }
    public boolean isCiscoAP() { return isCiscoAP; }
    public boolean isConnected() { return isConnected; }
    public String getApName() { return apName; }
    public String getDeviceType() { return deviceType; }
    public String getRadioType() { return radioType; }
    public int getClientCount() { return clientCount; }
    public int getApLoad() { return apLoad; }
    public int getChannelUtilization() { return channelUtilization; }
    public String getRawAironetData() { return rawAironetData; }
    public String getRawAironetDataAscii() { return rawAironetDataAscii; }
    public int getWifiStandard() { return wifiStandard; }
    public int getChannelWidth() { return channelWidth; }
    public int getCenterFreq0() { return centerFreq0; }
    public int getCenterFreq1() { return centerFreq1; }

    // --- Setters ---
    public void setCiscoAP(boolean ciscoAP) { isCiscoAP = ciscoAP; }
    public void setConnected(boolean connected) { isConnected = connected; }
    public void setApName(String apName) { this.apName = apName; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }
    public void setRadioType(String radioType) { this.radioType = radioType; }
    public void setClientCount(int clientCount) { this.clientCount = clientCount; }
    public void setApLoad(int apLoad) { this.apLoad = apLoad; }
    public void setChannelUtilization(int channelUtilization) { this.channelUtilization = channelUtilization; }
    public void setWifiStandard(int wifiStandard) { this.wifiStandard = wifiStandard; }
    public void setChannelWidth(int channelWidth) { this.channelWidth = channelWidth; }
    public void setCenterFreq0(int centerFreq0) { this.centerFreq0 = centerFreq0; }
    public void setCenterFreq1(int centerFreq1) { this.centerFreq1 = centerFreq1; }
    
    public void appendRawAironetData(String hex, String ascii, int ieId) {
        String header = "IE " + ieId + " (" + (hex.length() / 3 + 1) + " bytes):\n";
        this.rawAironetData += header + hex + "\n\n";
        this.rawAironetDataAscii += header + ascii + "\n\n";
    }

    // --- Helper Methods ---
    public String getFrequencyBand() {
        if (frequency >= 2400 && frequency < 3000) return "2.4 GHz";
        if (frequency >= 5000 && frequency < 6000) return "5 GHz";
        if (frequency >= 6000 && frequency < 7125) return "6 GHz";
        return "Unknown";
    }

    public String getWifiStandardString() {
        switch (wifiStandard) {
            case 1: return "Legacy (802.11a/b/g)";
            case 4: return "Wi-Fi 4 (802.11n)";
            case 5: return "Wi-Fi 5 (802.11ac)";
            case 6: return "Wi-Fi 6 (802.11ax)";
            case 7: return "Wi-Fi 7 (802.11be)";
            default: return "Unknown";
        }
    }

    public String getChannelWidthString() {
        switch (channelWidth) {
            case 0: return "20 MHz";
            case 1: return "40 MHz";
            case 2: return "80 MHz";
            case 3: return "160 MHz";
            case 4: return "80+80 MHz";
            case 5: return "320 MHz";
            default: return "Unknown";
        }
    }

    public static int getChannelFromFrequency(int frequency) {
        if (frequency >= 2412 && frequency <= 2484) return (frequency - 2412) / 5 + 1;
        if (frequency >= 5180 && frequency <= 5825) return (frequency - 5180) / 5 + 36;
        if (frequency >= 5955 && frequency <= 7095) return (frequency - 5955) / 5 + 1;
        return -1;
    }

    public SignalQuality getSignalQuality() {
        if (rssi >= -55) return SignalQuality.EXCELLENT;
        if (rssi >= -67) return SignalQuality.GOOD;
        if (rssi >= -75) return SignalQuality.FAIR;
        if (rssi >= -85) return SignalQuality.WEAK;
        return SignalQuality.POOR;
    }

    public enum SignalQuality { EXCELLENT, GOOD, FAIR, WEAK, POOR }
}
