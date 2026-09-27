import joblib
import pandas as pd

# =========================
# LOAD TRAINED MODEL
# =========================

model = joblib.load("rf_model.pkl")

# =========================
# LABEL MAP
# =========================

labels = {
    1: "NORMAL",
    2: "STRESS",
    3: "ACTIVE",
    4: "RELAXED"
}

# =========================
# USER INPUT
# =========================

print("\nENTER SENSOR FEATURES\n")

ecg_mean = float(input("ECG_MEAN: "))
ecg_std = float(input("ECG_STD: "))
eda_mean = float(input("EDA_MEAN: "))
temp_mean = float(input("TEMP_MEAN: "))
motion = float(input("MOTION: "))

# =========================
# CREATE INPUT DATAFRAME
# =========================

sample = pd.DataFrame([[
    ecg_mean,
    ecg_std,
    eda_mean,
    temp_mean,
    motion
]], columns=[
    'ECG_MEAN',
    'ECG_STD',
    'EDA_MEAN',
    'TEMP_MEAN',
    'MOTION'
])

# =========================
# PREDICT
# =========================

prediction = model.predict(sample)[0]

print("\nPREDICTED STATE:")
print(labels[prediction])

