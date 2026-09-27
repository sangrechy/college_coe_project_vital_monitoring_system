import pandas as pd

from sklearn.model_selection import train_test_split
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import accuracy_score
from sklearn.metrics import classification_report

# ====================================
# LOAD ECG FEATURE DATASET
# ====================================

df = pd.read_csv("ecg_features.csv")

# ====================================
# INPUT FEATURES
# ====================================

X = df.drop("LABEL", axis=1)

# ====================================
# OUTPUT LABELS
# ====================================

y = df["LABEL"]

# ====================================
# TRAIN TEST SPLIT
# ====================================

X_train, X_test, y_train, y_test = train_test_split(
    X,
    y,
    test_size=0.2,
    random_state=42
)

# ====================================
# RANDOM FOREST MODEL
# ====================================

model = RandomForestClassifier(
    n_estimators=50,
    max_depth=6,
    random_state=42
)

# ====================================
# TRAIN MODEL
# ====================================

model.fit(X_train, y_train)

# ====================================
# PREDICTIONS
# ====================================

y_pred = model.predict(X_test)

# ====================================
# ACCURACY
# ====================================

accuracy = accuracy_score(y_test, y_pred)

print("\nACCURACY:")
print(round(accuracy, 4))

# ====================================
# REPORT
# ====================================

print("\nCLASSIFICATION REPORT:")
print(classification_report(y_test, y_pred))

# ====================================
# FEATURE IMPORTANCE
# ====================================

print("\nFEATURE IMPORTANCE:\n")

for name, importance in zip(X.columns, model.feature_importances_):
    print(f"{name}: {importance:.4f}")
