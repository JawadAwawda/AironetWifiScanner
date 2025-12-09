package com.aironet.wifiscanner;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Import the Kotlin classes
import com.aironet.wifiscanner.WifiGroup;
import com.aironet.wifiscanner.WifiGroupAdapter;
import kotlin.Unit;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1001;

    private MaterialButton scanButton;
    private ProgressBar progressBar;
    private TextView statusText;
    private RecyclerView networksRecyclerView;
    private TextView emptyStateText;

    private WifiGroupAdapter adapter;
    private WifiScannerService scannerService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        scannerService = new WifiScannerService(this);
        setupRecyclerView();
        scanButton.setOnClickListener(v -> onScanButtonClicked());
        checkPermissions();
    }

    private void initViews() {
        scanButton = findViewById(R.id.scanButton);
        progressBar = findViewById(R.id.progressBar);
        statusText = findViewById(R.id.statusText);
        networksRecyclerView = findViewById(R.id.networksRecyclerView);
        emptyStateText = findViewById(R.id.emptyStateText);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    private void setupRecyclerView() {
        networksRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void showNetworkDetailDialog(WifiNetworkInfo network) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_network_details, null);

        // --- Find Views ---
        TextView dialogSsid = dialogView.findViewById(R.id.dialogSsid);
        TextView dialogBssid = dialogView.findViewById(R.id.dialogBssid);
        TextView dialogSignal = dialogView.findViewById(R.id.dialogSignal);
        TextView dialogSignalQuality = dialogView.findViewById(R.id.dialogSignalQuality);
        TextView dialogFrequency = dialogView.findViewById(R.id.dialogFrequency);
        TextView dialogChannel = dialogView.findViewById(R.id.dialogChannel);
        TextView dialogStandard = dialogView.findViewById(R.id.dialogStandard);
        TextView dialogChannelWidth = dialogView.findViewById(R.id.dialogChannelWidth);
        View headerAironet = dialogView.findViewById(R.id.headerAironet);
        View cardAironet = dialogView.findViewById(R.id.cardAironet);
        TextView dialogApName = dialogView.findViewById(R.id.dialogApName);
        TextView dialogClientCount = dialogView.findViewById(R.id.dialogClientCount);
        TextView dialogApLoad = dialogView.findViewById(R.id.dialogApLoad);
        TextView dialogChannelUtilization = dialogView.findViewById(R.id.dialogChannelUtilization);
        Button closeButton = dialogView.findViewById(R.id.closeButton);

        // --- Set General Data ---
        dialogSsid.setText(network.getSsid());
        dialogBssid.setText(network.getBssid());
        dialogSignal.setText(network.getRssi() + " dBm");
        dialogSignalQuality.setText(getQualityString(network.getSignalQuality()));
        dialogSignalQuality.setTextColor(getSignalColorForQuality(network.getSignalQuality()));
        dialogFrequency.setText(network.getFrequency() + " MHz (" + network.getFrequencyBand() + ")");
        dialogChannel.setText(String.valueOf(network.getChannel()));

        // --- Set Protocol Data ---
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            dialogStandard.setText(network.getWifiStandardString());
            dialogChannelWidth.setText(network.getChannelWidthString());
        } else {
            dialogStandard.setText("Not supported");
            dialogChannelWidth.setText("--");
        }

        // --- Set Cisco Aironet Data ---
        if (network.isCiscoAP()) {
            headerAironet.setVisibility(View.VISIBLE);
            cardAironet.setVisibility(View.VISIBLE);

            dialogApName.setText(network.getApName() != null ? network.getApName() : "--");
            dialogClientCount.setText(network.getClientCount() != -1 ? String.valueOf(network.getClientCount()) : "--");
            dialogApLoad.setText(network.getApLoad() != -1 ? (network.getApLoad() * 100 / 255) + "%" : "--");
            dialogChannelUtilization.setText(network.getChannelUtilization() != -1 ? (network.getChannelUtilization() * 100 / 255) + "%" : "--");

        }

        // --- Dialog Setup ---
        AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogView).create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        closeButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private String getQualityString(WifiNetworkInfo.SignalQuality quality) {
        if (quality == null) return "Unknown";
        switch (quality) {
            case EXCELLENT: return "Excellent";
            case GOOD: return "Good";
            case FAIR: return "Fair";
            case WEAK: return "Weak";
            case POOR: return "Poor";
            default: return "Unknown";
        }
    }

    private int getSignalColorForQuality(WifiNetworkInfo.SignalQuality quality) {
        if (quality == null) return getColor(R.color.signal_poor);
        switch (quality) {
            case EXCELLENT: return getColor(R.color.signal_excellent);
            case GOOD: return getColor(R.color.signal_good);
            case FAIR: return getColor(R.color.signal_fair);
            case WEAK: return getColor(R.color.signal_weak);
            default: return getColor(R.color.signal_poor);
        }
    }

    private void onScanButtonClicked() {
        if (!hasLocationPermission()) {
            requestLocationPermission();
            return;
        }
        startWifiScan();
    }

    private void startWifiScan() {
        showLoading(true);
        scannerService.startScan(new WifiScannerService.ScanCallback() {
            @Override
            public void onScanCompleted(List<WifiNetworkInfo> networks) {
                runOnUiThread(() -> {
                    showLoading(false);

                    Map<String, List<WifiNetworkInfo>> groupedBySsid = networks.stream()
                            .collect(Collectors.groupingBy(WifiNetworkInfo::getSsid));

                    List<WifiGroup> wifiGroups = new ArrayList<>();
                    for (Map.Entry<String, List<WifiNetworkInfo>> entry : groupedBySsid.entrySet()) {
                        entry.getValue().sort(Comparator.comparingInt(WifiNetworkInfo::getRssi).reversed());
                        wifiGroups.add(new WifiGroup(entry.getKey(), new ArrayList<>(entry.getValue()), false));
                    }

                    wifiGroups.sort((g1, g2) -> {
                        int rssi1 = g1.getNetworks().get(0).getRssi();
                        int rssi2 = g2.getNetworks().get(0).getRssi();
                        return Integer.compare(rssi2, rssi1);
                    });

                    adapter = new WifiGroupAdapter(wifiGroups, network -> {
                        showNetworkDetailDialog(network);
                        return Unit.INSTANCE;
                    });
                    networksRecyclerView.setAdapter(adapter);

                    if (wifiGroups.isEmpty()) {
                        emptyStateText.setVisibility(View.VISIBLE);
                        networksRecyclerView.setVisibility(View.GONE);
                    } else {
                        emptyStateText.setVisibility(View.GONE);
                        networksRecyclerView.setVisibility(View.VISIBLE);
                    }

                    long ciscoCount = networks.stream().filter(WifiNetworkInfo::isCiscoAP).count();
                    String msg = "Found " + networks.size() + " networks in " + wifiGroups.size() + " groups" + (ciscoCount > 0 ? " (" + ciscoCount + " Cisco)" : "");
                    Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onScanFailed(String error) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(MainActivity.this, "Scan failed: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void showLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        statusText.setVisibility(loading ? View.VISIBLE : View.GONE);
        scanButton.setEnabled(!loading);
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
    }

    private void checkPermissions() {
        if (!hasLocationPermission()) {
            requestLocationPermission();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startWifiScan();
        } else {
            Toast.makeText(this, "Location permission is required to scan for WiFi networks.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (scannerService != null) {
            scannerService.stopScan();
        }
    }
}
