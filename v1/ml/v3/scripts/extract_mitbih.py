import os
import wfdb
import numpy as np
import pandas as pd

# -------------------------------------------------
# Paths
# -------------------------------------------------

DATASET_DIR = "../dataset/mitbih/physionet.org/files/mitdb/1.0.0"
OUTPUT_FILE = "../dataset/all_waveform_features.csv"

# -------------------------------------------------
# MIT-BIH label mapping
# -------------------------------------------------

NORMAL_LABELS = ['N', 'L', 'R', 'e', 'j']
ABNORMAL_LABELS = ['A', 'a', 'J', 'S', 'V', 'E', 'F', '/', 'f', 'Q']

rows = []

records = []

for file in os.listdir(DATASET_DIR):
    if file.endswith(".dat"):
        records.append(file.replace(".dat", ""))

records.sort()

print("Found records:", len(records))

# -------------------------------------------------
# Feature extraction
# -------------------------------------------------

for rec in records:

    try:
        signal, fields = wfdb.rdsamp(
            os.path.join(DATASET_DIR, rec)
        )

        ann = wfdb.rdann(
            os.path.join(DATASET_DIR, rec),
            "atr"
        )

        ecg = signal[:, 0]

        samples = ann.sample
        symbols = ann.symbol

        for i in range(1, len(samples)-1):

            symbol = symbols[i]

            if symbol not in NORMAL_LABELS and symbol not in ABNORMAL_LABELS:
                continue

            prev_peak = samples[i-1]
            curr_peak = samples[i]
            next_peak = samples[i+1]

            rr_interval = (next_peak - curr_peak) / 360.0

            start = max(0, curr_peak - 90)
            end = min(len(ecg), curr_peak + 90)

            beat = ecg[start:end]

            if len(beat) < 50:
                continue

            peak_amp = np.max(np.abs(beat))
            beat_std = np.std(beat)
            beat_energy = np.sum(beat**2)

            label = 0 if symbol in NORMAL_LABELS else 1

            rows.append([
                rr_interval,
                peak_amp,
                beat_std,
                beat_energy,
                label
            ])

        print("Processed:", rec)

    except Exception as e:
        print("Skipped:", rec, e)

# -------------------------------------------------
# Save CSV
# -------------------------------------------------

df = pd.DataFrame(
    rows,
    columns=[
        "RR_INTERVAL",
        "PEAK_AMP",
        "BEAT_STD",
        "BEAT_ENERGY",
        "LABEL"
    ]
)

df.to_csv(OUTPUT_FILE, index=False)

print("\nDataset Created")
print(df.head())
print("\nTotal Samples:", len(df))
print("Saved to:", OUTPUT_FILE)
