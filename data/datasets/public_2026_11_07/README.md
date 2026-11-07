# OpenMobileContextEnvSense — six-session public data release

Version **2026-11-07**. License: **CC BY 4.0** (data only). The repository's source code is separately licensed under MIT.

This public dataset contains **six synchronized smartphone field recordings**, presented as `session_01.zip` through `session_06.zip`. It includes GNSS satellite observations, cellular radio metrics, Wi-Fi scan statistics and anonymized access-point identity, inertial measurements, light, pressure, proximity and audio summary features, plus event-based environmental context annotations.

## Contents and provenance

Each ZIP contains 14 CSV streams: `labels`, `accel`, `gyro`, `mag`, `rotation`, `pressure`, `proximity`, `light`, `wifi_scan`, `wifi_ap`, `gnss_fix`, `gnss_sv`, `cell_snapshot`, and `audio`. The original recordings also contained raw NMEA, which has been intentionally withheld from this public edition. `manifest.json` provides the file-level row counts, schema fields, length, and SHA-256 archive digest for verification.

One of these six sessions corresponds to the sample previously listed in this repository; **do not count it as an additional seventh session**. The six recordings are field-collection examples, not six proven independent sites or participants, nor a benchmarking dataset establishing generalization across users/devices. Their availability is independent of the 108 published reports (105 independent evidence families) included in the review.

## Time and annotations

All public CSVs use `t_rel_ms`, a relative millisecond timestamp measured from the earliest observable event in each session. The same origin is shared by all streams within that session. **Absolute timestamps are removed**. `labels.csv` stores annotation-change events in four columns: `place`, `transport`, `motion`, and `pose`. A label remains active until a subsequent event only if the downstream analysis explicitly adopts a carry-forward rule; users should document how they convert event annotations into frame/window targets. Any labeling inconsistencies or transitions are preserved instead of silently corrected.

## De-identification

- **Removed:** exact GNSS latitude/longitude/altitude, raw NMEA sentences, original Wi-Fi SSIDs and BSSIDs, cell-network location identifiers (`mccmnc`, `ci`, `pci`, `tac_lac`), device-uptime counters, and absolute epoch timestamps.
- **Pseudonymized:** Wi-Fi `ap_id` and cellular `cell_id`. These are opaque, secret-keyed tokens, designed to be consistent across the six sessions for the same original identifiers; there is no published reverse lookup or key. Because the mapping is not published, the data cannot be joined to real network names or a geographic radio map by direct identifier lookup.
- **Retained:** numeric Wi-Fi RSS/channel fields, cellular signal quality/strength fields, GNSS satellite C/N0/geometry and fix quality (without coordinates), inertial and ambient measurements, and audio summary features. Raw microphone audio is not provided.
- **Caveat:** De-identification does not guarantee anonymity. High-rate sensor traces, behavior sequences, and wireless patterns may remain sensitive in some contexts. Users should treat them as research recordings and respect privacy requirements. The authors should verify collection permissions and release rights before publishing.

## Signal availability

Availability varies by session. For example, `session_05` has empty `wifi_scan.csv` and `wifi_ap.csv`, while `session_06` contains sparse Wi-Fi data. Missing or empty streams are actual recording limitations and must not be filled with fabricated observations. See `manifest.json` for counts for all streams and sessions.

## Recommended use

These examples are suitable for illustrating logging formats, synchronization, environmental label transitions, and measurement analysis. They are **not** a substitute for a well-controlled cross-site/device benchmark. Do not use random sensor-row splits to claim independent environmental generalization; train/test separation and any domain overlap must be reported explicitly.

## SHA-256 verification

Each file's SHA-256 is in `manifest.json` under the corresponding `session_id`. Please cite the dataset release and the accompanying manuscript if using these files in publications.
