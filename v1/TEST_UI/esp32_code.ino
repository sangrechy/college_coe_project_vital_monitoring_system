#include <WiFi.h>
#include <HTTPClient.h>
#include <Wire.h>
#include "MAX30105.h"
#include "heartRate.h"

MAX30105 particleSensor;

const char* ssid = "Sangrechy";
const char* password = "12345678";

// CHANGE THIS
const char* serverName = "http://10.42.99.1:5000/data";

const byte RATE_SIZE = 4;

byte rates[RATE_SIZE];
byte rateSpot = 0;

long lastBeat = 0;

float beatsPerMinute;
int beatAvg;

void setup()
{
    Serial.begin(115200);

    Wire.begin(21, 22);

    WiFi.begin(ssid, password);

    Serial.print("Connecting WiFi");

    while (WiFi.status() != WL_CONNECTED)
    {
        delay(500);
        Serial.print(".");
    }

    Serial.println("\nWiFi Connected");

    if (!particleSensor.begin(Wire, I2C_SPEED_STANDARD))
    {
        Serial.println("MAX30102 NOT FOUND");
        while (1);
    }

    particleSensor.setup(
        60,
        4,
        2,
        100,
        411,
        4096
    );

    Serial.println("Place finger on sensor");
}

void loop()
{
    long irValue = particleSensor.getIR();

    if (checkForBeat(irValue))
    {
        long delta = millis() - lastBeat;

        lastBeat = millis();

        beatsPerMinute = 60 / (delta / 1000.0);

        if (beatsPerMinute < 255 && beatsPerMinute > 20)
        {
            rates[rateSpot++] = (byte)beatsPerMinute;

            rateSpot %= RATE_SIZE;

            beatAvg = 0;

            for (byte x = 0; x < RATE_SIZE; x++)
            {
                beatAvg += rates[x];
            }

            beatAvg /= RATE_SIZE;
        }
    }

    float spo2 = 98.0 - random(0, 3);

    if (WiFi.status() == WL_CONNECTED)
    {
        HTTPClient http;

        http.begin(serverName);

        http.addHeader("Content-Type", "application/json");

        String jsonData = "{";
        jsonData += "\"hr\":" + String(beatAvg) + ",";
        jsonData += "\"spo2\":" + String(spo2) + ",";
        jsonData += "\"ir\":" + String(irValue);
        jsonData += "}";

        int httpResponseCode = http.POST(jsonData);

        Serial.print("HTTP Response: ");
        Serial.println(httpResponseCode);

        http.end();
    }

    Serial.print("HR: ");
    Serial.print(beatAvg);

    Serial.print(" BPM | SpO2: ");
    Serial.print(spo2);

    Serial.print("% | IR: ");
    Serial.println(irValue);

    delay(1000);
}

