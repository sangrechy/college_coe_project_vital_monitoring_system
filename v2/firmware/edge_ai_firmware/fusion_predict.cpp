#include "fusion_predict.h"

int fusion_predict(
    int ecg_class,
    int stress_class
)
{
    if(ecg_class == 0 && stress_class == 0)
        return HEALTHY;

    if(ecg_class == 0 && stress_class == 1)
        return STRESSED;

    if(ecg_class != 0 && stress_class == 0)
        return CARDIAC_ALERT;

    return HIGH_RISK;
}
