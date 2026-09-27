import pandas as pd
from sklearn.ensemble import RandomForestClassifier

# =========================
# LOAD DATASET
# =========================

df = pd.read_csv("features.csv")

X = df.drop("LABEL", axis=1)
y = df["LABEL"]

# =========================
# TRAIN MODEL
# =========================

model = RandomForestClassifier(
    n_estimators=20,
    max_depth=5,
    random_state=42
)

model.fit(X, y)

# =========================
# MANUAL TEST SAMPLE
# =========================

sample = [[
    0.01,   # ECG_MEAN
    0.14,   # ECG_STD
    7.0,    # EDA_MEAN
    36.8,   # TEMP_MEAN
    0.4     # MOTION
]]

prediction = model.predict(sample)

print("\nPREDICTED LABEL:")
print(prediction[0])

