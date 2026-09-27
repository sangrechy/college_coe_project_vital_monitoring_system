import pickle
import numpy as np
import pandas as pd

# =========================
# LOAD DATA
# =========================

path = "/run/media/sangrechy/STUDIES/PROJECTS/COE_SEM_4_VITAL_MOINTER_SYSTEM/v1/dataset/archive/WESAD/S2/S2.pkl"

with open(path, "rb") as file:
    data = pickle.load(file, encoding="latin1")

# =========================
# EXTRACT SIGNALS
# =========================

chest = data['signal']['chest']

ecg = chest['ECG'].flatten()
eda = chest['EDA'].flatten()
temp = chest['Temp'].flatten()
acc = chest['ACC']

labels = data['label']

# =========================
# WINDOW SETTINGS
# =========================

WINDOW_SIZE = 3500
STEP_SIZE = 3500

# 700 Hz × 5 sec = 3500 samples

# =========================
# FEATURE EXTRACTION
# =========================

rows = []

for start in range(0, len(labels) - WINDOW_SIZE, STEP_SIZE):

    end = start + WINDOW_SIZE

    ecg_win = ecg[start:end]
    eda_win = eda[start:end]
    temp_win = temp[start:end]
    acc_win = acc[start:end]

    label_win = labels[start:end]

    # use most common label
    label = np.bincount(label_win.astype(int)).argmax()

    # skip undefined labels
    if label not in [1,2,3,4]:
        continue

    # simple features
    hr_feature = np.mean(ecg_win)
    ecg_std = np.std(ecg_win)

    eda_mean = np.mean(eda_win)

    temp_mean = np.mean(temp_win)

    acc_mag = np.sqrt(
        acc_win[:,0]**2 +
        acc_win[:,1]**2 +
        acc_win[:,2]**2
    )

    motion_mean = np.mean(acc_mag)

    rows.append([
        hr_feature,
        ecg_std,
        eda_mean,
        temp_mean,
        motion_mean,
        label
    ])

# =========================
# CREATE DATAFRAME
# =========================

df = pd.DataFrame(rows, columns=[
    'ECG_MEAN',
    'ECG_STD',
    'EDA_MEAN',
    'TEMP_MEAN',
    'MOTION',
    'LABEL'
])

print(df.head())

print("\nDATASET SHAPE:")
print(df.shape)

# SAVE CSV
df.to_csv("features.csv", index=False)

print("\nfeatures.csv saved successfully!")
