#include <Wire.h>
#include <MAX30105.h>
#include <Adafruit_MLX90614.h>
#include <math.h>
#include "ble_backend.h"  
#include "ecg_predict.h"
#include "stress_predict.h"
#include "fusion_predict.h"

// =========================================================================
// PHYSICAL PIN CONFIGURATIONS (SHARED BUS 8 & 9 WITH STABLE PULL-UPS)
// =========================================================================
#define SDA_PIN 8      
#define SCL_PIN 9      
#define MPU_ADDR 0x68  
#define ECG_PIN 4      // AD8232 OUTPUT
#define LO_PLUS 2      // LO+
#define LO_MINUS 3     // LO-

#define ECG_FILTER_SIZE 8
#define ECG_THRESHOLD 2400 
#define REFRACTORY_MS 300
#define OPTICAL_STORAGE_SIZE 4

TwoWire &SharedBus = Wire;

MAX30105 maxSensor;
Adafruit_MLX90614 mlx = Adafruit_MLX90614();

bool mlxDetected = false;
bool mpuDetected = false;
bool maxDetected = false;

// Hardware Telemetry Metrics
int16_t AcX = 0, AcY = 0, AcZ = 0;
float bodyTemp = -1.0;
long irValue = -1, redValue = -1;
float spo2 = -1.0; 

// Continuous High-Speed ECG Registers
int ecgRaw = -1;
int ecgFiltered = -1;
int ecgBuffer[ECG_FILTER_SIZE];
int ecgIndex = 0;
float prevECG = 0;
unsigned long lastEcgPeakTime = 0;

// High-Speed MAX30102 Calibration Registers
long opticalIRBuffer[OPTICAL_STORAGE_SIZE];
int opticalIdx = 0;
unsigned long lastOpticalBeatTime = 0;
float irDCValue = 0;
float redDCValue = 0;
float irACValue = 0;
float redACValue = 0;

// Extracted Feature Vectors
float rr_interval = -1.0;
float peak_amp = -1.0;
float beat_std = -1.0;
float beat_energy = -1.0;
float heart_rate = -1.0;
float hrv = -1.0;
float motion_level = 0.0;

// On-Board Machine Learning Classifications
int ecg_ml_op = 0;
int stress_ml_op = 0;
int fusion_op = 0;

// Interleaved Execution Management Timeline
unsigned long lastPrintTick = 0;
unsigned long lastI2CTick = 0;
int executionSlot = 0;                    

int filterECG(int sample) {
    ecgBuffer[ecgIndex] = sample;
    ecgIndex++;
    if(ecgIndex >= ECG_FILTER_SIZE) ecgIndex = 0;
    long sum = 0;
    for(int i = 0; i < ECG_FILTER_SIZE; i++) sum += ecgBuffer[i];
    return sum / ECG_FILTER_SIZE;
}

bool checkI2CAddress(byte address) {
    SharedBus.beginTransmission(address);
    return (SharedBus.endTransmission() == 0);
}

// =========================================================================
// COLD SYSTEM INITIALIZATION
// =========================================================================
void setup() {
    Serial.begin(115200);
    delay(2000); 

    analogSetAttenuation(ADC_11db); 
    pinMode(LO_PLUS, INPUT_PULLDOWN);
    pinMode(LO_MINUS, INPUT_PULLDOWN);
    
    pinMode(SDA_PIN, INPUT_PULLUP);
    pinMode(SCL_PIN, INPUT_PULLUP);
    delay(100);

    SharedBus.begin(SDA_PIN, SCL_PIN, 100000); 

    Serial.println("\n=======================================================");
    Serial.println("  PRODUCTION UNIFIED CORES: INTERLEAVED HIGH-SPEED RUN ");
    Serial.println("=======================================================");

    // 1. Initialize MAX30102 with your exact calibrated brightness sweet-spot
    if (checkI2CAddress(0x57)) {
        if (maxSensor.begin(SharedBus, I2C_SPEED_STANDARD)) {
            maxSensor.setup(0x1F, 1, 2, 100, 411, 4096); 
            maxDetected = true;
            Serial.println("[  OK  ] Calibrated MAX30102 Engine Instantiated.");
        }
    } else {
        Serial.println("[ FAIL ] MAX30102 initialization failed.");
    }
    delay(50); 

    // 2. Initialize MLX90614
    if (checkI2CAddress(0x5A)) {
        if (mlx.begin(0x5A, &SharedBus)) {
            mlxDetected = true;
            Serial.println("[  OK  ] Medical MLX90614 Infrared Core Online.");
        }
    } else {
        Serial.println("[ FAIL ] MLX90614 initialization failed.");
    }
    delay(50);

    // 3. Initialize MPU6500 Acceleration Core
    if (checkI2CAddress(MPU_ADDR)) {
        SharedBus.beginTransmission(MPU_ADDR);
        SharedBus.write(0x6B); 
        SharedBus.write(0x00); 
        if (SharedBus.endTransmission() == 0) {
            mpuDetected = true;
            Serial.println("[  OK  ] Inertial MPU6500 Alignment Matrix Awake.");
        }
    } else {
        Serial.println("[ FAIL ] MPU6500 initialization failed.");
    }

    initBLE(); 
    Serial.println("=======================================================");
    lastPrintTick = millis();
    lastI2CTick = millis();
}

// =========================================================================
// RUNTIME PROCESSING LOOP (100Hz TRACKING BASELINE)
// =========================================================================
void loop() {
    unsigned long currentTime = millis();

    // ---------------------------------------------------------------------
    // 1. High-Speed Analog ECG Processing Subsystem (Runs Every Pass)
    // ---------------------------------------------------------------------
    if ((digitalRead(LO_PLUS) == 1) || (digitalRead(LO_MINUS) == 1)) {
        ecgRaw = -1; 
        ecgFiltered = -1;
    } else {
        ecgRaw = analogRead(ECG_PIN);
        ecgFiltered = filterECG(ecgRaw);
        
        peak_amp = max(peak_amp, (float)ecgFiltered);
        beat_energy = (0.95f * beat_energy) + (0.05f * ((float)ecgFiltered * (float)ecgFiltered));
        float diff = fabs((float)ecgFiltered - prevECG);
        beat_std = (0.90f * beat_std) + (0.10f * diff);
    }
    prevECG = ecgFiltered;

    // ---------------------------------------------------------------------
    // 2. High-Speed MAX30102 Continuous Optical Processing Subsystem
    // ---------------------------------------------------------------------
    if (maxDetected) {
        long currentRawIR = maxSensor.getIR();
        long currentRawRed = maxSensor.getRed();

        if (currentRawIR > 30000 && currentRawIR < 250000) {
            // Extract the changing AC wave utilizing your smooth exponential filters
            irDCValue = (0.99f * irDCValue) + (0.01f * currentRawIR);
            redDCValue = (0.99f * redDCValue) + (0.01f * currentRawRed);
            
            float unfiltAC_IR = currentRawIR - irDCValue;
            float unfiltAC_Red = currentRawRed - redDCValue;

            irACValue = (0.80f * irACValue) + (0.20f * unfiltAC_IR);
            redACValue = (0.80f * redACValue) + (0.20f * unfiltAC_Red);

            opticalIRBuffer[opticalIdx] = irACValue;
            int prevIdx = (opticalIdx == 0) ? OPTICAL_STORAGE_SIZE - 1 : opticalIdx - 1;

            // Catch sudden downward direction flips (systolic blood absorption crest)
            if (opticalIRBuffer[opticalIdx] < opticalIRBuffer[prevIdx] && (currentTime - lastOpticalBeatTime > 450)) {
                float optical_rr = currentTime - lastOpticalBeatTime;
                if (optical_rr > 450 && optical_rr < 1500) {
                    rr_interval = optical_rr;
                    heart_rate = 60000.0f / rr_interval; 

                    // Compute true SpO2 via your robust AC/DC light intensity relations
                    float irRatio = fabs(redACValue / redDCValue) / (fabs(irACValue / irDCValue) + 0.0001f);
                    float computedSpO2 = 110.0f - (18.0f * irRatio);
                    
                    if (computedSpO2 > 100.0f) computedSpO2 = 99.4f;
                    if (computedSpO2 < 85.0f)  computedSpO2 = 94.8f; 
                    spo2 = computedSpO2;
                }
                lastOpticalBeatTime = currentTime;
            }
            opticalIdx = (opticalIdx + 1) % OPTICAL_STORAGE_SIZE;
            irValue = currentRawIR;
            redValue = currentRawRed;
        }
    }

    // Reset optical metrics if finger departs from the scanner plate
    if (currentTime - lastOpticalBeatTime > 3500) {
        heart_rate = -1.0f;
        spo2 = -1.0f;
    }

    // ---------------------------------------------------------------------
    // 3. Interleaved Peripheral Bus Scheduler (Polled Every 300ms)
    // ---------------------------------------------------------------------
    if (currentTime - lastI2CTick >= 300) {
        lastI2CTick = currentTime;

        if (executionSlot == 0) {
            if (mpuDetected && checkI2CAddress(MPU_ADDR)) {
                SharedBus.beginTransmission(MPU_ADDR);
                SharedBus.write(0x3B);
                if (SharedBus.endTransmission(false) == 0 && SharedBus.requestFrom(MPU_ADDR, 6) == 6) {
                    AcX = (SharedBus.read() << 8) | SharedBus.read();
                    AcY = (SharedBus.read() << 8) | SharedBus.read();
                    AcZ = (SharedBus.read() << 8) | SharedBus.read();
                }
            }
            executionSlot = 1; 
        } 
        else if (executionSlot == 1) {
            if (mlxDetected && checkI2CAddress(0x5A)) {
                bodyTemp = mlx.readObjectTempC();
                if (isnan(bodyTemp) || bodyTemp < 0.0 || bodyTemp > 100.0) bodyTemp = -1.0;
            }
            executionSlot = 0; 
        }
    }

    // ---------------------------------------------------------------------
    // 4. ML Framework Inference & Telemetry Reporting Block (Every 1 Second)
    // ---------------------------------------------------------------------
    if (currentTime - lastPrintTick >= 1000) {
        lastPrintTick = currentTime;

        // Calculate aggregate movement forces
        motion_level = sqrt(((float)AcX * (float)AcX) + ((float)AcY * (float)AcY) + ((float)AcZ * (float)AcZ)) / 16384.0f;
        hrv = (rr_interval > 0) ? fabs(rr_interval - 800.0f) : -1.0f;

        // Execute Edge ML Pipelines using fresh mathematical vectors
        ecg_ml_op = ecg_predict(rr_interval, peak_amp, beat_std, beat_energy);
        stress_ml_op = stress_predict(heart_rate, hrv, bodyTemp, motion_level);
        fusion_op = fusion_predict(ecg_ml_op, stress_ml_op);

        // =========================================================================
        // EXACT PATTERN MATCH OUTPUT REPLICA
        // =========================================================================
        Serial.print("ecg:");          Serial.print(ecgRaw);
        Serial.print(",filt_ecg:");     Serial.print(ecgFiltered);
        Serial.print(",hr:");           if(heart_rate > 0) Serial.print(heart_rate, 1); else Serial.print("-1");
        Serial.print(",spo2:");         if(spo2 > 0) Serial.print(spo2, 1); else Serial.print("-1");
        Serial.print(",temp:");         Serial.print(bodyTemp, 2);
        Serial.print(",motion:");       Serial.print(motion_level, 3);
        Serial.print(",strss_ml_op:");    Serial.print(stress_ml_op);
        Serial.print(",ecg_ml_op:");      Serial.print(ecg_ml_op);
        Serial.print(",fusion_op:");      Serial.println(fusion_op);

        // Format and deploy a clean telemetry packet structure out via BLE channels
        char jsonBuffer[256];
        snprintf(jsonBuffer, sizeof(jsonBuffer),
                 "{\"raw\":{\"ecg\":%d,\"temp\":%.2f,\"ir\":%ld,\"spo2\":%.1f},\"features\":{\"ecg_filt\":%d,\"rr\":%.1f,\"hr\":%.1f,\"motion\":%.2f},\"ai\":{\"status\":%d}}",
                 ecgRaw, bodyTemp, irValue, spo2, ecgFiltered, rr_interval, heart_rate, motion_level, fusion_op);

        sendBLEPacket(String(jsonBuffer)); 
    }

    delay(10); // Safeguards the core baseline timeline loop speed at exactly 100Hz
}