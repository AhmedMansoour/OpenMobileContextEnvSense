package com.mansour.indooroutdoordetection;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.List;
import java.util.Locale;

/**
 * Wi-Fi logger module for Indoor–Outdoor Detection (IOD).
 * Periodically performs scans, logs AP summaries and details to CSV,
 * and sends compact live summaries to the UI.
 *
 * Files:
 *   wifi_scan.csv → timestamp_ms,elapsed_ms,ap_count,mean_level,strongest_level
 *   wifi_ap.csv   → timestamp_ms,bssid,ssid,frequency,level,centerFreq0,centerFreq1,channelWidth,ts_micros
 */
public class WifiLogger {

    private static final String TAG = "WifiLogger";

    private final Context ctx;
    private final WifiManager wm;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final int periodMs = 3000;   // scan interval
    private boolean running = false;

    // ---------------------------------------------------------------------
    // CSV headers (constant columns, no label repetition)
    // ---------------------------------------------------------------------
    private static final String H_SCAN =
            "timestamp_ms,elapsed_ms,ap_count,mean_level,strongest_level\n";
    private static final String H_AP =
            "timestamp_ms,bssid,ssid,frequency,level,centerFreq0,centerFreq1,channelWidth,ts_micros\n";

    // ---------------------------------------------------------------------
    // Broadcast receiver for scan results
    // ---------------------------------------------------------------------
    private final BroadcastReceiver rx = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {

            if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED) {
                sendLog("[WIFI] ⚠ Location permission denied — cannot read scan results");
                return;
            }

            List<ScanResult> results = wm.getScanResults();
            if (results == null || results.isEmpty()) {
                sendLog("[WIFI] No scan results");
                return;
            }

            long ts = System.currentTimeMillis();
            long elapsed = SystemClock.elapsedRealtime();

            int n = results.size();
            double mean = 0;
            int strongest = Integer.MIN_VALUE;

            for (ScanResult s : results) {
                mean += s.level;
                if (s.level > strongest) strongest = s.level;
            }
            if (n > 0) mean /= n;

            // ---- Summary ----
            FileWriterQueue.enqueue("wifi_scan.csv",
                    String.format(Locale.US, "%d,%d,%d,%.1f,%d\n",
                            ts, elapsed, n, mean, strongest));

            // ---- Per-AP details ----
            for (ScanResult s : results) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        FileWriterQueue.enqueue("wifi_ap.csv",
                                String.format(Locale.US,
                                        "%d,%s,%s,%d,%d,%d,%d,%d,%d\n",
                                        ts,
                                        s.BSSID,
                                        s.SSID.replace(',', '_'),
                                        s.frequency,
                                        s.level,
                                        s.centerFreq0,
                                        s.centerFreq1,
                                        s.channelWidth,
                                        s.timestamp));
                    } else {
                        FileWriterQueue.enqueue("wifi_ap.csv",
                                String.format(Locale.US,
                                        "%d,%s,%s,%d,%d,,,,\n",
                                        ts,
                                        s.BSSID,
                                        s.SSID.replace(',', '_'),
                                        s.frequency,
                                        s.level));
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Error formatting AP line", e);
                }
            }

            // ---- Broadcast live summary ----
            sendLog(String.format(Locale.US,
                    "[%tT] [WIFI] %d APs | MeanRSSI=%.1f dBm | Strongest=%d dBm",
                    ts, n, mean, strongest));
        }
    };

    // ---------------------------------------------------------------------
    // Constructor
    // ---------------------------------------------------------------------
    public WifiLogger(Context ctx, LabelBus labels) {
        this.ctx = ctx;
        this.wm = (WifiManager) ctx.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        // Ensure headers once
        FileWriterQueue.enqueueHeader("wifi_scan.csv", H_SCAN);
        FileWriterQueue.enqueueHeader("wifi_ap.csv", H_AP);
    }

    // ---------------------------------------------------------------------
    // Lifecycle control
    // ---------------------------------------------------------------------
    /** Start periodic scanning and logging. */
    public void start() {
        if (running) return;
        running = true;
        ctx.registerReceiver(rx, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        handler.post(scanTick);
        sendLog("[WIFI] Logger started");
    }

    /** Stop scanning and unregister receiver. */
    public void stop() {
        if (!running) return;
        running = false;
        handler.removeCallbacksAndMessages(null);
        try { ctx.unregisterReceiver(rx); } catch (Exception ignored) {}
        sendLog("[WIFI] Logger stopped");
    }

    // ---------------------------------------------------------------------
    // Periodic scan trigger
    // ---------------------------------------------------------------------
    private final Runnable scanTick = new Runnable() {
        @Override
        public void run() {
            if (!running) return;

            if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED) {
                sendLog("[WIFI] ⚠ Location permission missing — skipping scan");
            } else {
                try {
                    boolean ok = wm.startScan();
                    if (!ok) sendLog("[WIFI] startScan() returned false");
                } catch (Exception e) {
                    sendLog("[WIFI] startScan() failed: " + e.getMessage());
                }
            }
            handler.postDelayed(this, periodMs);
        }
    };

    // ---------------------------------------------------------------------
    // Helper: broadcast to console
    // ---------------------------------------------------------------------
    private void sendLog(String msg) {
        Intent i = new Intent("LIVE_LOG");
        i.putExtra("msg", msg);
        LocalBroadcastManager.getInstance(ctx).sendBroadcast(i);
    }
}
