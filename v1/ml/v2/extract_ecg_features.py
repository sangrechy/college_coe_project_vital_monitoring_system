import pickle
import numpy as np
import pandas as pd

from scipy.signal import find_peaks

# ====================================
# LOAD WESAD DATASET
# ====================================

path = "/run/media/sangrechy/STUDIES/PROJECTS/COE_SEM_4_VITAL_MOINTER_SYSTEM/v1/dataset/archive/WESAD/S2/S2.pkl"

with open(path, "rb") as file:
    data = pickle.load(file, encoding="latin1")

# ====================================
# EXTRACT CHEST SIGNALS
# ====================================

chest = data['signal']['chest']

ecg = chest['ECG'].flatten()

temp = chest['Temp'].flatten()

acc = chest['ACC']

labels = data['label']

# ====================================
# SETTINGS
# ====================================

sampling_rate = 700

window_size = sampling_rate * 10

step = sampling_rate * 5

features = []

# ====================================
# MAIN WINDOW LOOP
# ====================================

for start in range(0, len(ecg) - window_size, step):

    end = start + window_size

    # =========================
    # CURRENT WINDOWS
    # =========================

    ecg_win = ecg[start:end]

    temp_win = temp[start:end]

    acc_win = acc[start:end]

    label_win = labels[start:end]

    # =========================
    # MOST COMMON LABEL
    # =========================

    label = int(np.bincount(label_win).argmax())

    # Keep only useful labels
    if label not in [1, 2, 3, 4]:
        continue

    # =========================
    # ECG PEAK DETECTION
    # =========================

    peaks, _ = find_peaks(
        ecg_win,
        distance=300,
        height=np.mean(ecg_win)
    )

    # Need enough peaks
    if len(peaks) < 3:
        continue

    # =========================
    # RR INTERVALS
    # =========================

    rr_intervals = np.diff(peaks) / sampling_rate

    rr_mean = np.mean(rr_intervals)

    rr_std = np.std(rr_intervals)

    # =========================
    # HEART RATE
    # =========================

    hr = 60 / rr_mean

    # =========================
    # HEART RATE VARIABILITY
    # =========================

    hrv = np.std(rr_intervals)

    # =========================
    # MOTION MAGNITUDE
    # =========================

    motion = np.mean(
        np.sqrt(
            acc_win[:,0]**2 +
            acc_win[:,1]**2 +
            acc_win[:,2]**2
        )
    )

    # =========================
    # TEMPERATURE
    # =========================

    temp_mean = np.mean(temp_win)

    # =========================
    # STORE FEATURES
    # =========================

    features.append([
        hr,
        hrv,
        rr_mean,
        rr_std,
        motion,
        temp_mean,
        label
    ])

# ====================================
# CREATE DATAFRAME
# ====================================

df = pd.DataFrame(features, columns=[
    'HR',
    'HRV',
    'RR_MEAN',
    'RR_STD',
    'MOTION',
    'TEMP',
    'LABEL'
])

# ====================================
# SAVE CSV
# ====================================

df.to_csv("ecg_features.csv", index=False)

# ====================================
# RESULTS
# ====================================

print(df.head())

print("\nDATASET SHAPE:")
print(df.shape)

print("\nSaved as ecg_features.csv")
