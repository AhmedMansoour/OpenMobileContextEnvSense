package com.mansour.indooroutdoordetection;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread-safe label holder for Indoor–Outdoor Detection (IOD) experiments.
 * Keeps track of current semantic labels (place, transport, motion, pose)
 * and logs any label change into a dedicated "labels.csv" file.
 *
 * Format: timestamp_ms,place,transport,motion,pose
 */
public class LabelBus {
    private final AtomicReference<String> place     = new AtomicReference<>("unlabeled");
    private final AtomicReference<String> transport = new AtomicReference<>("unknown");
    private final AtomicReference<String> motion    = new AtomicReference<>("unknown");
    private final AtomicReference<String> pose      = new AtomicReference<>("unknown");

    private static final String HEADER = "timestamp_ms,place,transport,motion,pose\n";
    private static boolean headerWritten = false;

    // --- Static global instance for shared access ---
    private static LabelBus globalInstance;

    public LabelBus() {
        // Ensure header once per session
        if (!headerWritten) {
            FileWriterQueue.enqueueHeader("labels.csv", HEADER);
            headerWritten = true;
        }
        globalInstance = this; // set global reference
    }

    // ---------------------------------------------------------------------
    // Setters with automatic logging
    // ---------------------------------------------------------------------
    public void setPlace(String v) {
        if (!v.equals(place.get())) {
            place.set(v);
            logCurrent();
        }
    }

    public void setTransport(String v) {
        if (!v.equals(transport.get())) {
            transport.set(v);
            logCurrent();
        }
    }

    public void setMotion(String v) {
        if (!v.equals(motion.get())) {
            motion.set(v);
            logCurrent();
        }
    }

    public void setPose(String v) {
        if (!v.equals(pose.get())) {
            pose.set(v);
            logCurrent();
        }
    }

    // ---------------------------------------------------------------------
    // Accessors
    // ---------------------------------------------------------------------
    public String getPlace() { return place.get(); }
    public String getTransport() { return transport.get(); }
    public String getMotion() { return motion.get(); }
    public String getPose() { return pose.get(); }

    /** Returns a compact joined representation for debugging. */
    public String joined() {
        return place.get() + "|" + transport.get() + "|" + motion.get() + "|" + pose.get();
    }

    // ---------------------------------------------------------------------
    // Logging helper
    // ---------------------------------------------------------------------
    /** Logs current label state into labels.csv with timestamp in ms. */
    private void logCurrent() {
        String ts = Utils.timestampMs();
        String line = ts + "," +
                place.get() + "," +
                transport.get() + "," +
                motion.get() + "," +
                pose.get() + "\n";
        FileWriterQueue.enqueue("labels.csv", line);
    }

    // ---------------------------------------------------------------------
    // Global helper for MainActivity periodic access
    // ---------------------------------------------------------------------
    /**
     * Allows global access to the latest label values
     * from anywhere (e.g., MainActivity labelTick loop).
     */
    public static String getCurrent(String category) {
        if (globalInstance == null) return null;
        switch (category) {
            case "place": return globalInstance.place.get();
            case "transport": return globalInstance.transport.get();
            case "motion": return globalInstance.motion.get();
            case "pose": return globalInstance.pose.get();
            default: return null;
        }
    }

    /** Returns the current singleton LabelBus (may be null before initialization). */
    public static LabelBus getInstance() {
        return globalInstance;
    }
}
