"""
Edge AI Wearable Health Monitor - Backend
FastAPI + Bleak BLE + SQLite + WebSocket
"""

import asyncio
import json
import logging
import time
from contextlib import asynccontextmanager
from datetime import datetime
from typing import Optional

import uvicorn
from fastapi import FastAPI, WebSocket, WebSocketDisconnect
from fastapi.middleware.cors import CORSMiddleware

from ble_client import BLEClient
from database import Database
from models import HealthPacket
from websocket_manager import ConnectionManager

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s"
)
logger = logging.getLogger("main")

# Global instances
db = Database()
ble_client = BLEClient()
ws_manager = ConnectionManager()

# Packet rate tracking
packet_count = 0
packet_rate = 0.0
last_rate_check = time.time()


def on_packet_received(packet: dict):
    """Callback from BLE client when new data arrives."""
    global packet_count, packet_rate, last_rate_check

    packet_count += 1
    now = time.time()
    elapsed = now - last_rate_check
    if elapsed >= 1.0:
        packet_rate = round(packet_count / elapsed, 1)
        packet_count = 0
        last_rate_check = now

    # Store in DB
    db.insert_packet(packet)

    # Build broadcast payload
    payload = {
        "type": "live_data",
        "timestamp": datetime.utcnow().isoformat(),
        "packet_rate": packet_rate,
        "ble_connected": ble_client.is_connected,
        "signal_quality": _calc_signal_quality(packet),
        "data": packet
    }

    # Broadcast to all WebSocket clients
    asyncio.create_task(ws_manager.broadcast(json.dumps(payload)))


def _calc_signal_quality(packet: dict) -> str:
    try:
        ir = packet.get("raw", {}).get("ir", 0)
        if ir > 50000:
            return "Good"
        elif ir > 20000:
            return "Fair"
        else:
            return "Poor"
    except Exception:
        return "Unknown"


@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup
    logger.info("Initializing database...")
    db.init()
    logger.info("Starting BLE client...")
    ble_client.set_callback(on_packet_received)
    asyncio.create_task(ble_client.run())
    logger.info("Backend ready.")
    yield
    # Shutdown
    logger.info("Shutting down BLE client...")
    await ble_client.disconnect()
    logger.info("Shutdown complete.")


app = FastAPI(
    title="Edge AI Wearable Health Monitor",
    version="1.0.0",
    lifespan=lifespan
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# ─── REST ENDPOINTS ─────────────────────────────────────────────────────────

@app.get("/api/status")
async def get_status():
    """System and BLE connection status."""
    return {
        "ble_connected": ble_client.is_connected,
        "ble_device_name": ble_client.device_name,
        "ble_device_address": ble_client.device_address,
        "packet_rate": packet_rate,
        "uptime_seconds": int(time.time() - ble_client.start_time),
        "db_record_count": db.get_count(),
        "server_time": datetime.utcnow().isoformat()
    }


@app.get("/api/latest")
async def get_latest():
    """Get the most recent health packet."""
    row = db.get_latest()
    if not row:
        return {"data": None}
    return {"data": row}


@app.get("/api/history")
async def get_history(limit: int = 500, offset: int = 0):
    """Get historical records (paginated)."""
    rows = db.get_history(limit=limit, offset=offset)
    total = db.get_count()
    return {
        "total": total,
        "limit": limit,
        "offset": offset,
        "rows": rows
    }


@app.get("/api/history/ecg")
async def get_ecg_history(minutes: int = 5):
    """Get ECG trend data for the last N minutes."""
    rows = db.get_trend("ecg_raw", minutes=minutes)
    return {"minutes": minutes, "data": rows}


@app.get("/api/history/vitals")
async def get_vitals_history(minutes: int = 10):
    """Get vitals trend data for the last N minutes."""
    rows = db.get_vitals_trend(minutes=minutes)
    return {"minutes": minutes, "data": rows}


@app.post("/api/ble/scan")
async def scan_ble():
    """Trigger a BLE scan for nearby devices."""
    devices = await ble_client.scan()
    return {"devices": devices}


@app.post("/api/ble/connect")
async def connect_ble(address: Optional[str] = None):
    """Connect to BLE device (optionally by address)."""
    success = await ble_client.connect(address)
    return {"success": success, "connected": ble_client.is_connected}


@app.post("/api/ble/disconnect")
async def disconnect_ble():
    """Disconnect from BLE device."""
    await ble_client.disconnect()
    return {"success": True, "connected": False}


@app.delete("/api/history")
async def clear_history():
    """Clear all historical records."""
    db.clear()
    return {"success": True}


# ─── WEBSOCKET ───────────────────────────────────────────────────────────────

@app.websocket("/ws")
async def websocket_endpoint(websocket: WebSocket):
    await ws_manager.connect(websocket)
    logger.info(f"WebSocket client connected. Total: {ws_manager.count}")

    # Send current status on connect
    status_msg = json.dumps({
        "type": "status",
        "ble_connected": ble_client.is_connected,
        "packet_rate": packet_rate,
    })
    await websocket.send_text(status_msg)

    try:
        while True:
            # Keep connection alive, listen for pings
            data = await asyncio.wait_for(websocket.receive_text(), timeout=30.0)
            if data == "ping":
                await websocket.send_text(json.dumps({"type": "pong"}))
    except (WebSocketDisconnect, asyncio.TimeoutError):
        ws_manager.disconnect(websocket)
        logger.info(f"WebSocket client disconnected. Total: {ws_manager.count}")


if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=False)
