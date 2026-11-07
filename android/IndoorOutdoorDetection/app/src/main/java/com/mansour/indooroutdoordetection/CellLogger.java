package com.mansour.indooroutdoordetection;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.telephony.*;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;

/**
 * Logs cellular information (LTE, NR, WCDMA, GSM) with a unified 21-column format.
 * Each record aligns with the header fields for reliable CSV parsing.
 * Labels are logged separately in labels.csv.
 */
public class CellLogger {

    private final Context ctx;
    private final TelephonyManager tm;
    private final Handler h = new Handler(Looper.getMainLooper());
    private final int periodMs = 3000;

    // Placeholder for missing numeric values
    private static final int NA = -9999;

    // --- Unified CSV Header (21 columns, no label) ---
    private static final String H =
            "timestamp_ms,rat,registered,mccmnc,ci,pci,tac_lac,dbm,rsrp,rsrq,rssnr,cqi,ta," +
                    "ssRsrp,ssRsrq,ssSinr,csiRsrp,csiRsrq,csiSinr,level\n";

    public CellLogger(Context ctx, LabelBus labels) {
        this.ctx = ctx;
        this.tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
        FileWriterQueue.enqueueHeader("cell_snapshot.csv", H);
    }

    // ---------------------------------------------------------------------
    // Lifecycle
    // ---------------------------------------------------------------------
    public void start() { h.post(tick); }
    public void stop()  { h.removeCallbacksAndMessages(null); }

    // ---------------------------------------------------------------------
    // Periodic Sampling
    // ---------------------------------------------------------------------
    private final Runnable tick = new Runnable() {
        @SuppressLint("MissingPermission")
        @Override
        public void run() {
            long ts = System.currentTimeMillis();
            List<CellInfo> list = tm.getAllCellInfo();

            if (list != null && !list.isEmpty()) {
                for (CellInfo ci : list) {
                    boolean reg = ci.isRegistered();

                    try {
                        // ---------------- LTE ----------------
                        if (ci instanceof CellInfoLte) {
                            CellInfoLte c = (CellInfoLte) ci;
                            CellSignalStrengthLte s = c.getCellSignalStrength();
                            CellIdentityLte id = c.getCellIdentity();

                            int dbm = safeGet(s, "getDbm", NA);
                            int rsrp = safeGet(s, "getRsrp", NA);
                            int rsrq = safeGet(s, "getRsrq", NA);
                            int rssnr = safeGet(s, "getRssnr", NA);
                            int cqi = safeGet(s, "getCqi", NA);
                            int ta = safeGet(s, "getTimingAdvance", NA);
                            int ssRsrp = safeGet(s, "getSsRsrp", NA);
                            int ssRsrq = safeGet(s, "getSsRsrq", NA);
                            int ssSinr = safeGet(s, "getSsSinr", NA);
                            int csiRsrp = safeGet(s, "getCsiRsrp", NA);
                            int csiRsrq = safeGet(s, "getCsiRsrq", NA);
                            int csiSinr = safeGet(s, "getCsiSinr", NA);
                            int level = safeGet(s, "getLevel", NA);

                            String line = null;
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                                line = String.format(Locale.US,
                                        "%d,LTE,%d,%s,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d\n",
                                        ts, reg ? 1 : 0,
                                        safeJoin(id.getMccString(), id.getMncString()),
                                        id.getCi(), id.getPci(), id.getTac(),
                                        dbm, rsrp, rsrq, rssnr, cqi, ta,
                                        ssRsrp, ssRsrq, ssSinr,
                                        csiRsrp, csiRsrq, csiSinr,
                                        level);
                            }
                            FileWriterQueue.enqueue("cell_snapshot.csv", line);
                        }

                        // ---------------- NR (5G) ----------------
                        else if (Build.VERSION.SDK_INT >= 29 && ci instanceof CellInfoNr) {
                            CellInfoNr c = (CellInfoNr) ci;
                            CellSignalStrength sBase = c.getCellSignalStrength();
                            CellIdentityNr id = (CellIdentityNr) c.getCellIdentity();

                            int dbm = safeGet(sBase, "getDbm", NA);
                            int ssRsrp = safeGet(sBase, "getSsRsrp", NA);
                            int ssRsrq = safeGet(sBase, "getSsRsrq", NA);
                            int ssSinr = safeGet(sBase, "getSsSinr", NA);
                            int csiRsrp = safeGet(sBase, "getCsiRsrp", NA);
                            int csiRsrq = safeGet(sBase, "getCsiRsrq", NA);
                            int csiSinr = safeGet(sBase, "getCsiSinr", NA);
                            int level = safeGet(sBase, "getLevel", NA);
                            String mccmnc = safeJoin(id.getMccString(), id.getMncString());

                            String line = String.format(Locale.US,
                                    "%d,NR,%d,%s,,,,%d,,,,,%d,%d,%d,%d,%d,%d,%d,%d,%d\n",
                                    ts, reg ? 1 : 0, mccmnc,
                                    dbm,
                                    ssRsrp, ssRsrq, ssSinr,
                                    csiRsrp, csiRsrq, csiSinr,
                                    level);
                            FileWriterQueue.enqueue("cell_snapshot.csv", line);
                        }

                        // ---------------- WCDMA (3G) ----------------
                        else if (ci instanceof CellInfoWcdma) {
                            CellInfoWcdma c = (CellInfoWcdma) ci;
                            CellSignalStrengthWcdma s = c.getCellSignalStrength();
                            CellIdentityWcdma id = c.getCellIdentity();

                            int dbm = safeGet(s, "getDbm", NA);
                            int level = safeGet(s, "getLevel", NA);

                            String line = null;
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                                line = String.format(Locale.US,
                                        "%d,WCDMA,%d,%s,%d,,%d,%d,%d,,,,,,,,,,,,%d\n",
                                        ts, reg ? 1 : 0,
                                        safeJoin(id.getMccString(), id.getMncString()),
                                        id.getCid(), id.getLac(),
                                        dbm, NA, NA, level);
                            }
                            FileWriterQueue.enqueue("cell_snapshot.csv", line);
                        }

                        // ---------------- GSM (2G) ----------------
                        else if (ci instanceof CellInfoGsm) {
                            CellInfoGsm c = (CellInfoGsm) ci;
                            CellSignalStrengthGsm s = c.getCellSignalStrength();
                            CellIdentityGsm id = c.getCellIdentity();

                            int dbm = safeGet(s, "getDbm", NA);
                            int level = safeGet(s, "getLevel", NA);

                            String line = null;
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                                line = String.format(Locale.US,
                                        "%d,GSM,%d,%s,%d,,%d,%d,,,,,,,,,,,,%d\n",
                                        ts, reg ? 1 : 0,
                                        safeJoin(id.getMccString(), id.getMncString()),
                                        id.getCid(), id.getLac(),
                                        dbm, level);
                            }
                            FileWriterQueue.enqueue("cell_snapshot.csv", line);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            h.postDelayed(this, periodMs);
        }
    };

    // ---------------------------------------------------------------------
    // Safe reflection getter for compatibility
    // ---------------------------------------------------------------------
    private static int safeGet(Object obj, String method, int def) {
        if (obj == null) return def;
        try {
            Method m = obj.getClass().getMethod(method);
            Object v = m.invoke(obj);
            if (v instanceof Integer) return (Integer) v;
        } catch (Exception ignored) {}
        return def;
    }

    // ---------------------------------------------------------------------
    // Helper
    // ---------------------------------------------------------------------
    private static String safeJoin(String a, String b) {
        if (a == null && b == null) return "";
        if (a == null) return b;
        if (b == null) return a;
        return a + "-" + b;
    }
}
