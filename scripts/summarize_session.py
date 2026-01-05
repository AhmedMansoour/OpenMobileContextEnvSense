\
import argparse
import zipfile
from io import BytesIO
import pandas as pd

def safe_read_csv(zf: zipfile.ZipFile, name: str) -> pd.DataFrame:
    with zf.open(name) as f:
        try:
            return pd.read_csv(f)
        except Exception:
            f.seek(0)
            return pd.read_csv(f, engine="python", on_bad_lines="skip")

def summarize_zip(zip_path: str):
    with zipfile.ZipFile(zip_path, "r") as zf:
        csvs = [n for n in zf.namelist() if n.endswith(".csv")]
        rows = []
        for name in sorted(csvs):
            df = safe_read_csv(zf, name)
            n = len(df)
            rate = None
            span = None
            if "timestamp_ms" in df.columns and n > 1:
                t = pd.to_numeric(df["timestamp_ms"], errors="coerce").dropna().astype("int64")
                if len(t) > 1:
                    dt = t.sort_values().diff().dropna()
                    dt = dt[dt > 0]
                    if len(dt) > 0:
                        rate = 1000.0 / float(dt.median())
                    span = (int(t.max()) - int(t.min())) / 1000.0
            rows.append({
                "file": name,
                "rows": n,
                "approx_rate_hz": None if rate is None else round(rate, 3),
                "time_span_s": None if span is None else round(span, 1),
                "columns": list(df.columns),
            })
        out = pd.DataFrame(rows)
        print(out[["file", "rows", "approx_rate_hz", "time_span_s"]].to_string(index=False))

        # Labels (if present)
        for cand in [n for n in csvs if n.endswith("/labels.csv") or n.endswith("labels.csv")]:
            lab = safe_read_csv(zf, cand)
            if "place" in lab.columns:
                print("\nPlace label counts:\n", lab["place"].value_counts())
            if "motion" in lab.columns:
                print("\nMotion label counts:\n", lab["motion"].value_counts())

if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--zip", required=True, help="Path to a session zip (IOD_Record_*.zip)")
    args = ap.parse_args()
    summarize_zip(args.zip)
