# 📡 Edge AI Wearable Health Monitor

An **ESP32-S3 based wearable health monitoring system** that collects ECG, heart-rate/optical, temperature, and motion data from multiple sensors. The collected data is processed on the edge, where AI models perform ECG and stress-related classification. The results are transmitted through **Bluetooth Low Energy (BLE)** to a FastAPI backend and displayed on a web dashboard. A separate simulator is included for testing the software without the physical wearable.

---

# 📸 Project Images

<div align="center">

<img width="850" alt="Edge AI Wearable Health Monitor" src="https://github.com/user-attachments/assets/f4e5d72b-f06f-4d34-8394-02a7ae7d833e" />

<br><br>

<img width="500" alt="Wearable Setup 1" src="https://github.com/user-attachments/assets/10d5982e-85c3-40f6-b865-a8a75438ef01" />

<br><br>

<img width="300" alt="Wearable Setup 2" src="https://github.com/user-attachments/assets/fd39f5d5-ba74-451b-b020-ba7a455e9960" />
<img width="300" alt="Wearable Setup 3" src="https://github.com/user-attachments/assets/6867e67f-5ece-426a-b807-85cbc8eced1d" />

</div>

---

# 🎥 Demo

<div align="center">

<img width="800" alt="Dashboard Demo 1" src="https://github.com/user-attachments/assets/4666d9b0-704f-4773-9263-22572835c88e" />

<br><br>

<img width="800" alt="Dashboard Demo 2" src="https://github.com/user-attachments/assets/336baab3-113e-4879-8640-57cf76702220" />

<br><br>

<img width="800" alt="Dashboard Demo 3" src="https://github.com/user-attachments/assets/143551f8-1c14-4855-9701-238f2be0face" />

<br><br>

<img width="800" alt="Dashboard Demo 4" src="https://github.com/user-attachments/assets/85bcf956-f952-42e1-9c0f-5279ae5bf862" />

</div>


---


# 📁 Project Directory Structure

```text
COE_SEM_4_VITAL_MOINTER_SYSTEM/
│
├── v2/
│   │
│   ├── UI/
│   │   └── backend/
│   │       ├── main.py
│   │       ├── ble_client.py
│   │       ├── database.py
│   │       ├── models.py
│   │       └── websocket_manager.py
│   │
│   ├── firmware/
│   │   └── edge_ai_firmware/
│   │       ├── edge_ai_firmware.ino
│   │       ├── ble_backend.cpp
│   │       ├── ble_backend.h
│   │       ├── ecg_predict.cpp
│   │       ├── ecg_predict.h
│   │       ├── stress_predict.cpp
│   │       ├── stress_predict.h
│   │       ├── fusion_predict.cpp
│   │       ├── fusion_predict.h
│   │       └── libraries/
│   │
│   ├── edgeai_simulator/
│   │   ├── main.py
│   │   ├── simulator.py
│   │   ├── history.py
│   │   ├── requirements.txt
│   │   └── run.sh
│   │
│   ├── sim/
│   │   └── frontend/
│   │       ├── package.json
│   │       ├── package-lock.json
│   │       ├── public/
│   │       └── src/
│   │
│   ├── dataset/
│   ├── exports/
│   ├── tests/
│   └── docs/
│
└── README.md
```

---

# 🌐 Web Backend Installation

### 1. Clone the Repository

```bash
git clone https://github.com/sangrechy/collage_project_coe_sem_4_vital_mointer_system.git
cd collage_project_coe_sem_4_vital_mointer_system
```

### 2. Install the Backend

```bash
cd v2/UI/backend
python3 -m venv venv
source venv/bin/activate
pip install fastapi uvicorn bleak
```

### 3. Run the Backend

```bash
python3 main.py
```

Backend:

```text
http://localhost:8000
```

Check:

```bash
curl http://localhost:8000/api/status
```

### 4. Install and Run the Web Interface

Open another terminal:

```bash
cd v2/sim/frontend
npm install
npm start
```

Open:

```text
http://localhost:3000
```

---

# 🔧 ESP32 Installation

The ESP32 firmware is located at:

```text
v2/firmware/edge_ai_firmware/
```

Open:

```text
edge_ai_firmware.ino
```

using **Arduino IDE** or another compatible ESP32 development environment.

Install the required libraries, select the **ESP32-S3** board and the correct USB port, then upload the firmware.

Open Serial Monitor at:

```text
115200 baud
```

---

# 🔌 Hardware Pinout

## 🫀 AD8232 — ECG

| AD8232 | ESP32-S3 |
| ------ | -------- |
| OUTPUT | GPIO 4   |
| VCC    | 3.3V     |
| GND    | GND      |

---

## ❤️ MAX30102 — Heart Rate / Optical Data

| MAX30102 | ESP32-S3 |
| -------- | -------- |
| SDA      | GPIO 8   |
| SCL      | GPIO 9   |
| VCC      | 3.3V     |
| GND      | GND      |

**I2C Address:**

```text
0x57
```

---

## 🌡️ MLX90614 — Temperature

| MLX90614 | ESP32-S3 |
| -------- | -------- |
| SDA      | GPIO 8   |
| SCL      | GPIO 9   |
| VCC      | 3.3V     |
| GND      | GND      |

**I2C Address:**

```text
0x5A
```

---

## 🏃 MPU6050 — Motion / Acceleration

| MPU6050 | ESP32-S3 |
| ------- | -------- |
| SDA     | GPIO 8   |
| SCL     | GPIO 9   |
| VCC     | 3.3V     |
| GND     | GND      |

**I2C Address:**

```text
0x68
```

All I2C sensors share:

```text
SDA → GPIO 8
SCL → GPIO 9
```

---

# 📶 BLE Connection

The wearable advertises as:

```text
EdgeAI_Wearable
```

**Service UUID:**

```text
12345678-1234-1234-1234-123456789001
```

**Characteristic UUID:**

```text
12345678-1234-1234-1234-123456789002
```

**Current Device Address:**

```text
DC:B4:D9:07:1F:81
```

Start the backend after powering on the ESP32. It will automatically attempt to connect to the wearable.

---

# ⚙️ Configuration / Changing BLE Address

BLE configuration is located in:

```text
v2/UI/backend/ble_client.py
```

Look for:

```python
DEVICE_NAME = "EdgeAI_Wearable"
DEVICE_ADDRESS = "DC:B4:D9:07:1F:81"

SERVICE_UUID = "12345678-1234-1234-1234-123456789001"
CHARACTERISTIC_UUID = "12345678-1234-1234-1234-123456789002"
```

Change these values if your ESP32 uses different BLE information.

---

# 🧪 Simulator

The simulator allows the project to be tested **without the ESP32 hardware**.

Location:

```text
v2/edgeai_simulator/
```

### Install

```bash
cd v2/edgeai_simulator
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
```

### Run

```bash
python main.py
```

Simulator:

```text
http://localhost:8000
```

Then start the frontend in another terminal:

```bash
cd v2/sim/frontend
npm install
npm start
```

Open:

```text
http://localhost:3000
```

The simulator generates test wearable data for the dashboard.

```text
Simulator
    ↓
FastAPI
    ↓
WebSocket
    ↓
React Dashboard
```

For real hardware:

```text
ESP32-S3
    ↓
BLE
    ↓
FastAPI Backend
    ↓
WebSocket
    ↓
React Dashboard
```

---

# 🛠️ Troubleshooting

### Backend not starting

```bash
source venv/bin/activate
python3 main.py
```

### ESP32 not detected

Check that:

* ESP32 is powered on
* Bluetooth is enabled
* `EdgeAI_Wearable` is advertising
* BLE configuration matches `ble_client.py`

### Dashboard shows no data

Make sure:

```text
Backend  → http://localhost:8000
Frontend → http://localhost:3000
```

If the ESP32 is unavailable, run the simulator.
