# Aironet WiFi Scanner

A simple Android application that scans WiFi networks and displays Cisco Aironet Information Elements (IE), including AP name, SSID, and signal strength.

## Features

- **WiFi Network Scanning**: Scans and displays all nearby WiFi networks
- **Signal Strength Visualization**: Color-coded signal strength indicators (Excellent/Good/Fair/Weak/Poor)
- **Frequency Band Display**: Shows whether network is on 2.4 GHz, 5 GHz, or 6 GHz
- **Cisco Aironet Detection**: Automatically identifies Cisco Aironet Access Points
- **Aironet Information Elements**: Displays AP name and load information from Cisco APs
- **Material Design UI**: Modern, clean interface with Cisco branding

## Requirements

- **Android 11 (API Level 30) or higher** - Required for InformationElement API
- **Location Permission** - Required for WiFi scanning
- **WiFi Enabled** - Device must have WiFi capability

## Permissions

The app requires the following permissions:
- `ACCESS_FINE_LOCATION` - Required to scan WiFi networks
- `ACCESS_WIFI_STATE` - Required to access WiFi information
- `CHANGE_WIFI_STATE` - Required to initiate WiFi scans

## Building the App

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK 34
- Gradle 8.2+

### Build Instructions

1. Clone or download this project
2. Open the project in Android Studio
3. Sync Gradle files
4. Build the APK:
   ```bash
   ./gradlew assembleDebug
   ```
5. Install on device:
   ```bash
   ./gradlew installDebug
   ```

## Usage

1. Launch the app - displays "Aironet WiFi Scanner" header
2. Grant location permission when prompted
3. Tap the "SCAN NETWORKS" button to scan for available WiFi networks
4. Networks are displayed grouped by SSID with collapsible sections
5. Each network group shows the number of access points (APs) available
6. Tap on an individual AP to view detailed information

### Network List View
The main screen shows:
- **Network Groups**: WiFi networks organized by SSID name
- **AP Count**: Number of access points broadcasting each network
- **Expandable Groups**: Click the expand/collapse icon to show/hide individual APs
- **AP Details**: Each AP shows:
  - BSSID (MAC address)
  - AP Name (for Cisco Aironet APs)
  - Signal Strength (dBm with color indicator)
  - Frequency Band (2.4 GHz, 5 GHz, etc.)
  - Channel number

### Detailed Network Information
Tap on a network to see comprehensive details:
- **GENERAL Section**:
  - Signal Strength (dBm)
  - Signal Quality (Excellent/Good/Fair/Weak/Poor)
  - Frequency (MHz and GHz)
  - Channel number

- **PROTOCOL Section**:
  - WiFi Standard (802.11n, 802.11ac, etc.)
  - Channel Width (20 MHz, 40 MHz, 80 MHz, etc.)

- **CISCO AIRONET Section** (when available):
  - AP Name (configured access point name)
  - Client Count (number of connected devices)
  - AP Load (percentage utilization)
  - Channel Utilization (percentage)

## Screenshots

### Main Screen
The main screen displays all detected WiFi networks grouped by SSID. Each group shows the number of access points and expandable details.

![Main Screen](screenshots/main_screen.jpg)

### Network Details
Detailed view showing complete information about a selected network including general info, protocol details, and Cisco Aironet specific data.

![Network Details](screenshots/network_details.jpg)

## Technical Details

### Aironet Information Element Parsing

The app parses vendor-specific Information Elements (IE ID 221) from WiFi scan results. It identifies Cisco APs by their Organizationally Unique Identifier (OUI): `00:40:96`.

For Cisco Aironet APs, the app attempts to extract:
- **AP Name**: The configured name of the Access Point
- **Load Information**: Client count and utilization data

### Architecture

- **MainActivity**: Handles UI, permissions, and user interactions
- **WifiScannerService**: Manages WiFi scanning and BroadcastReceiver
- **AironetParser**: Parses Cisco-specific Information Elements
- **WifiNetworkInfo**: Data model for network information
- **WifiNetworkAdapter**: RecyclerView adapter for displaying networks

## Important Notes

⚠️ **Aironet IE Availability**: Aironet Information Elements are proprietary Cisco features and will only be present in scan results from Cisco Aironet Access Points. Non-Cisco APs will not broadcast these IEs.

⚠️ **Testing Limitations**: Full Aironet IE parsing functionality requires access to physical Cisco Aironet hardware for testing.

⚠️ **Modern Networks**: Many modern Cisco deployments have moved away from proprietary Aironet IEs in favor of standardized 802.11 features.

## License

This is a demonstration project created for educational purposes.

