package com.mansour.indooroutdoordetection;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.Locale;

/**
 * Logs onboard sensors (IMU + light + pressure + proximity + rotation)
 * with millisecond timestamps and sends compact summaries to the UI.
 *
 * Each CSV file has fixed columns and no label repetition.
 */
public class SensorLogger implements SensorEventListener {

    private final SensorManager sm;
    private final Context ctx;

    private final Sensor acc, gyro, mag, rot, press, prox, light;
    private boolean running = false;

    private float lastLight = 0f, lastPress = 0f;
    private long lastBroadcast = 0L;

    // ---------------------------------------------------------------------
    // CSV Headers (fixed, label-free)
    // ---------------------------------------------------------------------
    private static final String H_ACCEL  = "timestamp_ms,elapsed_nanos,ax,ay,az\n";
    private static final String H_GYRO   = "timestamp_ms,elapsed_nanos,gx,gy,gz\n";
    private static final String H_MAG    = "timestamp_ms,elapsed_nanos,mx,my,mz,mag_norm\n";
    private static final String H_ROT    = "timestamp_ms,elapsed_nanos,qx,qy,qz,qw,yaw_deg,pitch_deg,roll_deg\n";
    private static final String H_PRESS  = "timestamp_ms,elapsed_nanos,pressure_hpa\n";
    private static final String H_PROX   = "timestamp_ms,elapsed_nanos,proximity_cm\n";
    private static final String H_LIGHT  = "timestamp_ms,elapsed_nanos,light_lux\n";

    public SensorLogger(Context ctx, LabelBus labels) {
        this.ctx = ctx;
        sm = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);

        acc  = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyro = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mag  = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        rot  = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        press= sm.getDefaultSensor(Sensor.TYPE_PRESSURE);
        prox = sm.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        light= sm.getDefaultSensor(Sensor.TYPE_LIGHT);

        // Write all headers once
        FileWriterQueue.enqueueHeader("accel.csv", H_ACCEL);
        FileWriterQueue.enqueueHeader("gyro.csv",  H_GYRO);
        FileWriterQueue.enqueueHeader("mag.csv",   H_MAG);
        FileWriterQueue.enqueueHeader("rotation.csv", H_ROT);
        FileWriterQueue.enqueueHeader("pressure.csv", H_PRESS);
        FileWriterQueue.enqueueHeader("proximity.csv", H_PROX);
        FileWriterQueue.enqueueHeader("light.csv", H_LIGHT);
    }

    // ---------------------------------------------------------------------
    // Lifecycle
    // ---------------------------------------------------------------------
    public void start() {
        if (running) return;
        running = true;
        int fast = SensorManager.SENSOR_DELAY_GAME;
        int normal = SensorManager.SENSOR_DELAY_NORMAL;

        if (acc  != null) sm.registerListener(this, acc,  fast);
        if (gyro != null) sm.registerListener(this, gyro, fast);
        if (mag  != null) sm.registerListener(this, mag,  fast);
        if (rot  != null) sm.registerListener(this, rot,  fast);
        if (press!= null) sm.registerListener(this, press, normal);
        if (prox != null) sm.registerListener(this, prox,  normal);
        if (light!= null) sm.registerListener(this, light, SensorManager.SENSOR_DELAY_FASTEST);
    }

    public void stop() {
        if (!running) return;
        sm.unregisterListener(this);
        running = false;
    }

    // ---------------------------------------------------------------------
    // Sensor Data Logging
    // ---------------------------------------------------------------------
    @Override
    public void onSensorChanged(SensorEvent e) {
        long ts = System.currentTimeMillis();
        long n = e.timestamp;

        switch (e.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                FileWriterQueue.enqueue("accel.csv",
                        String.format(Locale.US,"%d,%d,%.5f,%.5f,%.5f\n",
                                ts,n,e.values[0],e.values[1],e.values[2]));
                break;

            case Sensor.TYPE_GYROSCOPE:
                FileWriterQueue.enqueue("gyro.csv",
                        String.format(Locale.US,"%d,%d,%.5f,%.5f,%.5f\n",
                                ts,n,e.values[0],e.values[1],e.values[2]));
                break;

            case Sensor.TYPE_MAGNETIC_FIELD:
                float mx=e.values[0], my=e.values[1], mz=e.values[2];
                double mnorm = Math.sqrt(mx*mx + my*my + mz*mz);
                FileWriterQueue.enqueue("mag.csv",
                        String.format(Locale.US,"%d,%d,%.3f,%.3f,%.3f,%.3f\n",
                                ts,n,mx,my,mz,mnorm));
                break;

            case Sensor.TYPE_ROTATION_VECTOR:
                float[] q = new float[4];
                SensorManager.getQuaternionFromVector(q, e.values);
                float[] R = new float[9], OR = new float[3];
                SensorManager.getRotationMatrixFromVector(R, e.values);
                SensorManager.getOrientation(R, OR);
                FileWriterQueue.enqueue("rotation.csv",
                        String.format(Locale.US,"%d,%d,%.6f,%.6f,%.6f,%.6f,%.2f,%.2f,%.2f\n",
                                ts,n,q[1],q[2],q[3],q[0],
                                Math.toDegrees(OR[0]),
                                Math.toDegrees(OR[1]),
                                Math.toDegrees(OR[2])));
                break;

            case Sensor.TYPE_PRESSURE:
                lastPress = e.values[0];
                FileWriterQueue.enqueue("pressure.csv",
                        String.format(Locale.US,"%d,%d,%.2f\n",ts,n,lastPress));
                break;

            case Sensor.TYPE_PROXIMITY:
                FileWriterQueue.enqueue("proximity.csv",
                        String.format(Locale.US,"%d,%d,%.3f\n",ts,n,e.values[0]));
                break;

            case Sensor.TYPE_LIGHT:
                lastLight = e.values[0];
                FileWriterQueue.enqueue("light.csv",
                        String.format(Locale.US,"%d,%d,%.2f\n",ts,n,lastLight));
                break;
        }

        // Compact broadcast every ~1s
        if (ts - lastBroadcast > 1000) {
            lastBroadcast = ts;
            String msg = String.format(Locale.US,
                    "[%tT] [IMU] Light=%.1f lx | Pressure=%.2f hPa",
                    ts, lastLight, lastPress);
            Intent i = new Intent("LIVE_LOG");
            i.putExtra("msg", msg);
            LocalBroadcastManager.getInstance(ctx).sendBroadcast(i);
        }
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
