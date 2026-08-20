#include "ble_backend.h"

// Define the global characteristic pointer
NimBLECharacteristic *pCharacteristic = nullptr;

void initBLE() {
    NimBLEDevice::init(BLE_DEVICE_NAME);
    NimBLEDevice::setDeviceName(BLE_DEVICE_NAME);
    NimBLEDevice::setMTU(247);

    NimBLEServer *pServer = NimBLEDevice::createServer();
    NimBLEService *pService = pServer->createService(SERVICE_UUID);
    NimBLEAdvertising *pAdvertising = pServer->getAdvertising();

    pCharacteristic = pService->createCharacteristic(
        CHAR_UUID,
        NIMBLE_PROPERTY::READ | NIMBLE_PROPERTY::NOTIFY
    );

    pService->start();
    pAdvertising->addServiceUUID(SERVICE_UUID);
    pAdvertising->start();

    Serial.println("BLE BACKEND INITIALIZED");
}

void sendBLEPacket(const String& packetData) {
    if (pCharacteristic) {
        pCharacteristic->setValue((const uint8_t*)packetData.c_str(), packetData.length());
        pCharacteristic->notify();
    }
}
