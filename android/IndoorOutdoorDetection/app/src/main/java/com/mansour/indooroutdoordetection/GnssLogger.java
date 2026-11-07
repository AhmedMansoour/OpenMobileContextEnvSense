package com.mansour.indooroutdoordetection;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.OnNmeaMessageListener;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.Locale;

/**
 * GNSS Logger — logs Fix, Satellites, and NMEA messages.
 * Uses millisecond timestamps and fixed column layouts (no labels).
 * Compatible with Android 6 → 14.
 */
public class GnssLogger {

    private static final String TAG = "GnssLogger";

    private final Context ctx;
    private final LocationManager lm;
    private boolean started = false;

    // --- CSV Headers (constant columns, label-free) ---
    private static final String H_FIX =
            "timestamp_ms,provider,lat,lon,alt_m,speed_mps,bearing_deg,acc_m,vertAcc_m,speedAcc_mps,bearingAcc_deg\n";
    private static final String H_SV =
            "timestamp_ms,svid,constellation,usedInFix,cn0_dbhz,elev_deg,az_deg,cf_hz,hasAlm,hasEph\n";
    private static final String H_NMEA =
            "timestamp_ms,talker,raw\n";

    public GnssLogger(Context ctx, LabelBus labels) {
        this.ctx = ctx;
        this.lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);

        // Ensure headers once per session
        FileWriterQueue.enqueueHeader("gnss_fix.csv", H_FIX);
        FileWriterQueue.enqueueHeader("gnss_sv.csv",  H_SV);
        FileWriterQueue.enqueueHeader("gnss_nmea.csv", H_NMEA);
    }

    // ------------------------------------------------------------------
    // Start / Stop
    // ------------------------------------------------------------------
    @SuppressLint("MissingPermission")
    public void start() {
        if (started) return;
        started = true;

        if (!hasPermission()) {
            sendLog("[GNSS] ⚠ Location permission denied");
            return;
        }

        try {
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0.5f, fixListener);
        } catch (Exception e) {
            sendLog("[GNSS] ⚠ Failed to request location updates: " + e.getMessage());
        }

        // Register GNSS callbacks (Android N+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                lm.registerGnssStatusCallback(statusCallback);
                lm.addNmeaListener(nmeaListener);
            } catch (Exception e) {
                sendLog("[GNSS] ⚠ Failed to register callbacks: " + e.getMessage());
            }
        }

        sendLog("[GNSS] Logger started");
    }

    // ------------------------------------------------------------------
    // Location Fix Listener
    // ------------------------------------------------------------------
    private final LocationListener fixListener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location loc) {
            long ts = System.currentTimeMillis();

            double lat = loc.getLatitude();
            double lon = loc.getLongitude();
            double alt = loc.hasAltitude() ? loc.getAltitude() : Double.NaN;
            double spd = loc.hasSpeed() ? loc.getSpeed() : Double.NaN;
            double brg = loc.hasBearing() ? loc.getBearing() : Double.NaN;
            double acc = loc.hasAccuracy() ? loc.getAccuracy() : Double.NaN;
            double vert = (Build.VERSION.SDK_INT >= 26 && loc.hasVerticalAccuracy())
                    ? loc.getVerticalAccuracyMeters() : Double.NaN;
            double spdAcc = (Build.VERSION.SDK_INT >= 26 && loc.hasSpeedAccuracy())
                    ? loc.getSpeedAccuracyMetersPerSecond() : Double.NaN;
            double brgAcc = (Build.VERSION.SDK_INT >= 26 && loc.hasBearingAccuracy())
                    ? loc.getBearingAccuracyDegrees() : Double.NaN;

            String record = String.format(Locale.US,
                    "%d,%s,%.8f,%.8f,%.2f,%.2f,%.2f,%.1f,%.1f,%.1f,%.1f\n",
                    ts, loc.getProvider(), lat, lon, alt, spd, brg, acc, vert, spdAcc, brgAcc);
            FileWriterQueue.enqueue("gnss_fix.csv", record);
        }
    };

    // ------------------------------------------------------------------
    // Satellite Callback (API ≥ 24)
    // ------------------------------------------------------------------
    private final GnssStatus.Callback statusCallback =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                    ? new GnssStatus.Callback() {
                @Override
                public void onSatelliteStatusChanged(@NonNull GnssStatus st) {
                    long ts = System.currentTimeMillis();
                    int used = 0;
                    float snrSum = 0f;

                    int count = st.getSatelliteCount();
                    for (int i = 0; i < count; i++) {
                        boolean usedFix = st.usedInFix(i);
                        if (usedFix) used++;
                        float cn0 = st.getCn0DbHz(i);
                        snrSum += cn0;

                        // Satellite details
                        String line = String.format(Locale.US,
                                "%d,%d,%d,%d,%.2f,%.2f,%.2f,%.1f,%d,%d\n",
                                ts,
                                st.getSvid(i),
                                st.getConstellationType(i),
                                usedFix ? 1 : 0,
                                cn0,
                                st.getElevationDegrees(i),
                                st.getAzimuthDegrees(i),
                                Build.VERSION.SDK_INT >= 26 ? st.getCarrierFrequencyHz(i) : Double.NaN,
                                Build.VERSION.SDK_INT >= 26 && st.hasAlmanacData(i) ? 1 : 0,
                                Build.VERSION.SDK_INT >= 26 && st.hasEphemerisData(i) ? 1 : 0);
                        FileWriterQueue.enqueue("gnss_sv.csv", line);
                    }

                    if (count > 0) {
                        float avgSnr = snrSum / count;
                        sendLog(String.format(Locale.US,
                                "[%tT] [GNSS] %d sats (used %d) | avg CN0=%.1f dBHz",
                                ts, count, used, avgSnr));
                    }
                }
            } : null;

    // ------------------------------------------------------------------
    // NMEA Listener (API ≥ 24)
    // ------------------------------------------------------------------
    private final OnNmeaMessageListener nmeaListener =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                    ? new OnNmeaMessageListener() {
                @Override
                public void onNmeaMessage(String msg, long ts) {
                    // Clean and safely quote the message to avoid CSV parsing issues
                    String clean = msg.replace('\n', ' ').replace('\r', ' ');
                    String talker = clean.length() >= 6 ? clean.substring(1, 3) : "";

                    // ✅ Wrap raw NMEA in quotes to prevent commas from breaking the CSV
                    String record = String.format(Locale.US,
                            "%d,%s,\"%s\"\n",
                            System.currentTimeMillis(), talker, clean);

                    FileWriterQueue.enqueue("gnss_nmea.csv", record);
                }
            } : null;

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------
    private boolean hasPermission() {
        return ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void sendLog(String msg) {
        Intent i = new Intent("LIVE_LOG");
        i.putExtra("msg", msg);
        LocalBroadcastManager.getInstance(ctx).sendBroadcast(i);
    }

    public void stop() {
        if (!started) return;
        started = false;

        try { lm.removeUpdates(fixListener); } catch (Exception e) {
            Log.e(TAG, "Failed to remove location updates", e);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try { lm.unregisterGnssStatusCallback(statusCallback); } catch (Exception e) {
                Log.e(TAG, "Failed to unregister GNSS status callback", e);
            }
            try { lm.removeNmeaListener(nmeaListener); } catch (Exception e) {
                Log.e(TAG, "Failed to remove NMEA listener", e);
            }
        }

        sendLog("[GNSS] Logger stopped");
    }
}
