# Privacy and De-identification Policy

This project collects smartphone sensor data for research on environmental context inference (e.g., indoor–outdoor detection). The public release follows a minimization and de-identification policy.

## Public sample dataset (this upload pack)
The included public dataset zip removes precise coordinates:
- `gnss_fix.csv`: latitude and longitude are removed.
- `gnss_nmea.csv`: excluded, because raw NMEA sentences can contain location information.

For the full release, further de-identification rules (e.g., BSSID hashing, SSID removal, and optional spatial coarsening) should be applied consistently across all sessions.
