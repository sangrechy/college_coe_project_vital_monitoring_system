#include "stress_predict.h"
#include "stress_forest_export.inc"


static int predict_tree(
    const int *feature,
    const float *threshold,
    const int *left,
    const int *right,
    const int *value,
    float x[]
)
{
    int node = 0;

    while(feature[node] != -2)
    {
        if(x[feature[node]] <= threshold[node])
            node = left[node];
        else
            node = right[node];
    }

    return value[node];
}

int stress_predict(
    float heart_rate,
    float hrv,
    float body_temperature,
    float motion_level
)
{
    float sample[4];

    sample[0] = heart_rate;
    sample[1] = hrv;
    sample[2] = body_temperature;
    sample[3] = motion_level;

    int votes[4] = {0};

    votes[predict_tree(stress_feature_0,stress_threshold_0,stress_left_0,stress_right_0,stress_value_0,sample)]++;
    votes[predict_tree(stress_feature_1,stress_threshold_1,stress_left_1,stress_right_1,stress_value_1,sample)]++;
    votes[predict_tree(stress_feature_2,stress_threshold_2,stress_left_2,stress_right_2,stress_value_2,sample)]++;
    votes[predict_tree(stress_feature_3,stress_threshold_3,stress_left_3,stress_right_3,stress_value_3,sample)]++;
    votes[predict_tree(stress_feature_4,stress_threshold_4,stress_left_4,stress_right_4,stress_value_4,sample)]++;
    votes[predict_tree(stress_feature_5,stress_threshold_5,stress_left_5,stress_right_5,stress_value_5,sample)]++;
    votes[predict_tree(stress_feature_6,stress_threshold_6,stress_left_6,stress_right_6,stress_value_6,sample)]++;
    votes[predict_tree(stress_feature_7,stress_threshold_7,stress_left_7,stress_right_7,stress_value_7,sample)]++;
    votes[predict_tree(stress_feature_8,stress_threshold_8,stress_left_8,stress_right_8,stress_value_8,sample)]++;
    votes[predict_tree(stress_feature_9,stress_threshold_9,stress_left_9,stress_right_9,stress_value_9,sample)]++;
    votes[predict_tree(stress_feature_10,stress_threshold_10,stress_left_10,stress_right_10,stress_value_10,sample)]++;
    votes[predict_tree(stress_feature_11,stress_threshold_11,stress_left_11,stress_right_11,stress_value_11,sample)]++;
    votes[predict_tree(stress_feature_12,stress_threshold_12,stress_left_12,stress_right_12,stress_value_12,sample)]++;
    votes[predict_tree(stress_feature_13,stress_threshold_13,stress_left_13,stress_right_13,stress_value_13,sample)]++;
    votes[predict_tree(stress_feature_14,stress_threshold_14,stress_left_14,stress_right_14,stress_value_14,sample)]++;
    votes[predict_tree(stress_feature_15,stress_threshold_15,stress_left_15,stress_right_15,stress_value_15,sample)]++;
    votes[predict_tree(stress_feature_16,stress_threshold_16,stress_left_16,stress_right_16,stress_value_16,sample)]++;
    votes[predict_tree(stress_feature_17,stress_threshold_17,stress_left_17,stress_right_17,stress_value_17,sample)]++;
    votes[predict_tree(stress_feature_18,stress_threshold_18,stress_left_18,stress_right_18,stress_value_18,sample)]++;
    votes[predict_tree(stress_feature_19,stress_threshold_19,stress_left_19,stress_right_19,stress_value_19,sample)]++;

    int best = 0;

    for(int i = 1; i < 4; i++)
    {
        if(votes[i] > votes[best])
            best = i;
    }

    return best;
}
