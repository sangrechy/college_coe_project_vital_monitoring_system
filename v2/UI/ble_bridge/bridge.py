import asyncio
import json
import logging
from typing import Optional

import uvicorn
from fastapi import FastAPI
from bleak import BleakClient, BleakScanner


logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("ble_bridge")


DEVICE_NAME = "EdgeAI_Wearable"

DEVICE_ADDRESS = "DC:B4:D9:07:1F:81"

SERVICE_UUID = "12345678-1234-1234-1234-123456789001"

CHARACTERISTIC_UUID = "12345678-1234-1234-1234-123456789002"


app = FastAPI(
    title="Windows BLE Bridge"
)


client: Optional[BleakClient] = None

connected_device = None

latest_packet = None


def notification_handler(
    sender,
    data: bytearray
):
    global latest_packet

    try:
        raw = data.decode(
            "utf-8"
        ).strip()

        latest_packet = json.loads(
            raw
        )

        logger.info(
            f"BLE packet received: "
            f"{latest_packet}"
        )

    except Exception as e:
        logger.warning(
            f"Packet parse error: {e}"
        )


def is_wearable_device(
    device,
    advertisement_data
):
    device_name = (
        device.name
        or getattr(
            advertisement_data,
            "local_name",
            None
        )
        or ""
    )

    if device_name == DEVICE_NAME:
        return True

    if (
        device.address
        and device.address.lower()
        == DEVICE_ADDRESS.lower()
    ):
        return True

    service_uuids = [
        uuid.lower()
        for uuid in getattr(
            advertisement_data,
            "service_uuids",
            []
        )
    ]

    if SERVICE_UUID.lower() in service_uuids:
        return True

    return False


async def find_wearable(
    timeout: float = 10
):
    logger.info(
        "Scanning for EdgeAI_Wearable..."
    )

    found_device = None


    def detection_callback(
        device,
        advertisement_data
    ):
        nonlocal found_device

        if found_device is not None:
            return

        if is_wearable_device(
            device,
            advertisement_data
        ):
            found_device = device

            logger.info(
                f"Wearable discovered: "
                f"{device.name or DEVICE_NAME} "
                f"({device.address})"
            )


    scanner = BleakScanner(
        detection_callback=detection_callback
    )

    await scanner.start()

    try:
        for _ in range(
            int(timeout * 10)
        ):
            if found_device is not None:
                break

            await asyncio.sleep(
                0.1
            )

    finally:
        await scanner.stop()

    return found_device


async def connect_to_device(
    device
):
    global client
    global connected_device
    global latest_packet

    try:
        logger.info(
            f"Connecting to "
            f"{device.name or DEVICE_NAME} "
            f"({device.address})"
        )

        client = BleakClient(
            device.address
        )

        await client.connect()

        if not client.is_connected:
            client = None

            return {
                "success": False,
                "connected": False,
                "error": "BLE connection failed"
            }

        connected_device = device

        latest_packet = None

        await client.start_notify(
            CHARACTERISTIC_UUID,
            notification_handler
        )

        logger.info(
            f"Connected successfully: "
            f"{device.name or DEVICE_NAME} "
            f"({device.address})"
        )

        return {
            "success": True,
            "connected": True,
            "device_name": (
                device.name
                or DEVICE_NAME
            ),
            "device_address": (
                device.address
            )
        }

    except Exception as e:
        logger.error(
            f"BLE connection error: {e}"
        )

        if client:
            try:
                await client.disconnect()

            except Exception:
                pass

        client = None

        connected_device = None

        return {
            "success": False,
            "connected": False,
            "error": str(e)
        }


async def auto_connect():
    global client

    if (
        client
        and client.is_connected
    ):
        return {
            "success": True,
            "connected": True
        }

    logger.info(
        "Starting automatic wearable connection..."
    )

    device = await find_wearable(
        timeout=10
    )

    if device is None:
        logger.warning(
            "Wearable not found"
        )

        return {
            "success": False,
            "connected": False,
            "error": (
                "EdgeAI_Wearable "
                "not found"
            )
        }

    return await connect_to_device(
        device
    )


@app.on_event("startup")
async def startup_event():
    logger.info(
        "BLE Bridge started"
    )

    asyncio.create_task(
        auto_connect()
    )


@app.get("/status")
async def status():
    is_connected = (
        client is not None
        and client.is_connected
    )

    return {
        "connected": is_connected,

        "device_name": (
            connected_device.name
            if (
                connected_device
                and connected_device.name
            )
            else (
                DEVICE_NAME
                if is_connected
                else None
            )
        ),

        "device_address": (
            connected_device.address
            if connected_device
            else None
        )
    }


@app.post("/scan")
async def scan():
    logger.info(
        "Scanning BLE devices..."
    )

    devices = await BleakScanner.discover(
        timeout=10
    )

    return {
        "devices": [
            {
                "name": (
                    device.name
                    or "Unknown"
                ),

                "address": (
                    device.address
                )
            }

            for device in devices
        ]
    }


@app.post("/connect")
async def connect(
    address: Optional[str] = None
):
    global client

    if (
        client
        and client.is_connected
    ):
        return {
            "success": True,
            "connected": True,

            "device_name": (
                connected_device.name
                if (
                    connected_device
                    and connected_device.name
                )
                else DEVICE_NAME
            ),

            "device_address": (
                connected_device.address
                if connected_device
                else None
            )
        }

    if address:
        logger.info(
            f"Manual BLE connection "
            f"requested: {address}"
        )

        devices = await BleakScanner.discover(
            timeout=10
        )

        device = next(
            (
                d
                for d in devices
                if (
                    d.address.lower()
                    == address.lower()
                )
            ),
            None
        )

        if not device:
            return {
                "success": False,
                "connected": False,
                "error": "Device not found"
            }

        return await connect_to_device(
            device
        )

    return await auto_connect()


@app.post("/disconnect")
async def disconnect():
    global client
    global connected_device
    global latest_packet

    if (
        client
        and client.is_connected
    ):
        try:
            await client.stop_notify(
                CHARACTERISTIC_UUID
            )

        except Exception:
            pass

        try:
            await client.disconnect()

        except Exception as e:
            logger.warning(
                f"Disconnect error: {e}"
            )

    client = None

    connected_device = None

    latest_packet = None

    logger.info(
        "BLE disconnected"
    )

    return {
        "success": True
    }


@app.get("/latest")
async def latest():
    return {
        "data": latest_packet
    }


if __name__ == "__main__":

    logger.info(
        "Starting Windows BLE Bridge..."
    )

    uvicorn.run(
        app,
        host="127.0.0.1",
        port=8001
    )