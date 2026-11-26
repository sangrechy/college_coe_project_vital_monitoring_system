#include "ble_backend.h"

static BLECharacteristic notifyChr;
bool bleClientConnected = false;

void initBLE() {
    BTStatus initStatus = BLE.begin(BLE_DEVICE_NAME);
    if (!initStatus) {
        Serial.printf("[ FAIL ] BLE.begin failed (%s)\n", initStatus.toString());
        return;
    }

    BLE.setMTU(512);

    BLEServer server = BLE.createServer();
    if (!server) {
        Serial.println("[ FAIL ] BLE.createServer failed");
        return;
    }

    server.onConnect([](BLEServer s, const BLEConnInfo &conn) {
        bleClientConnected = true;
        Serial.printf("[ BLE  ] Client connected: %s (MTU %d)\n",
                      conn.getAddress().toString().c_str(), conn.getMTU());
    });

    server.onDisconnect([](BLEServer s, const BLEConnInfo &conn, uint8_t reason) {
        bleClientConnected = false;
        Serial.printf("[ BLE  ] Client disconnected (0x%02X) — restarting advertising\n", reason);
        BLE.startAdvertising();
    });

    BLEService svc = server.createService(BLEUUID(SERVICE_UUID));
    if (!svc) {
        Serial.println("[ FAIL ] BLE createService failed");
        return;
    }

    notifyChr = svc.createCharacteristic(
        BLEUUID(CHAR_UUID),
        BLEProperty::Read | BLEProperty::Notify,
        BLEPermission::ReadOpen
    );
    if (!notifyChr) {
        Serial.println("[ FAIL ] BLE createCharacteristic failed");
        return;
    }

    BTStatus status = server.start();
    if (!status) {
        Serial.printf("[ FAIL ] BLE server.start failed (%s)\n", status.toString());
        return;
    }

    BLEAdvertising adv = BLE.getAdvertising();
    adv.addServiceUUID(BLEUUID(SERVICE_UUID));
    status = adv.start();
    if (!status) {
        Serial.printf("[ FAIL ] BLE adv.start failed (%s)\n", status.toString());
        return;
    }

    Serial.printf("[  OK  ] BLE Advertising as %s | MAC: %s\n",
                  BLE.getDeviceName().c_str(),
                  BLE.getAddress().toString().c_str());
}

void sendBLEPacket(const String& packetData) {
    if (notifyChr && notifyChr.getSubscribedCount() > 0) {
        notifyChr.setValue((const uint8_t*)packetData.c_str(), packetData.length());
        notifyChr.notify();
    }
}
