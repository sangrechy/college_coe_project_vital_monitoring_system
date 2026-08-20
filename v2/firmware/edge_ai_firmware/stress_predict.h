#ifndef STRESS_PREDICT_H
#define STRESS_PREDICT_H

#ifdef __cplusplus
extern "C" {
#endif

#define STRESS_BASELINE 0
#define STRESS_STRESS 1
#define STRESS_AMUSEMENT 2
#define STRESS_MEDITATION 3

int stress_predict(
    float heart_rate,
    float hrv,
    float body_temperature,
    float motion_level
);

#ifdef __cplusplus
}
#endif

#endif
