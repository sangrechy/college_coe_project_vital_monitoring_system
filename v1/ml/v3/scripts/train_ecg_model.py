import pandas as pd
import joblib

from sklearn.model_selection import train_test_split
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import accuracy_score
from sklearn.metrics import classification_report

# -----------------------------
# Load Dataset
# -----------------------------

df = pd.read_csv("../dataset/all_waveform_features.csv")

X = df[
    [
        "RR_INTERVAL",
        "PEAK_AMP",
        "BEAT_STD",
        "BEAT_ENERGY"
    ]
]

y = df["LABEL"]

# -----------------------------
# Split
# -----------------------------

X_train, X_test, y_train, y_test = train_test_split(
    X,
    y,
    test_size=0.2,
    random_state=42,
    stratify=y
)

# -----------------------------
# Train
# -----------------------------

model = RandomForestClassifier(
    n_estimators=50,
    max_depth=10,
    random_state=42,
    n_jobs=-1
)

model.fit(X_train, y_train)

# -----------------------------
# Evaluate
# -----------------------------

pred = model.predict(X_test)

print("\nAccuracy:")
print(accuracy_score(y_test, pred))

print("\nClassification Report:")
print(classification_report(y_test, pred))

# -----------------------------
# Save
# -----------------------------

joblib.dump(
    model,
    "../models/ecg_rf_model.pkl"
)

print("\nModel Saved:")
print("../models/ecg_rf_model.pkl")
