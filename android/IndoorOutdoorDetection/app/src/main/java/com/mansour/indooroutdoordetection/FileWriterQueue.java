package com.mansour.indooroutdoordetection;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Thread-safe asynchronous file writer for logging sensor data
 * during Indoor–Outdoor Detection (IOD) experiments.
 * Supports per-file writers, session-based directories,
 * and convenient export/zip functions.
 */
public class FileWriterQueue {
    private static final String TAG = "IOD_LOGGER";
    private static final ConcurrentHashMap<String, BufferedWriter> writers = new ConcurrentHashMap<>();
    private static final ExecutorService exec = Executors.newSingleThreadExecutor();
    private static File baseDir;
    private static volatile boolean inited = false;
    private static String sessionId;

    // ---------------------------------------------------------------------
    // Initialization
    // ---------------------------------------------------------------------
    public static synchronized void init(Context ctx) {
        if (inited) return;
        sessionId = String.valueOf(System.currentTimeMillis());

        // Preferred app-scoped external directory (always writable)
        baseDir = new File(ctx.getExternalFilesDir(null), "IOD_Record/" + sessionId);
        if (!baseDir.exists() && !baseDir.mkdirs()) {
            // Fallback for older Android versions
            File legacy = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS), "IOD_Record/" + sessionId);
            if (legacy.mkdirs()) baseDir = legacy;
        }

        inited = true;
        Log.d(TAG, "Logger initialized at: " + baseDir.getAbsolutePath());
    }

    // ---------------------------------------------------------------------
    // Header management
    // ---------------------------------------------------------------------
    /**
     * Writes a header to the file only if the file is empty or new.
     * Ensures consistent CSV column structure across all runs.
     */
    public static void enqueueHeader(final String fileName, final String headerLine) {
        if (!inited || baseDir == null) return;

        exec.submit(() -> {
            try {
                File f = new File(baseDir, fileName);
                if (!f.exists() || f.length() == 0) {
                    try (BufferedWriter bw = new BufferedWriter(new FileWriter(f, true))) {
                        bw.write(headerLine);
                        bw.flush();
                    }
                    Log.d(TAG, "Header written to: " + fileName);
                }
            } catch (IOException e) {
                Log.e(TAG, "Error writing header: " + fileName, e);
            }
        });
    }

    // ---------------------------------------------------------------------
    // Data writing
    // ---------------------------------------------------------------------
    /** Queues a line to be written asynchronously into a log file. */
    public static void enqueue(final String fileName, final String line) {
        if (!inited || baseDir == null) return;

        exec.submit(() -> {
            try {
                BufferedWriter bw = writers.get(fileName);
                if (bw == null) {
                    File f = new File(baseDir, fileName);
                    bw = new BufferedWriter(new FileWriter(f, true));
                    writers.put(fileName, bw);
                }
                bw.write(line);
            } catch (IOException e) {
                Log.e(TAG, "Error writing to file: " + fileName, e);
            }
        });
    }

    // ---------------------------------------------------------------------
    // Export / Zip
    // ---------------------------------------------------------------------
    /** Flushes and closes all open BufferedWriters. */
    public static synchronized void exportAll() {
        try {
            for (BufferedWriter bw : writers.values()) {
                if (bw != null) {
                    bw.flush();
                    bw.close();
                }
            }
            writers.clear();
            Log.d(TAG, "All writers flushed and closed successfully.");
        } catch (IOException e) {
            Log.e(TAG, "Error while exporting files", e);
        }
    }

    /** Zips the entire session folder into a single .zip file. */
    public static File zipAll(Context ctx) {
        exportAll(); // ensure everything is flushed

        File zipFile = new File(ctx.getExternalFilesDir(null),
                "IOD_Record_" + sessionId + ".zip");

        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            zipDirRecursive(baseDir, baseDir.getName(), zos);
            Log.d(TAG, "Zipped successfully: " + zipFile.getAbsolutePath());
            return zipFile;

        } catch (IOException e) {
            Log.e(TAG, "Error while zipping files", e);
            return null;
        }
    }

    private static void zipDirRecursive(File dir, String basePath, ZipOutputStream zos) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                zipDirRecursive(file, basePath + "/" + file.getName(), zos);
            } else {
                try (FileInputStream fis = new FileInputStream(file)) {
                    ZipEntry entry = new ZipEntry(basePath + "/" + file.getName());
                    zos.putNextEntry(entry);
                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, len);
                    }
                    zos.closeEntry();
                }
            }
        }
    }

    // ---------------------------------------------------------------------
    // Cleanup
    // ---------------------------------------------------------------------
    public static void cleanup() {
        exec.submit(() -> {
            deleteRecursive(baseDir);
            Log.d(TAG, "Session folder deleted: " + baseDir);
        });
    }

    private static void deleteRecursive(File fileOrDir) {
        if (fileOrDir == null || !fileOrDir.exists()) return;
        if (fileOrDir.isDirectory()) {
            File[] children = fileOrDir.listFiles();
            if (children != null) {
                for (File child : children) deleteRecursive(child);
            }
        }
        fileOrDir.delete();
    }

    // ---------------------------------------------------------------------
    // Optional: Share ZIP via Intent
    // ---------------------------------------------------------------------
    public static File prepareExportZip(Context ctx) {
        File zip = zipAll(ctx);
        if (zip != null && zip.exists()) {
            Log.d(TAG, "Ready to share zip: " + zip.getAbsolutePath());
        } else {
            Log.e(TAG, "Zip export failed.");
        }
        return zip;
    }
}
