"""
BLE Client using Bleak library with concurrency locks.
Connects to ESP32-S3 EdgeAI Wearable device.
"""

import asyncio
import json
import logging
import time
from typing import Callable, Optional

from bleak import BleakClient, BleakScanner
from bleak.backends.device import BLEDevice

logger = logging.getLogger("ble_client")

# Constants
SERVICE_UUID        = "12345678-1234-1234-1234-123456789001"
CHARACTERISTIC_UUID = "12345678-1234-1234-1234-123456789002"
DEVICE_NAME         = "EdgeAI_Wearable"
DEVICE_ADDRESS      = "44:B1:76:CE:D9:ED"

RECONNECT_DELAY     = 5   
SCAN_TIMEOUT        = 10  


class BLEClient:
    def __init__(self):
        self._client: Optional[BleakClient] = None
        self._device: Optional[BLEDevice] = None
        self._callback: Optional[Callable] = None
        self._running = False
        self.is_connected = False
        self._is_connecting = False  # CONCURRENCY GUARD FLAG
        self.device_name = None
        self.device_address = None
        self.start_time = time.time()
        self._packets_received = 0

    def set_callback(self, callback: Callable):
        """Register a callback for received packets."""
        self._callback = callback

    async def scan(self, timeout: float = SCAN_TIMEOUT) -> list:
        """Scan for nearby BLE devices."""
        logger.info("Scanning for BLE devices...")
        devices = await BleakScanner.discover(timeout=timeout)
        result = []
        for d in devices:
            result.append({
                "name": d.name or "Unknown",
                "address": d.address,
                "rssi": getattr(d, "rssi", None)
            })
        logger.info(f"Found {len(result)} BLE devices.")
        return result

    async def _find_device(self) -> Optional[BLEDevice]:
        """Find the EdgeAI Wearable device using deep Service UUID filtering."""
        logger.info(f"Scanning for Service UUID '{SERVICE_UUID}'...")
        devices = await BleakScanner.discover(timeout=SCAN_TIMEOUT)
        
        for device in devices:
            try:
                uuids = device.metadata.get("uuids", [])
                if SERVICE_UUID.lower() in [u.lower() for u in uuids]:
                    return device
            except Exception:
                pass
        return None

    def _notification_handler(self, sender, data: bytearray):
        """Called by Bleak on every BLE notification."""
        try:
            raw = data.decode("utf-8").strip()
            packet = json.loads(raw)
            self._packets_received += 1
            if self._callback:
                self._callback(packet)
        except (json.JSONDecodeError, UnicodeDecodeError) as e:
            logger.warning(f"Failed to parse packet: {e}")

    def _disconnected_callback(self, client: BleakClient):
        """Called when BLE device disconnects unexpectedly."""
        logger.warning("BLE device disconnected!")
        self.is_connected = False
        self.device_name = None

    async def connect(self, address: Optional[str] = None) -> bool:
        """Connect to BLE device with concurrency interlocking."""
        # If an operation is already running, block this thread execution
        if self._is_connecting or self.is_connected:
            logger.warning("Connection action bypassed: Connection or handshake already active.")
            return False

        try:
            self._is_connecting = True  # RAISE CONCURRENCY SHIELD
            
            if address:
                logger.info(f"Bypassing discovery. Targeting static MAC: {address}")
                self._device = await BleakScanner.find_device_by_address(
                    address, timeout=SCAN_TIMEOUT
                )
            else:
                self._device = await self._find_device()

            if not self._device:
                logger.error("No valid hardware device target could be resolved.")
                return False

            self._client = BleakClient(
                self._device,
                disconnected_callback=self._disconnected_callback
            )
            await self._client.connect()
            self.is_connected = self._client.is_connected

            if self.is_connected:
                self.device_name = self._device.name or "EdgeAI_Wearable"
                self.device_address = self._device.address
                logger.info(f"Connected successfully to {self.device_name} [{self.device_address}]")

                await self._client.start_notify(
                    CHARACTERISTIC_UUID,
                    self._notification_handler
                )
                logger.info("GATT BLE notifications actively monitoring.")
                return True
            else:
                logger.error("Handshake established but connection state failed.")
                return False

        except Exception as e:
            logger.error(f"BLE connect error: {e}")
            self.is_connected = False
            return False
        finally:
            self._is_connecting = False  # LOWER CONCURRENCY SHIELD

    async def disconnect(self):
        """Disconnect cleanly from BLE device."""
        self._running = False
        if self._client and self._client.is_connected:
            try:
                await self._client.stop_notify(CHARACTERISTIC_UUID)
                await self._client.disconnect()
                logger.info("BLE disconnected cleanly.")
            except Exception as e:
                logger.warning(f"Error during disconnect lifecycle: {e}")
        self.is_connected = False
        self.device_name = None

    async def run(self):
        """Main run loop with auto-reconnect logic targeting hardware vectors."""
        self._running = True
        logger.info("BLE client run loop initiated.")

        while self._running:
            if not self.is_connected and not self._is_connecting:
                logger.info("Initiating connection procedure...")
                if DEVICE_ADDRESS:
                    await self.connect(DEVICE_ADDRESS)
                else:
                    await self.connect()

                if not self.is_connected:
                    await asyncio.sleep(RECONNECT_DELAY)
                    continue
            else:
                await asyncio.sleep(1.0)
                if self._client and not self._client.is_connected:
                    self.is_connected = False
