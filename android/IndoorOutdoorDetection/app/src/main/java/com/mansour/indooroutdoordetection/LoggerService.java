package com.mansour.indooroutdoordetection;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

/**
 * Foreground service for continuous sensor logging.
 * Starts individual logger modules (Sensor, WiFi, GNSS, Cellular, Audio)
 * and writes synchronized data streams through FileWriterQueue.
 */
public class LoggerService extends Service {

    public static final String CHANNEL_ID = "LOGGER_CH";
    public static final int NOTIF_ID = 1001;
    private static final String TAG = "LoggerService";

    // Loggers and label bus
    private SensorLogger sensorLogger;
    private WifiLogger wifiLogger;
    private GnssLogger gnssLogger;
    private CellLogger cellLogger;
    private AudioLogger audioLogger;
    private LabelBus labelBus;

    private boolean isInitialized = false;
    private BroadcastReceiver labelReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Service created");

        // Create notification channel early
        createChannel();

        // Initialize shared label bus
        labelBus = new LabelBus();
        registerLabelReceiver();

        // Initialize global writer queue (session folder)
        FileWriterQueue.init(getApplicationContext());

        // Instantiate all loggers
        sensorLogger = new SensorLogger(this, labelBus);
        wifiLogger   = new WifiLogger(this, labelBus);
        gnssLogger   = new GnssLogger(this, labelBus);
        cellLogger   = new CellLogger(this, labelBus);
        audioLogger  = new AudioLogger(this, labelBus);

        // Start all
        startAllLoggers();

        // Persistent foreground service
        startForeground(NOTIF_ID, buildNotification("Logging sensors…"));
        isInitialized = true;

        Log.i(TAG, "LoggerService initialized and started.");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // On Android 9 and below, ensure storage permission
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "WRITE_EXTERNAL_STORAGE missing on legacy device — stopping service.");
            stopSelf();
            return START_NOT_STICKY;
        }

        if (!isInitialized) {
            Log.i(TAG, "Reinitializing loggers after restart...");
            FileWriterQueue.init(getApplicationContext());
            startAllLoggers();
            isInitialized = true;
        }

        return START_STICKY;
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------
    /** Starts all logger modules safely. */
    private void startAllLoggers() {
        try {
            if (sensorLogger != null) sensorLogger.start();
            if (wifiLogger   != null) wifiLogger.start();
            if (gnssLogger   != null) gnssLogger.start();
            if (cellLogger   != null) cellLogger.start();
            if (audioLogger  != null) audioLogger.start();
        } catch (Exception e) {
            Log.e(TAG, "Error starting loggers", e);
        }
    }

    /** Receives label updates from MainActivity and updates LabelBus. */
    private void registerLabelReceiver() {
        labelReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String cat = intent.getStringExtra("category");
                String val = intent.getStringExtra("value");
                if (cat == null || val == null) return;

                switch (cat) {
                    case "place":     labelBus.setPlace(val);     break;
                    case "transport": labelBus.setTransport(val); break;
                    case "motion":    labelBus.setMotion(val);    break;
                    case "pose":      labelBus.setPose(val);      break;
                    default:
                        Log.w(TAG, "Unknown label category: " + cat);
                }
            }
        };
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(labelReceiver, new IntentFilter("LABEL_UPDATE"));
    }

    // ---------------------------------------------------------------------
    // Cleanup
    // ---------------------------------------------------------------------
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Service destroying, stopping loggers...");

        try {
            if (sensorLogger != null) sensorLogger.stop();
            if (wifiLogger   != null) wifiLogger.stop();
            if (gnssLogger   != null) gnssLogger.stop();
            if (cellLogger   != null) cellLogger.stop();
            if (audioLogger  != null) audioLogger.stop();

            // ✅ Flush and close all files
            FileWriterQueue.exportAll();

            Log.i(TAG, "All writers flushed and closed.");
        } catch (Exception e) {
            Log.e(TAG, "Error during shutdown", e);
        }

        try {
            if (labelReceiver != null)
                LocalBroadcastManager.getInstance(this).unregisterReceiver(labelReceiver);
        } catch (Exception ignored) {}

        stopForeground(true);
        stopSelf();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not a bound service
    }

    // ---------------------------------------------------------------------
    // Foreground notification helpers
    // ---------------------------------------------------------------------
    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID,
                    "Data Logger",
                    NotificationManager.IMPORTANCE_LOW
            );
            ch.setDescription("Background data logging channel");
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    private Notification buildNotification(String text) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("IOD Logger")
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    /** Accessor for current label state (optional). */
    public LabelBus getLabelBus() {
        return labelBus;
    }
}
