#ifndef ECG_PREDICT_H
#define ECG_PREDICT_H

#ifdef __cplusplus
extern "C" {
#endif

#define ECG_NORMAL 0
#define ECG_PVC 1
#define ECG_PAC 2
#define ECG_LBBB 3
#define ECG_RBBB 4
#define ECG_OTHER_ABNORMAL 5

int ecg_predict(
    float rr_interval,
    float peak_amp,
    float beat_std,
    float beat_energy
);

#ifdef __cplusplus
}
#endif

#endif
