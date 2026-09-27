import pandas as pd
import joblib

from sklearn.ensemble import RandomForestClassifier

# ====================================
# LOAD ECG FEATURE DATASET
# ====================================

df = pd.read_csv("ecg_features.csv")

X = df.drop("LABEL", axis=1)

y = df["LABEL"]

# ====================================
# TRAIN MODEL
# ====================================

model = RandomForestClassifier(
    n_estimators=50,
    max_depth=6,
    random_state=42
)

model.fit(X, y)

# ====================================
# SAVE MODEL
# ====================================

joblib.dump(model, "ecg_rf_model.pkl")

print("\nMODEL SAVED:")
print("ecg_rf_model.pkl")
