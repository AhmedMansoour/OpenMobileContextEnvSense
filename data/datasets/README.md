# Public Sample Dataset (One Session)

This folder contains **one public sample session** intended to demonstrate the dataset structure and analysis workflow.

## Included file
- `IOD_Record_1761144837784_public.zip` (SHA256: `2fe9af07698112930e36c99402cf8b797ee0c4a0058970864f3e1039847215b3`)

## Session summary (from raw logs)
- Session span: 729.5 s (≈ 12.16 min)
- UTC start–end: 2025-10-22T14:54:24.082000+00:00 to 2025-10-22T15:06:33.620000+00:00
- Labels are event-level records in `labels.csv` (place / transport / motion / pose).

### Label distribution (place)
{
  "unlabeled": 3,
  "outdoor": 3,
  "elevator": 1,
  "semi_outdoor": 1,
  "tunnel": 1,
  "stairwell": 1,
  "indoor_deep": 1
}

### Label distribution (motion)
{
  "walking_with_minor_stops": 10,
  "unknown": 1
}

### Label distribution (pose)
{
  "hand_swinging": 5,
  "hand_holding_horizontal": 4,
  "unknown": 2
}

### Label distribution (transport)
{
  "on_foot": 11
}

## Files inside the session (raw structure)
The original session contains the following CSV logs (some may be empty, depending on permissions and phone state):

| file | rows | approx_rate_hz | time_span_s | columns |
| --- | --- | --- | --- | --- |
| accel.csv | 75861 | 100.0 | 729.5 | timestamp_ms, elapsed_nanos, ax, ay, az |
| audio.csv | 160 | 0.218 | 728.3 | timestamp_ms, rms, dbfs, peak |
| cell_snapshot.csv | 1152 | 0.331 | 729.0 | timestamp_ms, rat, registered, mccmnc, ci, pci, tac_lac, dbm ... |
| gnss_fix.csv | 391 | 0.999 | 614.6 | timestamp_ms, provider, lat, lon, alt_m, speed_mps, bearing_deg, acc_m ... |
| gnss_nmea.csv | 23704 | 1000.0 | 728.6 | timestamp_ms, talker, raw |
| gnss_sv.csv | 31193 | 1.0 | 728.6 | timestamp_ms, svid, constellation, usedInFix, cn0_dbhz, elev_deg, az_deg, cf_hz ... |
| gyro.csv | 45279 | 50.0 | 728.9 | timestamp_ms, elapsed_nanos, gx, gy, gz |
| labels.csv | 11 | 0.028 | 692.2 | timestamp_ms, place, transport, motion, pose |
| light.csv | 2082 | 2.857 | 729.3 | timestamp_ms, elapsed_nanos, light_lux |
| mag.csv | 67516 | 100.0 | 729.5 | timestamp_ms, elapsed_nanos, mx, my, mz, mag_norm |
| pressure.csv | 8534 | 12.5 | 729.5 | timestamp_ms, elapsed_nanos, pressure_hpa |
| proximity.csv | 83 | 12.5 | 722.0 | timestamp_ms, elapsed_nanos, proximity_cm |
| rotation.csv | 36457 | 50.0 | 729.5 | timestamp_ms, elapsed_nanos, qx, qy, qz, qw, yaw_deg, pitch_deg ... |
| wifi_ap.csv | 0 | — | — | timestamp_ms, bssid, ssid, frequency, level, centerFreq0, centerFreq1, channelWidth ... |
| wifi_scan.csv | 0 | — | — | timestamp_ms, elapsed_ms, ap_count, mean_level, strongest_level |

## Notes
- In this specific session, `wifi_scan.csv` and `wifi_ap.csv` contain headers but no rows (no Wi‑Fi records were logged).
- For public sharing, the provided zip removes `lat/lon` and excludes `gnss_nmea.csv`.
