import time
import math
import random

class Simulator:
    def __init__(self):
        self.hr = 72.0
        self.temp = 36.6
        self.time_step = 0.0
        
    def generate_tick(self):
        now_ms = int(time.time() * 1000)

        # 1. Natural Heart Rate Variability (HRV) & Temp Drift Simulation
        self.hr = max(65.0, min(95.0, self.hr + random.uniform(-0.8, 0.8)))
        self.temp = max(36.4, min(37.2, self.temp + random.uniform(-0.02, 0.02)))
        
        # Calculate features matching your Random Forest model expectations
        rr = int((60.0 / self.hr) * 1000)
        hrv = random.randint(18, 38)
        motion = round(random.uniform(0.02, 0.15), 2)

        # 2. High-Fidelity ECG Generator (Simulating True Clinical Morphology)
        # P, Q, R, S, T complex wave synthesis using parameterized Gaussians
        # Base heart cycle mapping
        frequency = self.hr / 60.0
        self.time_step += 0.5  # Advancing phase
        phase = (self.time_step * frequency) % 1.0

        # Baseline offset
        ecg_val = 2048 

        # Gaussian parameters: (Center Phase, Amplitude, Width)
        p_wave = (0.15, 35, 0.03)
        q_wave = (0.22, -45, 0.01)
        r_wave = (0.25, 520, 0.015)  # Dominant QRS Spike
        s_wave = (0.28, -110, 0.015)
        t_wave = (0.48, 85, 0.05)

        for center, amp, width in [p_wave, q_wave, r_wave, s_wave, t_wave]:
            diff = phase - center
            ecg_val += amp * math.exp(-(diff ** 2) / (2 * (width ** 2)))

        # Add natural high-frequency muscle artifact noise to raw line
        noise = random.randint(-8, 8)
        ecg_raw = int(ecg_val + noise)
        ecg_filt = int(ecg_val)  # Filtered line completely strips the noise

        # 3. Photo-plethysmography (PPG) Sensors & IMU Emulation
        ir = int(52000 + 1200 * math.sin(phase * 2 * math.pi))
        red = int(49000 + 950 * math.sin(phase * 2 * math.pi))
        
        ax = random.randint(-35, 35)
        ay = random.randint(-35, 35)
        az = 16384 + random.randint(-80, 80) # Sits near 1G gravity unit

        # 4. Predictor Class Outputs matching your random forest architecture
        ecg_class = random.choices([0, 1, 2, 3], weights=[0.92, 0.04, 0.02, 0.02])[0]
        stress_class = 1 if self.hr > 86.0 else 0
        
        # Fusion Status Logic
        if ecg_class > 0 and stress_class == 1:
            status_val = 2  # High Alert
        elif ecg_class > 0 or stress_class == 1:
            status_val = 1  # Guarded Warning
        else:
            status_val = 0  # Normal Baseline

        # Construct payload delivery maps
        data_payload = {
            "raw": {
                "ecg": ecg_raw, "temp": round(self.temp, 2), "ir": ir, "red": red,
                "ax": ax, "ay": ay, "az": az
            },
            "features": {
                "ecg_filt": ecg_filt, "rr": rr, "hr": int(self.hr), "hrv": hrv, "motion": motion
            },
            "ai": {
                "ecg_class": ecg_class, "stress_class": stress_class, "status": status_val
            }
        }

        vital_record = {
            "time": now_ms, "hr": int(self.hr), "spo2": random.randint(97, 99),
            "temp": round(self.temp, 2), "motion": motion
        }
        
        ecg_record = {
            "time": now_ms, "raw": ecg_raw, "filtered": ecg_filt
        }

        return data_payload, vital_record, ecg_record
