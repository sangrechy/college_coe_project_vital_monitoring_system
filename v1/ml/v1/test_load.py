import pickle

path = "/run/media/sangrechy/STUDIES/PROJECTS/COE_SEM_4_VITAL_MOINTER_SYSTEM/v1/dataset/archive/WESAD/S2/S2.pkl"

with open(path, "rb") as file:
    data = pickle.load(file, encoding="latin1")

print("\nTOP LEVEL:")
print(data.keys())

print("\nWRIST SIGNALS:")
print(data['signal']['wrist'].keys())

print("\nCHEST SIGNALS:")
print(data['signal']['chest'].keys())

print("\nLABEL SHAPE:")
print(data['label'].shape)
