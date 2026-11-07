package com.mansour.indooroutdoordetection;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.*;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import java.util.Locale;

/**
 * Periodically records short microphone bursts (400 ms every 4 s)
 * to estimate ambient sound level (RMS, dBFS, peak).
 * Uses millisecond timestamps and fixed CSV columns (no labels).
 */
public class AudioLogger {

    private final Context ctx;
    private final Handler h = new Handler(Looper.getMainLooper());
    private final int periodMs = 4000; // repeat every 4 s
    private final int burstMs  = 400;  // record 0.4 s per cycle

    private static final String H = "timestamp_ms,rms,dbfs,peak\n";

    public AudioLogger(Context ctx, LabelBus labels) {
        this.ctx = ctx;
        FileWriterQueue.enqueueHeader("audio.csv", H);
    }

    public void start() { h.post(tick); }
    public void stop()  { h.removeCallbacksAndMessages(null); }

    // ---------------------------------------------------------------------
    // Periodic burst capture
    // ---------------------------------------------------------------------
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {

            // ---- Permission check ----
            if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                sendLog("[AUDIO] ⚠ Microphone permission denied");
                h.postDelayed(this, periodMs);
                return;
            }

            int sr = 8000;
            int ch = AudioFormat.CHANNEL_IN_MONO;
            int fmt = AudioFormat.ENCODING_PCM_16BIT;
            int min = AudioRecord.getMinBufferSize(sr, ch, fmt);
            if (min <= 0) {
                sendLog("[AUDIO] ⚠ Invalid buffer size");
                h.postDelayed(this, periodMs);
                return;
            }

            AudioRecord ar = new AudioRecord(MediaRecorder.AudioSource.MIC, sr, ch, fmt, min * 2);
            short[] buf = new short[min];

            try {
                ar.startRecording();
                long end = SystemClock.uptimeMillis() + burstMs;

                double sumSq = 0;
                double peak = 0;
                long N = 0;

                while (SystemClock.uptimeMillis() < end) {
                    int n = ar.read(buf, 0, buf.length);
                    for (int i = 0; i < n; i++) {
                        double v = buf[i] / 32768.0;
                        sumSq += v * v;
                        if (Math.abs(v) > peak) peak = Math.abs(v);
                    }
                    N += n;
                }

                double rms = Math.sqrt(sumSq / Math.max(1, N));
                double dbfs = 20.0 * Math.log10(rms + 1e-9);
                long t = System.currentTimeMillis();

                String line = String.format(Locale.US, "%d,%.6f,%.2f,%.6f\n", t, rms, dbfs, peak);
                FileWriterQueue.enqueue("audio.csv", line);

                sendLog(String.format(Locale.US,
                        "[%tT] [AUDIO] RMS=%.3f | dBFS=%.1f | Peak=%.3f",
                        t, rms, dbfs, peak));

            } catch (Exception e) {
                sendLog("[AUDIO] Error: " + e.getMessage());
            } finally {
                try { ar.stop(); } catch (Exception ignored) {}
                try { ar.release(); } catch (Exception ignored) {}
            }

            h.postDelayed(this, periodMs);
        }
    };

    // ---------------------------------------------------------------------
    // Helper: broadcast to live console
    // ---------------------------------------------------------------------
    private void sendLog(String msg) {
        Intent i = new Intent("LIVE_LOG");
        i.putExtra("msg", msg);
        LocalBroadcastManager.getInstance(ctx).sendBroadcast(i);
    }
}
