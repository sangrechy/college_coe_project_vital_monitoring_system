import pandas as pd
import joblib

from sklearn.ensemble import RandomForestClassifier

# LOAD DATASET
df = pd.read_csv("features.csv")

X = df.drop("LABEL", axis=1)
y = df["LABEL"]

# TRAIN MODEL
model = RandomForestClassifier(
    n_estimators=20,
    max_depth=5,
    random_state=42
)

model.fit(X, y)

# SAVE MODEL
joblib.dump(model, "rf_model.pkl")

print("Model saved as rf_model.pkl")
