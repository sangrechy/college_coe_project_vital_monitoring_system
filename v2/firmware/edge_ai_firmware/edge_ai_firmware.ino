#include <Wire.h>
#include <math.h>
#include "MAX30105.h"
#include "heartRate.h"
#include "Adafruit_TMP117.h"
#include <Adafruit_Sensor.h>
#include <Adafruit_NeoPixel.h>
#include <BLE.h>

#include "ble_backend.h"
#include "ecg_predict.h"
#include "stress_predict.h"
#include "fusion_predict.h"

// =========================================================================
// NEW PROTOTYPE ESP32-S3 PIN CONFIGURATIONS
// =========================================================================
#define SDA_PIN    8      // Shared I2C SDA (MAX30102, TMP117, MPU6500)
#define SCL_PIN    9      // Shared I2C SCL
#define ECG_PIN    4      // AD8232 OUTPUT
#define LO_PLUS    5      // AD8232 LO+
#define LO_MINUS   6      // AD8232 LO-
#define MPU_ADDR   0x68   // MPU6500 I2C Address
#define TMP_ADDR   0x48   // TMP117 I2C Address
#define LED_PIN    48     // ESP32-S3 Onboard RGB NeoPixel
#define NUM_LEDS   1

// MPU6500 Register Map
#define WHO_AM_I_REG   0x75
#define PWR_MGMT_1     0x6B
#define ACCEL_CONFIG   0x1C
#define GYRO_CONFIG    0x1B

// Signal Processing Parameters
#define ECG_FILTER_SIZE    8
#define ECG_PEAK_THRESHOLD 2400
#define ECG_REFRACTORY_MS  300
#define RATE_SIZE          4

// =========================================================================
// HARDWARE INSTANCES
// =========================================================================
TwoWire &SharedBus = Wire;
Adafruit_NeoPixel onboardLED(NUM_LEDS, LED_PIN, NEO_GRB + NEO_KHZ800);
MAX30105 particleSensor;
Adafruit_TMP117 tmp117;

bool max30102Available = false;
bool tmp117Available   = false;
bool mpuAvailable      = false;

// Onboard NeoPixel state
uint16_t ledHue = 0;
unsigned long lastLEDUpdate = 0;

// =========================================================================
// HARDWARE TELEMETRY METRICS (REAL SENSOR READINGS ONLY)
// =========================================================================
int16_t AcX = 0, AcY = 0, AcZ = 0;
float ax = 0.0f, ay = 0.0f, az = 1.0f;
float gx = 0.0f, gy = 0.0f, gz = 0.0f;
float resultantG   = 0.0f;
float bodyTemp     = 0.0f;

long irValue       = 0;
long redValue      = 0;
float bpm          = 0.0f;
float averageBPM   = 0.0f;
float spo2         = 0.0f;

byte rates[RATE_SIZE] = {0};
byte rateSpot         = 0;
unsigned long lastOpticalBeatTime = 0;

// Continuous High-Speed ECG Registers
bool ecgLeadOff     = true;
int ecgRaw          = 0;
int ecgFiltered     = 0;
int ecgBuffer[ECG_FILTER_SIZE] = {0};
int ecgIndex        = 0;
float prevECG       = 0.0f;
unsigned long lastEcgPeakTime = 0;

// Extracted Feature Vectors (for Edge AI Models)
float rr_interval  = 0.0f;
float prev_rr      = 0.0f;
float peak_amp     = 0.0f;
float beat_std     = 0.0f;
float beat_energy  = 0.0f;
float heart_rate   = 0.0f;
float hrv          = 0.0f;
float motion_level = 0.0f;

// On-Board Machine Learning Classifications (-1 = No Valid Signal)
int ecg_ml_op    = -1;
int stress_ml_op = -1;
int fusion_op    = -1;

// Non-blocking Execution Timers (no blocking delay in loop!)
unsigned long lastECGRead   = 0;
unsigned long lastMPURead   = 0;
unsigned long lastTempRead  = 0;
unsigned long lastPrintTick = 0;

// =========================================================================
// UTILITY & FILTER FUNCTIONS
// =========================================================================
int filterECG(int sample) {
    ecgBuffer[ecgIndex] = sample;
    ecgIndex = (ecgIndex + 1) % ECG_FILTER_SIZE;
    long sum = 0;
    for (int i = 0; i < ECG_FILTER_SIZE; i++) {
        sum += ecgBuffer[i];
    }
    return (int)(sum / ECG_FILTER_SIZE);
}

void updateOnboardLED() {
    if (millis() - lastLEDUpdate >= 10) {
        lastLEDUpdate = millis();
        if (fusion_op >= 2) {
            // Cardiac Alert / High Risk: pulsing red
            uint8_t brightness = (uint8_t)(128 + 127 * sin(millis() * 0.015f));
            onboardLED.setPixelColor(0, onboardLED.Color(brightness, 0, 0));
        } else if (fusion_op == 1) {
            // Stressed / Monitor: steady amber
            onboardLED.setPixelColor(0, onboardLED.Color(180, 90, 0));
        } else if (fusion_op == 0) {
            // Healthy real signal: steady green
            onboardLED.setPixelColor(0, onboardLED.Color(0, 140, 40));
        } else {
            // Standby (waiting for finger/leads): smooth rainbow cycle
            onboardLED.setPixelColor(0, onboardLED.ColorHSV(ledHue, 255, 80));
            ledHue += 256;
        }
        onboardLED.show();
    }
}

// =========================================================================
// MPU6500 LOW-LEVEL I2C HELPERS
// =========================================================================
void writeMPU(byte reg, byte data) {
    SharedBus.beginTransmission(MPU_ADDR);
    SharedBus.write(reg);
    SharedBus.write(data);
    SharedBus.endTransmission();
}

byte readMPU(byte reg) {
    SharedBus.beginTransmission(MPU_ADDR);
    SharedBus.write(reg);
    if (SharedBus.endTransmission(false) != 0) return 0xFF;
    SharedBus.requestFrom(MPU_ADDR, (uint8_t)1);
    return SharedBus.available() ? SharedBus.read() : 0xFF;
}

int16_t readMPU16(byte reg) {
    SharedBus.beginTransmission(MPU_ADDR);
    SharedBus.write(reg);
    if (SharedBus.endTransmission(false) != 0) return 0;
    SharedBus.requestFrom(MPU_ADDR, (uint8_t)2);
    if (SharedBus.available() < 2) return 0;
    uint8_t highByte = SharedBus.read();
    uint8_t lowByte  = SharedBus.read();
    return (int16_t)((highByte << 8) | lowByte);
}

// =========================================================================
// SENSOR INITIALIZATION ROUTINES (NEW PROTOTYPE)
// =========================================================================
void initializeECG() {
    pinMode(LO_PLUS, INPUT);
    pinMode(LO_MINUS, INPUT);
    analogReadResolution(12);
    analogSetAttenuation(ADC_11db);
    Serial.println("[  OK  ] AD8232 ECG Initialized (GPIO 4, LO+ 5, LO- 6)");
}

void initializeMPU() {
    byte whoAmI = readMPU(WHO_AM_I_REG);
    if (whoAmI != 0x70) {
        Serial.printf("[ FAIL ] MPU6500 WHO_AM_I mismatch (got 0x%02X, expected 0x70)\n", whoAmI);
        mpuAvailable = false;
        return;
    }
    writeMPU(PWR_MGMT_1, 0x00);
    delay(100);
    writeMPU(ACCEL_CONFIG, 0x00);
    writeMPU(GYRO_CONFIG, 0x00);
    mpuAvailable = true;
    Serial.println("[  OK  ] MPU6500 Initialized Successfully (0x68)");
}

void initializeMAX30102() {
    if (!particleSensor.begin(SharedBus, I2C_SPEED_FAST)) {
        Serial.println("[ FAIL ] MAX30102 Not Found (0x57)");
        max30102Available = false;
        return;
    }
    // 400 Hz with sampleAverage=4 => clean 100 Hz hardware-averaged FIFO stream
    particleSensor.setup(60, 4, 2, 400, 411, 4096);
    particleSensor.setPulseAmplitudeRed(0x2A);
    particleSensor.setPulseAmplitudeIR(0x2A);
    particleSensor.setPulseAmplitudeGreen(0);
    max30102Available = true;
    Serial.println("[  OK  ] MAX30102 Initialized Successfully (0x57, 100Hz FIFO)");
}

void initializeTMP117() {
    if (!tmp117.begin(TMP_ADDR, &SharedBus)) {
        Serial.println("[ FAIL ] TMP117 Not Found (0x48)");
        tmp117Available = false;
        return;
    }
    tmp117Available = true;
    Serial.println("[  OK  ] TMP117 Precision Temperature Sensor Initialized (0x48)");
}

// =========================================================================
// SENSOR READING SUBSYSTEMS
// =========================================================================
void readECGData(unsigned long currentTime) {
    if (digitalRead(LO_PLUS) == 1 || digitalRead(LO_MINUS) == 1) {
        ecgLeadOff  = true;
        ecgRaw      = 0;
        ecgFiltered = 0;
        peak_amp    = 0.0f;
        beat_std    = 0.0f;
        beat_energy = 0.0f;
    } else {
        ecgLeadOff  = false;
        ecgRaw      = analogRead(ECG_PIN);
        ecgFiltered = filterECG(ecgRaw);

        if ((float)ecgFiltered > peak_amp) peak_amp = (float)ecgFiltered;
        beat_energy = (0.95f * beat_energy) + (0.05f * ((float)ecgFiltered * (float)ecgFiltered));
        float diff  = fabs((float)ecgFiltered - prevECG);
        beat_std    = (0.90f * beat_std) + (0.10f * diff);

        // Real R-peak detection for ECG RR interval
        if (ecgFiltered > ECG_PEAK_THRESHOLD && prevECG <= ECG_PEAK_THRESHOLD &&
            (currentTime - lastEcgPeakTime) > ECG_REFRACTORY_MS) {
            if (lastEcgPeakTime != 0) {
                unsigned long ecgDelta = currentTime - lastEcgPeakTime;
                if (ecgDelta > 300 && ecgDelta < 1800) {
                    if (rr_interval > 0.0f) {
                        prev_rr = rr_interval;
                        hrv = (0.7f * (hrv > 0.0f ? hrv : fabs((float)ecgDelta - prev_rr)))
                            + (0.3f * fabs((float)ecgDelta - prev_rr));
                    }
                    rr_interval = (float)ecgDelta;
                }
            }
            lastEcgPeakTime = currentTime;
        }
    }
    prevECG = (float)ecgFiltered;
}

void readMPUData() {
    if (!mpuAvailable) return;
    AcX = readMPU16(0x3B);
    AcY = readMPU16(0x3D);
    AcZ = readMPU16(0x3F);

    ax = AcX / 16384.0f;
    ay = AcY / 16384.0f;
    az = AcZ / 16384.0f;

    gx = readMPU16(0x43) / 131.0f;
    gy = readMPU16(0x45) / 131.0f;
    gz = readMPU16(0x47) / 131.0f;

    resultantG   = sqrtf(ax * ax + ay * ay + az * az);
    motion_level = resultantG;
}

void readMAX30102Data(unsigned long currentTime) {
    if (!max30102Available) return;

    // Non-blocking FIFO poll
    particleSensor.check();

    static float irDC = 0.0f;
    static float irACFilt = 0.0f;
    static float prevIrAC = 0.0f;
    static float irACEnv = 0.0f;
    static bool rising = false;

    while (particleSensor.available()) {
        irValue  = particleSensor.getFIFOIR();
        redValue = particleSensor.getFIFORed();
        particleSensor.nextSample();

        if (irValue <= 50000 || redValue <= 1000) {
            spo2       = 0.0f;
            bpm        = 0.0f;
            averageBPM = 0.0f;
            heart_rate = 0.0f;
            irDC       = 0.0f;
            irACFilt   = 0.0f;
            prevIrAC   = 0.0f;
            irACEnv    = 0.0f;
            lastOpticalBeatTime = 0;
            for (byte i = 0; i < RATE_SIZE; i++) rates[i] = 0;
            if (ecgLeadOff) {
                rr_interval = 0.0f;
                hrv         = 0.0f;
            }
            continue;
        }

        // Real-time SpO2 from Red/IR ratio
        float ratio    = (float)redValue / (float)irValue;
        float spo2Calc = 110.0f - 25.0f * ratio;
        spo2 = constrain(spo2Calc, 90.0f, 100.0f);

        // Adaptive DC-removal + AC envelope peak detector
        if (irDC == 0.0f) {
            irDC = (float)irValue;
        } else {
            irDC = 0.96f * irDC + 0.04f * (float)irValue;
        }
        float irAC = (float)irValue - irDC;
        irACFilt   = 0.70f * irACFilt + 0.30f * irAC;
        irACEnv    = max(irACEnv * 0.992f, fabs(irACFilt));

        bool adaptiveBeat = false;
        if (irACFilt > prevIrAC) {
            rising = true;
        } else if (rising && irACFilt < prevIrAC) {
            rising = false;
            // Peak detected above 35% of recent AC pulse envelope (minimum noise floor 25)
            float thresh = max(25.0f, irACEnv * 0.35f);
            if (prevIrAC > thresh && (currentTime - lastOpticalBeatTime) > 350) {
                adaptiveBeat = true;
            }
        }
        prevIrAC = irACFilt;

        bool sparkfunBeat = checkForBeat(irValue);

        if ((sparkfunBeat || adaptiveBeat) && (currentTime - lastOpticalBeatTime) > 350) {
            if (lastOpticalBeatTime != 0) {
                unsigned long delta = currentTime - lastOpticalBeatTime;
                float beatBPM = 60000.0f / (float)delta;
                if (beatBPM >= 45.0f && beatBPM <= 180.0f) {
                    if (ecgLeadOff) {
                        if (rr_interval > 0.0f) {
                            prev_rr = rr_interval;
                            hrv = (0.7f * (hrv > 0.0f ? hrv : fabs((float)delta - prev_rr)))
                                + (0.3f * fabs((float)delta - prev_rr));
                        }
                        rr_interval = (float)delta;
                    }
                    bpm = beatBPM;

                    rates[rateSpot++] = (byte)beatBPM;
                    rateSpot %= RATE_SIZE;
                    float total = 0.0f;
                    byte validCount = 0;
                    for (byte i = 0; i < RATE_SIZE; i++) {
                        if (rates[i] > 0) {
                            total += rates[i];
                            validCount++;
                        }
                    }
                    averageBPM = (validCount > 0) ? (total / validCount) : beatBPM;
                    heart_rate = averageBPM;
                }
            }
            lastOpticalBeatTime = currentTime;
        }
    }

    // Clear stale HR if no beat detected for > 6 seconds
    if (lastOpticalBeatTime != 0 && (currentTime - lastOpticalBeatTime > 6000)) {
        heart_rate = 0.0f;
    }
}

void readTMP117Data() {
    if (!tmp117Available) return;
    sensors_event_t temp;
    tmp117.getEvent(&temp);
    if (!isnan(temp.temperature) && temp.temperature > 0.0f && temp.temperature < 100.0f) {
        bodyTemp = temp.temperature;
    }
}

// =========================================================================
// SYSTEM INITIALIZATION
// =========================================================================
void setup() {
    Serial.begin(115200);
    delay(1500);

    onboardLED.begin();
    onboardLED.setBrightness(100);
    onboardLED.clear();
    onboardLED.show();

    SharedBus.begin(SDA_PIN, SCL_PIN, 100000);

    Serial.println("\n=======================================================");
    Serial.println("  EDGE AI WEARABLE V2 — NEW PROTOTYPE HARDWARE FIRMWARE");
    Serial.println("  Sensors: AD8232 | MAX30102 | TMP117 | MPU6500 | BLE  ");
    Serial.println("=======================================================");

    initializeECG();
    initializeMPU();
    initializeMAX30102();
    initializeTMP117();

    initBLE();
    Serial.println("=======================================================");

    unsigned long now = millis();
    lastECGRead   = now;
    lastMPURead   = now;
    lastTempRead  = now;
    lastPrintTick = now;
}

// =========================================================================
// RUNTIME PROCESSING LOOP (NON-BLOCKING FOR ACCURATE PPG BEAT DETECTION)
// =========================================================================
void loop() {
    unsigned long currentTime = millis();

    // 1. Onboard NeoPixel status animation
    updateOnboardLED();

    // 2. Full-speed MAX30102 sampling for accurate checkForBeat()
    readMAX30102Data(currentTime);

    // 3. AD8232 ECG sampling at 250 Hz (every 4 ms)
    if (currentTime - lastECGRead >= 4) {
        lastECGRead = currentTime;
        readECGData(currentTime);
    }

    // 4. MPU6500 IMU polling (10 Hz)
    if (currentTime - lastMPURead >= 100) {
        lastMPURead = currentTime;
        readMPUData();
    }

    // 5. TMP117 precision temperature polling (2 Hz)
    if (currentTime - lastTempRead >= 500) {
        lastTempRead = currentTime;
        readTMP117Data();
    }

    // 6. Edge AI Inference & BLE Telemetry Broadcast (1 Hz) — REAL SIGNALS ONLY
    if (currentTime - lastPrintTick >= 1000) {
        lastPrintTick = currentTime;

        bool hasValidECG    = (!ecgLeadOff && rr_interval > 0.0f && peak_amp > 0.0f);
        bool hasValidFinger = (irValue > 50000 && heart_rate > 0.0f);

        // Gate ML models so zero/unattached sensors NEVER produce fake classifications
        ecg_ml_op    = hasValidECG    ? ecg_predict(rr_interval, peak_amp, beat_std, beat_energy) : -1;
        stress_ml_op = hasValidFinger ? stress_predict(heart_rate, hrv, bodyTemp, motion_level)   : -1;

        if (ecg_ml_op >= 0 && stress_ml_op >= 0) {
            fusion_op = fusion_predict(ecg_ml_op, stress_ml_op);
        } else if (ecg_ml_op >= 0) {
            fusion_op = (ecg_ml_op == 0) ? HEALTHY : CARDIAC_ALERT;
        } else if (stress_ml_op >= 0) {
            fusion_op = (stress_ml_op == 1) ? STRESSED : HEALTHY;
        } else {
            fusion_op = -1; // Waiting for real physiological signal
        }

        // Reset beat peak tracker for next 1s window
        peak_amp = ecgLeadOff ? 0.0f : (float)ecgFiltered;

        // Serial Monitor Telemetry Output
        Serial.printf("ECG:%d | Filt:%d | BPM:%.1f | SpO2:%.1f%% | Temp:%.2fC | G:%.2fg | ECG_ML:%d | Stress_ML:%d | Fusion:%d\n",
                      ecgRaw, ecgFiltered, heart_rate, spo2, bodyTemp, motion_level,
                      ecg_ml_op, stress_ml_op, fusion_op);

        // Full JSON Telemetry Packet over BLE
        char jsonBuffer[384];
        snprintf(jsonBuffer, sizeof(jsonBuffer),
                 "{\"raw\":{\"ecg\":%d,\"temp\":%.2f,\"ir\":%ld,\"red\":%ld,\"spo2\":%.1f,\"ax\":%d,\"ay\":%d,\"az\":%d},"
                 "\"features\":{\"ecg_filt\":%d,\"rr\":%.1f,\"hr\":%.1f,\"hrv\":%.1f,\"motion\":%.2f},"
                 "\"ai\":{\"ecg_class\":%d,\"stress_class\":%d,\"status\":%d}}",
                 ecgRaw, bodyTemp, irValue, redValue, spo2, AcX, AcY, AcZ,
                 ecgFiltered, rr_interval, heart_rate, hrv, motion_level,
                 ecg_ml_op, stress_ml_op, fusion_op);

        sendBLEPacket(String(jsonBuffer));
    }
}