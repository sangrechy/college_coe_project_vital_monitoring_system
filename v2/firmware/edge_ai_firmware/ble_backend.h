#ifndef BLE_BACKEND_H
#define BLE_BACKEND_H

#include <Arduino.h>
#include <NimBLEDevice.h>

// Keeping your BLE Constants isolated and unchanged
const char* const BLE_DEVICE_NAME = "EdgeAI_Wearable";
const char* const SERVICE_UUID    = "12345678-1234-1234-1234-123456789001";
const char* const CHAR_UUID       = "12345678-1234-1234-1234-123456789002";

// Global pointers shared with the main file
extern NimBLECharacteristic *pCharacteristic;

// Function declarations
void initBLE();
void sendBLEPacket(const String& packetData);

#endif // BLE_BACKEND_H

