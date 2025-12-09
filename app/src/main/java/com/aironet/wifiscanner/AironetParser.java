package com.aironet.wifiscanner;

import android.net.wifi.ScanResult;
import android.util.Log;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class AironetParser {
    private static final String TAG = "AironetParser";

    private static final List<byte[]> CISCO_OUIS = Arrays.asList(
        new byte[]{(byte) 0x00, (byte) 0x40, (byte) 0x96}
    );

    private static final int VENDOR_SPECIFIC_IE_ID = 221;
    private static final int AIRONET_IE_ID = 133;
    private static final int BSS_LOAD_IE_ID = 11;

    // TLV Types from observation
    private static final int TLV_AP_NAME = 0x01;
    private static final int TLV_LOAD = 0x03;
    private static final int TLV_CLIENTS = 0x04;
    private static final int TLV_DEVICE_TYPE = 0x0B;
    private static final int TLV_CHANNEL_UTILIZATION = 0x14;
    private static final int TLV_RADIO_TYPE = 0x02; // This is a guess

    public static void parseAironetIE(ScanResult scanResult, WifiNetworkInfo networkInfo) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R) return;

        try {
            List<ScanResult.InformationElement> ies = scanResult.getInformationElements();
            if (ies == null) return;

            for (ScanResult.InformationElement ie : ies) {
                ByteBuffer buffer = ie.getBytes();
                if (buffer == null) continue;

                final byte[] rawData = new byte[buffer.remaining()];
                buffer.get(rawData);

                if (ie.getId() == AIRONET_IE_ID) {
                    networkInfo.setCiscoAP(true);
                    networkInfo.appendRawAironetData(bytesToHex(rawData), bytesToAscii(rawData), ie.getId());
                    parseIE133Payload(rawData, networkInfo);
                } else if (ie.getId() == VENDOR_SPECIFIC_IE_ID && rawData.length > 3) {
                    if (isCiscoOUI(Arrays.copyOfRange(rawData, 0, 3))) {
                        networkInfo.setCiscoAP(true);
                        networkInfo.appendRawAironetData(bytesToHex(rawData), bytesToAscii(rawData), ie.getId());
                        byte[] payload = Arrays.copyOfRange(rawData, 3, rawData.length);
                        parseIE221Payload(payload, networkInfo);
                    }
                } else if (ie.getId() == BSS_LOAD_IE_ID) {
                    networkInfo.appendRawAironetData(bytesToHex(rawData), bytesToAscii(rawData), ie.getId());
                    parseBssLoadIE(rawData, networkInfo);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Fatal error during IE processing: " + e.getMessage(), e);
        }
    }

    private static void parseBssLoadIE(byte[] payload, WifiNetworkInfo networkInfo) {
        if (payload.length < 5) return; // Station Count(2) + Chan Util(1) + Capacity(2)

        ByteBuffer buffer = ByteBuffer.wrap(payload);
        buffer.order(ByteOrder.LITTLE_ENDIAN); // Standard 802.11 fields are Little Endian

        // Field: Station Count (2 bytes)
        int stationCount = buffer.getShort() & 0xFFFF;
        if (networkInfo.getClientCount() == -1) {
            networkInfo.setClientCount(stationCount);
        }

        // Field: Channel Utilization (1 byte). Value is 0-255.
        int channelUtilization = buffer.get() & 0xFF;
        if (networkInfo.getChannelUtilization() == -1) {
            networkInfo.setChannelUtilization(channelUtilization);
        }
    }

    private static void parseIE133Payload(byte[] payload, WifiNetworkInfo networkInfo) {
        if (networkInfo.getApName() == null) {
            String name = findApNameAsString(payload);
            if (name != null) {
                networkInfo.setApName(name);
            }
        }
    }

    private static void parseIE221Payload(byte[] payload, WifiNetworkInfo networkInfo) {
        ByteBuffer buffer = ByteBuffer.wrap(payload);
        while (buffer.remaining() >= 2) {
            int type = buffer.get() & 0xFF;
            int value = buffer.get() & 0xFF;

            switch (type) {
                case TLV_LOAD:
                    if (networkInfo.getApLoad() == -1) networkInfo.setApLoad(value);
                    break;
                case TLV_CLIENTS:
                    if (networkInfo.getClientCount() == -1) networkInfo.setClientCount(value);
                    break;
                case TLV_DEVICE_TYPE: // 0x0B
                    if (networkInfo.getDeviceType() == null) {
                        networkInfo.setDeviceType("Unknown (0x" + String.format("%02X", value) + ")");
                    }
                    break;
                case TLV_CHANNEL_UTILIZATION: // 0x14
                    if (networkInfo.getChannelUtilization() == -1) networkInfo.setChannelUtilization(value);
                    break;
            }
        }
    }

    private static boolean isCiscoOUI(byte[] oui) {
        for (byte[] ciscoOUI : CISCO_OUIS) {
            if (Arrays.equals(oui, ciscoOUI)) return true;
        }
        return false;
    }

    private static String findApNameAsString(byte[] payload) {
        String asciiString = new String(payload, StandardCharsets.US_ASCII);
        StringBuilder name = new StringBuilder();
        boolean foundPrintable = false;

        for (char c : asciiString.toCharArray()) {
            if (c >= ' ' && c <= '~') {
                if (!foundPrintable && Character.isLetterOrDigit(c)) {
                    foundPrintable = true;
                }
                if(foundPrintable) {
                    name.append(c);
                }
            } else {
                if (foundPrintable) break;
            }
        }

        String potentialName = name.toString().trim();
        return potentialName.length() > 2 ? potentialName : null;
    }

    private static String decodeRadioType(byte b) {
        switch(b) {
            case 0: return "802.11a";
            case 1: return "802.11b";
            case 2: return "802.11g";
            case 3: return "802.11n (2.4GHz)";
            case 4: return "802.11n (5GHz)";
            case 5: return "802.11ac";
            default: return "Unknown";
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02X ", b));
        return sb.toString().trim();
    }

    private static String bytesToAscii(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append((b >= 0x20 && b < 0x7F) ? (char) b : '.');
        return sb.toString();
    }
}
