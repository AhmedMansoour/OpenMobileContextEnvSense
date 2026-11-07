package com.mansour.indooroutdoordetection;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Main Activity for the Indoor–Outdoor Detection data logger.
 * Logs each label update as a full contextual state:
 * timestamp_ms,place,transport,motion,pose
 */
public class MainActivity extends AppCompatActivity {

    private static final int PERM_REQ_CODE = 10;

    // --- UI elements ---
    private TextView statusText;
    private Button btnStart, btnStop;
    private Spinner spinnerPlace, spinnerTransport, spinnerMotion, spinnerPose;
    private RecyclerView logRecycler;
    private LogAdapter logAdapter;
    private final List<String> logLines = new ArrayList<>();

    // --- Label categories ---
    private final String[] places = {
            "unlabeled", "indoor_deep", "indoor_near_window", "semi_outdoor", "outdoor",
            "indoor_office","indoor_home","atrium","tunnel", "underground_floor", "glass_entrance","outdoor_train_platform","stadium","airport_terminal","covered_walkway", "footbridge", "underground_parking_garage", "parking_garage", "balcony",
            "shopping_mall", "elevator", "indoor_elevator","outdoor_elevator","indoor_stairwell", "outdoor_stairwell","bus station", "metro station"
    };
    private final String[] transports = {
            "unknown", "on_foot", "running", "bicycle", "scooter",
            "car", "bus", "train", "metro", "airplane"
    };
    private final String[] motions = {
            "unknown", "on_table", "standing", "walking","walking_with_minor_stops","jogging", "running",
            "sitting", "in_vehicle"
    };
    private final String[] poses = {
            "unknown", "on_table", "hand_holding_horizontal", "hand_texting", "hand_calling_ear", "pocket_front", "pocket_back",
            "bag", "hand_swinging", "armband", "mounted_dashboard","mixed"
    };

    // --- Current label state ---
    private String currentPlace = "unlabeled";
    private String currentTransport = "unknown";
    private String currentMotion = "unknown";
    private String currentPose = "unknown";

    // --- Live log receiver for console updates ---
    private final BroadcastReceiver liveReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String msg = intent.getStringExtra("msg");
            if (msg == null) return;
            logLines.add(0, msg);
            if (logLines.size() > 120) logLines.remove(logLines.size() - 1);
            logAdapter.notifyDataSetChanged();
        }
    };

    // ------------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ✅ Initialize FileWriterQueue once for this session
        FileWriterQueue.init(getApplicationContext());

        // ✅ Ensure header for label log (5-column format)
        FileWriterQueue.enqueueHeader("labels.csv", "timestamp_ms,place,transport,motion,pose\n");

        // --- Initialize UI references ---
        statusText = findViewById(R.id.statusText);
        btnStart = findViewById(R.id.btnStart);
        btnStop = findViewById(R.id.btnStop);
        spinnerPlace = findViewById(R.id.spinnerPlace);
        spinnerTransport = findViewById(R.id.spinnerTransport);
        spinnerMotion = findViewById(R.id.spinnerMotion);
        spinnerPose = findViewById(R.id.spinnerPose);
        logRecycler = findViewById(R.id.logRecycler);

        // --- Setup RecyclerView ---
        logAdapter = new LogAdapter(logLines);
        logRecycler.setLayoutManager(new LinearLayoutManager(this));
        logRecycler.setAdapter(logAdapter);

        // --- Setup label spinners ---
        setSpinnerWithHint(spinnerPlace, places, "Place", "place");
        setSpinnerWithHint(spinnerTransport, transports, "Transport", "transport");
        setSpinnerWithHint(spinnerMotion, motions, "Motion", "motion");
        setSpinnerWithHint(spinnerPose, poses, "Pose", "pose");

        // --- Button actions ---
        btnStart.setOnClickListener(v -> startLogging());
        btnStop.setOnClickListener(v -> stopLogging());

        // --- Permissions ---
        checkPermissions();

        // Request "All Files Access" on Android 11+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }

        // --- Register live log receiver ---
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(liveReceiver, new IntentFilter("LIVE_LOG"));

        statusText.setText("Ready. Press START to begin logging.");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(liveReceiver);
    }

    // ------------------------------------------------------------------------
    // Spinner setup helpers
    // ------------------------------------------------------------------------
    private void setSpinnerWithHint(Spinner spinner, String[] itemsArray, String hintText, String category) {
        List<String> items = new ArrayList<>();
        items.add(hintText);
        Collections.addAll(items, itemsArray);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, items) {

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView tv = (TextView) view;
                tv.setTextColor(position == 0 ? Color.parseColor("#FF4444") : Color.WHITE);
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) view;
                tv.setTextColor(position == 0 ? Color.parseColor("#FF4444") : Color.WHITE);
                tv.setBackgroundColor(Color.parseColor("#2A2A2A"));
                return view;
            }
        };

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(0, false);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            private boolean userHasInteracted = false;

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) return; // ignore the hint row

                // Only log after first manual interaction
                if (userHasInteracted) {
                    String selected = items.get(position);
                    updateAndLogLabel(category, selected);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}

            // This detects real touches (not automatic init)
            {
                spinner.setOnTouchListener((v, event) -> {
                    userHasInteracted = true;
                    return false;
                });
            }
        });

    }

    // ------------------------------------------------------------------------
    // Permissions
    // ------------------------------------------------------------------------
    private void checkPermissions() {
        String[] perms = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        };
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            perms = append(perms, Manifest.permission.ACCESS_BACKGROUND_LOCATION);

        List<String> needList = new ArrayList<>();
        for (String p : perms)
            if (ActivityCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED)
                needList.add(p);

        if (!needList.isEmpty())
            ActivityCompat.requestPermissions(this, needList.toArray(new String[0]), PERM_REQ_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERM_REQ_CODE) {
            boolean allGranted = true;
            for (int g : grantResults)
                if (g != PackageManager.PERMISSION_GRANTED)
                    allGranted = false;
            Toast.makeText(this,
                    allGranted ? "All permissions granted." : "Some permissions denied!",
                    Toast.LENGTH_LONG).show();
        }
    }

    private String[] append(String[] arr, String value) {
        String[] newArr = new String[arr.length + 1];
        System.arraycopy(arr, 0, newArr, 0, arr.length);
        newArr[arr.length] = value;
        return newArr;
    }

    // ------------------------------------------------------------------------
    // Start / Stop Logging Service
    // ------------------------------------------------------------------------
    private void startLogging() {
        Intent i = new Intent(this, LoggerService.class);
        startService(i);
        statusText.setText("Logging service started.");
        Toast.makeText(this, "Logger started", Toast.LENGTH_SHORT).show();
    }

    private void stopLogging() {
        Intent i = new Intent(this, LoggerService.class);
        stopService(i);
        statusText.setText("Logging service stopped.");
        Toast.makeText(this, "Logger stopped", Toast.LENGTH_SHORT).show();

        File zipFile = FileWriterQueue.zipAll(getApplicationContext());
        if (zipFile != null)
            Toast.makeText(this, "Logs saved: " + zipFile.getAbsolutePath(),
                    Toast.LENGTH_LONG).show();
    }

    // ------------------------------------------------------------------------
    // Label update and full-context log
    // ------------------------------------------------------------------------
    private void updateAndLogLabel(String category, String value) {
        // 1️⃣ Send the update to LoggerService (which updates LabelBus)
        Intent i = new Intent("LABEL_UPDATE");
        i.putExtra("category", category);
        i.putExtra("value", value);
        LocalBroadcastManager.getInstance(this).sendBroadcast(i);

        // 2️⃣ Update the local copy (for UI and later baseline check)
        switch (category) {
            case "place": currentPlace = value; break;
            case "transport": currentTransport = value; break;
            case "motion": currentMotion = value; break;
            case "pose": currentPose = value; break;
        }

        // 3️⃣ Optional UI feedback
        statusText.setText(String.format(Locale.US,
                "Label updated: %s → %s", category, value));

        // 4️⃣ Log to RecyclerView for debugging (optional)
        logLines.add(0, String.format(Locale.US,
                "%d,%s,%s", System.currentTimeMillis(), category, value));
        if (logLines.size() > 120) logLines.remove(logLines.size() - 1);
        logAdapter.notifyDataSetChanged();

        // 5️⃣ Check if all four label categories are initialized at least once
        if (!currentPlace.equals("unlabeled") &&
                !currentTransport.equals("unknown") &&
                !currentMotion.equals("unknown") &&
                !currentPose.equals("unknown")) {
            // Everything initialized → force full-state log in LoggerService
            Intent baseline = new Intent("LABEL_UPDATE");
            baseline.putExtra("category", "sync");
            baseline.putExtra("value", "baseline");
            LocalBroadcastManager.getInstance(this).sendBroadcast(baseline);
        }
    }


}
