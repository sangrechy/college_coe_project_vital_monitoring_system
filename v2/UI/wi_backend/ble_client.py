import asyncio
import logging
import time
from typing import Callable, Optional

import httpx

logger = logging.getLogger("ble_client")

BRIDGE_URL = "http://host.docker.internal:8001"

RECONNECT_DELAY = 5
POLL_INTERVAL = 0.1

TARGET_DEVICE_NAME = "EdgeAI_Wearable"


class BLEClient:
    def __init__(self):
        self._callback: Optional[Callable] = None
        self._running = False
        self._is_connecting = False

        self.is_connected = False
        self.device_name = None
        self.device_address = None

        self.start_time = time.time()
        self._last_packet = None

    def set_callback(self, callback: Callable):
        self._callback = callback

    async def scan(self, timeout: float = 10) -> list:
        logger.info(
            "Requesting BLE scan from Windows BLE Bridge..."
        )

        try:
            async with httpx.AsyncClient(
                timeout=timeout + 5
            ) as client:
                response = await client.post(
                    f"{BRIDGE_URL}/scan"
                )

                response.raise_for_status()

                data = response.json()
                devices = data.get("devices", [])

                logger.info(
                    f"Windows BLE Bridge found "
                    f"{len(devices)} devices."
                )

                return devices

        except Exception as e:
            logger.error(
                f"BLE bridge scan error: {e}"
            )

            return []

    async def find_device(self) -> Optional[dict]:
        devices = await self.scan()

        for device in devices:
            name = device.get("name", "")
            address = device.get("address")

            if name == TARGET_DEVICE_NAME and address:
                logger.info(
                    f"Found target device: "
                    f"{name} ({address})"
                )

                return device

        logger.warning(
            f"{TARGET_DEVICE_NAME} was not found."
        )

        return None

    async def connect(
        self,
        address: Optional[str] = None
    ) -> bool:
        if self._is_connecting:
            logger.warning(
                "BLE connection already in progress."
            )

            return False

        try:
            self._is_connecting = True

            if not address:
                logger.info(
                    f"No MAC address provided. "
                    f"Searching for {TARGET_DEVICE_NAME}..."
                )

                device = await self.find_device()

                if not device:
                    logger.error(
                        f"Automatic device discovery failed."
                    )

                    return False

                address = device.get("address")

            logger.info(
                f"Requesting Windows BLE Bridge "
                f"connection: {address}"
            )

            async with httpx.AsyncClient(
                timeout=20
            ) as client:
                response = await client.post(
                    f"{BRIDGE_URL}/connect",
                    params={"address": address}
                )

                response.raise_for_status()

                data = response.json()

                if data.get("success"):
                    self.is_connected = data.get(
                        "connected",
                        True
                    )

                    self.device_name = data.get(
                        "device_name",
                        TARGET_DEVICE_NAME
                    )

                    self.device_address = data.get(
                        "device_address",
                        address
                    )

                    logger.info(
                        f"Connected through "
                        f"Windows BLE Bridge: "
                        f"{self.device_name} "
                        f"({self.device_address})"
                    )

                    return self.is_connected

                self.is_connected = False

                logger.error(
                    f"BLE connection failed: "
                    f"{data.get('error', 'Unknown error')}"
                )

                return False

        except Exception as e:
            logger.error(
                f"BLE bridge connect error: {e}"
            )

            self.is_connected = False
            self.device_name = None
            self.device_address = None

            return False

        finally:
            self._is_connecting = False

    async def disconnect(self):
        self._running = False

        try:
            async with httpx.AsyncClient(
                timeout=10
            ) as client:
                await client.post(
                    f"{BRIDGE_URL}/disconnect"
                )

        except Exception as e:
            logger.warning(
                f"BLE bridge disconnect error: {e}"
            )

        self.is_connected = False
        self.device_name = None
        self.device_address = None

    async def _get_status(self):
        try:
            async with httpx.AsyncClient(
                timeout=5
            ) as client:
                response = await client.get(
                    f"{BRIDGE_URL}/status"
                )

                response.raise_for_status()

                data = response.json()

                self.is_connected = data.get(
                    "connected",
                    False
                )

                self.device_name = data.get(
                    "device_name"
                )

                self.device_address = data.get(
                    "device_address"
                )

        except Exception as e:
            logger.debug(
                f"BLE bridge status error: {e}"
            )

            self.is_connected = False

    async def _poll_packet(self):
        try:
            async with httpx.AsyncClient(
                timeout=5
            ) as client:
                response = await client.get(
                    f"{BRIDGE_URL}/latest"
                )

                response.raise_for_status()

                data = response.json()

                packet = data.get("data")

                if (
                    packet
                    and packet != self._last_packet
                ):
                    self._last_packet = packet

                    if self._callback:
                        self._callback(packet)

        except Exception as e:
            logger.debug(
                f"BLE packet polling error: {e}"
            )

    async def run(self):
        self._running = True

        logger.info(
            "BLE bridge monitoring loop started."
        )

        while self._running:
            await self._get_status()

            if self.is_connected:
                await self._poll_packet()

            await asyncio.sleep(POLL_INTERVAL)