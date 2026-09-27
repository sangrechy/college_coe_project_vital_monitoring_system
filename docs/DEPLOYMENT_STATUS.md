COE SEM 4 VITAL MONITOR SYSTEM
=============================

ML STATUS
---------

ECG MODEL
---------
Dataset: MIT-BIH
Classes: 6

0 = NORMAL
1 = PVC
2 = PAC
3 = LBBB
4 = RBBB
5 = OTHER_ABNORMAL

Accuracy: 85.66%

Original Model Size:
3.2 MB

Embedded Export Size:
679 KB


STRESS MODEL
------------
Dataset: WESAD
Classes: 4

0 = BASELINE
1 = STRESS
2 = AMUSEMENT
3 = MEDITATION

Accuracy: 91.32%

Original Model Size:
484 KB

Embedded Export Size:
117 KB


FUSION ENGINE
-------------
Status: Complete

Combines:
- ECG Classification
- Stress Classification
- Heart Rate
- SpO2
- Temperature
- Motion


EMBEDDED AI FOOTPRINT
---------------------

ECG Export:
679 KB

Stress Export:
117 KB

Total:
796 KB


TARGET HARDWARE
---------------

ESP32-S3 DevKitC-1
16 MB Flash
8 MB PSRAM


CURRENT STATUS
--------------

Training: Complete
Evaluation: Complete
Fusion: Complete
C Export: Complete

Remaining Work:
- Real-time feature extraction
- Embedded inference integration
- Hardware validation
