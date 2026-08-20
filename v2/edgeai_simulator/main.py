import asyncio
import time
from contextlib import asynccontextmanager
from fastapi import FastAPI, WebSocket, WebSocketDisconnect
from fastapi.middleware.cors import CORSMiddleware
from typing import List

from history import store
from simulator import Simulator

# ==========================================
# LIFESPAN (Replaces @app.on_event)
# ==========================================
@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup: Start the background simulation loop
    sim_task = asyncio.create_task(simulation_loop())
    yield
    # Shutdown: Clean up the task when the server stops
    sim_task.cancel()

# Initialize FastAPI with the lifespan manager
app = FastAPI(lifespan=lifespan)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

sim = Simulator()
ble_connected = True

class ConnectionManager:
    def __init__(self):
        self.active_connections: List[WebSocket] = []

    async def connect(self, websocket: WebSocket):
        await websocket.accept()
        self.active_connections.append(websocket)

    def disconnect(self, websocket: WebSocket):
        if websocket in self.active_connections:
            self.active_connections.remove(websocket)

    async def broadcast(self, message: dict):
        for connection in self.active_connections:
            try:
                await connection.send_json(message)
            except Exception:
                pass

manager = ConnectionManager()

@app.get("/api/status")
async def get_status():
    return {
        "ble_connected": ble_connected,
        "ble_device_name": "EdgeAI_Wearable",
        "ble_device_address": "DC:B4:D9:07:1F:81",
        "packet_rate": 2,
        "uptime_seconds": int(time.time() - store.start_time),
        "db_record_count": len(store.vitals)
    }

@app.post("/api/ble/scan")
async def scan_ble():
    return [{"name": "EdgeAI_Wearable", "address": "DC:B4:D9:07:1F:81", "rssi": -42}]

@app.post("/api/ble/connect")
async def connect_ble():
    global ble_connected
    ble_connected = True
    return {"success": True}

@app.post("/api/ble/disconnect")
async def disconnect_ble():
    global ble_connected
    ble_connected = False
    return {"success": True}

@app.delete("/api/history")
async def clear_history():
    store.clear()
    return {"success": True}

@app.get("/api/history/vitals")
async def get_history_vitals(minutes: int = 10):
    return store.get_vitals(minutes)

@app.get("/api/history/ecg")
async def get_history_ecg(minutes: int = 10):
    return store.get_ecg(minutes)

@app.websocket("/ws")
async def websocket_endpoint(websocket: WebSocket):
    await manager.connect(websocket)
    try:
        while True:
            await websocket.receive_text()
    except WebSocketDisconnect:
        manager.disconnect(websocket)

async def simulation_loop():
    while True:
        await asyncio.sleep(0.5)
        data, vital, ecg = sim.generate_tick()
        store.add_record(vital, ecg)
        payload = {
            "type": "live_data",
            "ble_connected": ble_connected,
            "packet_rate": 2,
            "signal_quality": "Excellent",
            "data": data
        }
        await manager.broadcast(payload)

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
