package com.aironet.wifiscanner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiInfo;
import android.os.Build;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class WifiScannerService {
    private static final String TAG = "WifiScannerService";
    private final Context context;
    private final WifiManager wifiManager;
    private ScanCallback scanCallback;
    private BroadcastReceiver wifiScanReceiver;

    public interface ScanCallback {
        void onScanCompleted(List<WifiNetworkInfo> networks);
        void onScanFailed(String error);
    }

    public WifiScannerService(Context context) {
        this.context = context;
        this.wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    public void startScan(ScanCallback callback) {
        this.scanCallback = callback;
        registerScanReceiver();
        boolean scanStarted = wifiManager.startScan();
        if (!scanStarted) {
            Log.e(TAG, "Wi-Fi scan failed to start.");
            if (scanCallback != null) {
                scanCallback.onScanFailed("Scan initiation failed.");
            }
        }
    }

    public void stopScan() {
        if (wifiScanReceiver != null) {
            try {
                context.unregisterReceiver(wifiScanReceiver);
                wifiScanReceiver = null;
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Receiver not registered, skipping unregister.");
            }
        }
    }

    private void registerScanReceiver() {
        if (wifiScanReceiver != null) return; // Already registered
        wifiScanReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent intent) {
                boolean success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false);
                if (success) {
                    processScanResults();
                } else {
                    Log.e(TAG, "Wi-Fi scan failed.");
                    if (scanCallback != null) {
                        scanCallback.onScanFailed("Scan failed to return results.");
                    }
                }
                stopScan(); // Unregister after getting results
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        context.registerReceiver(wifiScanReceiver, intentFilter);
    }

    private void processScanResults() {
        try {
            List<ScanResult> results = wifiManager.getScanResults();
            WifiInfo connectedWifiInfo = wifiManager.getConnectionInfo();
            String connectedBssid = (connectedWifiInfo != null) ? connectedWifiInfo.getBSSID() : null;
            Log.d(TAG, "Connected BSSID: " + connectedBssid);

            List<WifiNetworkInfo> networkInfos = new ArrayList<>();
            for (ScanResult result : results) {
                if (result.SSID == null || result.SSID.isEmpty()) continue;
                Log.d(TAG, "Found network: " + result.SSID + " (" + result.BSSID + ")");

                WifiNetworkInfo info = new WifiNetworkInfo(result.SSID, result.BSSID, result.level, result.frequency);
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    info.setWifiStandard(result.getWifiStandard());
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    info.setChannelWidth(result.channelWidth);
                    info.setCenterFreq0(result.centerFreq0);
                    info.setCenterFreq1(result.centerFreq1);
                }
                
                AironetParser.parseAironetIE(result, info);

                if (connectedBssid != null && connectedBssid.equalsIgnoreCase(result.BSSID)) {
                    info.setConnected(true);
                    Log.d(TAG, "Marked " + result.SSID + " as connected.");
                }
                
                networkInfos.add(info);
            }

            if (scanCallback != null) {
                scanCallback.onScanCompleted(networkInfos);
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Permission error processing scan results", e);
            if (scanCallback != null) {
                scanCallback.onScanFailed("Permission denied.");
            }
        }
    }
}
