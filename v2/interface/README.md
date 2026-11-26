# Edge AI Wearable Health Monitor

Full-stack laptop dashboard for the ESP32-S3 Edge AI Wearable Patch.

## Architecture

```
ESP32-S3 (BLE Peripheral)
    ↓  BLE Notifications @ 10 Hz
Python Backend (BLE Central)
  ├─ Bleak   — BLE connection & notification handler
  ├─ FastAPI — REST API + WebSocket server
  └─ SQLite  — local historical storage
    ↓  WebSocket (ws://localhost:8000/ws)
React Frontend
  ├─ 7-page dashboard (Overview, ECG, Vitals, AI, History, Device, Settings)
  └─ Recharts — live & historical graphs
```

## Tech Stack

| Layer      | Technology        | Why                                    |
|------------|-------------------|----------------------------------------|
| BLE        | Bleak 0.22        | Cross-platform BLE in pure Python      |
| Backend    | FastAPI + Uvicorn | Async, fast, WebSocket support         |
| Storage    | SQLite            | Zero-config, no external server needed |
| Real-time  | WebSocket         | Push updates to all browser tabs       |
| Frontend   | React 18          | Component model, hooks                 |
| Charts     | Recharts          | React-native charting library          |

## Requirements

- Python 3.8 or higher
- Node.js 16 or higher
- Bluetooth adapter on your laptop

## Quick Start

### Terminal 1 — Backend

```bash
cd backend
pip install -r requirements.txt
python main.py
```

### Terminal 2 — Frontend

```bash
cd frontend
npm install
npm start
```

### Terminal 3 (optional) — Open browser

```
http://localhost:3000
```

### Connect to ESP32

1. Power on the ESP32-S3 wearable patch
2. In the web UI, go to **Device Status**
3. Click **Connect** (auto-finds `EdgeAI_Wearable`)
   — or use **Scan for Devices** to pick by MAC address

## Project Structure

```
wearable-health-monitor/
├── backend/
│   ├── main.py            ← FastAPI app, REST endpoints, WebSocket
│   ├── ble_client.py      ← Bleak BLE client with auto-reconnect
│   ├── database.py        ← SQLite schema and queries
│   ├── websocket_manager.py ← Multi-client WebSocket broadcast
│   ├── models.py          ← Pydantic data models
│   └── requirements.txt
│
├── frontend/
│   ├── public/
│   │   └── index.html
│   ├── src/
│   │   ├── App.js         ← Router, page wiring
│   │   ├── index.js
│   │   ├── hooks/
│   │   │   └── useWearable.js  ← WebSocket + REST hook
│   │   ├── utils/
│   │   │   └── labels.js       ← AI class labels & colour helpers
│   │   ├── styles/
│   │   │   └── global.css      ← Design system tokens & layout
│   │   └── components/
│   │       ├── shared/
│   │       │   └── Sidebar.js
│   │       └── pages/
│   │           ├── Overview.js
│   │           ├── ECGMonitor.js
│   │           ├── VitalSigns.js
│   │           ├── AIResults.js
│   │           ├── HistoricalTrends.js
│   │           ├── DeviceStatus.js
│   │           └── Settings.js
│   └── package.json
│
├── start_backend.sh
├── start_frontend.sh
└── README.md
```

## REST API Reference

| Method | Endpoint              | Description                          |
|--------|-----------------------|--------------------------------------|
| GET    | /api/status           | System status, BLE state, packet rate|
| GET    | /api/latest           | Most recent health packet            |
| GET    | /api/history          | Paginated history (?limit=&offset=)  |
| GET    | /api/history/ecg      | ECG trend (?minutes=N)               |
| GET    | /api/history/vitals   | Vitals trend (?minutes=N)            |
| POST   | /api/ble/scan         | Scan for BLE devices                 |
| POST   | /api/ble/connect      | Connect (?address=XX:XX:…)           |
| POST   | /api/ble/disconnect   | Disconnect                           |
| DELETE | /api/history          | Clear all records                    |
| WS     | /ws                   | Live data WebSocket                  |

## WebSocket Payload

```json
{
  "type": "live_data",
  "timestamp": "2024-01-15T10:30:00.123",
  "packet_rate": 9.8,
  "ble_connected": true,
  "signal_quality": "Good",
  "data": {
    "raw":      { "ecg": 1234, "temp": 36.8, "ir": 70000, "red": 65000, "ax": 100, "ay": 50, "az": 16000 },
    "features": { "ecg_filt": 1200, "rr": 800, "hr": 75.0, "hrv": 20.0, "motion": 1.1 },
    "ai":       { "ecg_class": 0, "stress_class": 1, "status": 1 }
  }
}
```

## AI Class Reference

| Code | ECG Class    | Stress Class | Status   |
|------|--------------|--------------|----------|
| 0    | Normal       | Low          | Normal   |
| 1    | Arrhythmia   | Medium       | Monitor  |
| 2    | Unknown      | High         | Alert    |

## BLE Details

- **Device name**: `EdgeAI_Wearable`
- **Service UUID**: `12345678-1234-1234-1234-123456789001`
- **Characteristic UUID**: `12345678-1234-1234-1234-123456789002`
- **Protocol**: BLE Notifications (no polling required)
- **Rate**: 100 ms / ~10 Hz

## Notes

- The ESP32 firmware is **not modified** in any way. This project is purely the laptop-side client.
- All data stays **100% local** — no cloud, no external services.
- The SQLite database file (`backend/health_data.db`) is created automatically on first run.
- The backend auto-reconnects to the BLE device if the connection drops.
