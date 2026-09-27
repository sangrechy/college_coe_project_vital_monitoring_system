"""
WebSocket connection manager.
Handles multiple simultaneous frontend clients.
"""

import logging
from typing import List

from fastapi import WebSocket

logger = logging.getLogger("ws_manager")


class ConnectionManager:
    def __init__(self):
        self._clients: List[WebSocket] = []

    @property
    def count(self) -> int:
        return len(self._clients)

    async def connect(self, websocket: WebSocket):
        await websocket.accept()
        self._clients.append(websocket)

    def disconnect(self, websocket: WebSocket):
        if websocket in self._clients:
            self._clients.remove(websocket)

    async def broadcast(self, message: str):
        """Send a message to all connected WebSocket clients."""
        dead = []
        for client in self._clients:
            try:
                await client.send_text(message)
            except Exception:
                dead.append(client)
        for client in dead:
            self.disconnect(client)
