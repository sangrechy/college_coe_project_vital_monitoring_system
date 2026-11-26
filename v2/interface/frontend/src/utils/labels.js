/**
 * Utility helpers for label mapping and value classification.
 * These mirror the ESP32 firmware AI output codes exactly.
 */

// ECG Class (ecg_class): MIT-BIH 6-class embedded Random Forest outputs (-1 = No Signal)
export const ECG_CLASS_MAP = {
  [-1]: { label: 'No Signal',      badge: 'neutral', desc: 'Attach AD8232 ECG leads to classify rhythm' },
  0:    { label: 'Normal',         badge: 'ok',      desc: 'Regular sinus cardiac rhythm detected' },
  1:    { label: 'PVC',            badge: 'danger',  desc: 'Premature Ventricular Contraction detected' },
  2:    { label: 'PAC',            badge: 'warn',    desc: 'Premature Atrial Contraction detected' },
  3:    { label: 'LBBB',           badge: 'danger',  desc: 'Left Bundle Branch Block pattern detected' },
  4:    { label: 'RBBB',           badge: 'danger',  desc: 'Right Bundle Branch Block pattern detected' },
  5:    { label: 'Other Abnormal', badge: 'danger',  desc: 'Irregular rhythm anomaly detected' },
};

// Stress Class (stress_class): WESAD 4-class embedded Random Forest outputs (-1 = No Signal)
export const STRESS_CLASS_MAP = {
  [-1]: { label: 'No Signal',  badge: 'neutral', desc: 'Place finger on MAX30102 for stress analysis' },
  0:    { label: 'Baseline',   badge: 'ok',      desc: 'Physiological indicators calm & normal' },
  1:    { label: 'Stress',     badge: 'danger',  desc: 'Elevated physiological stress detected' },
  2:    { label: 'Amusement',  badge: 'info',    desc: 'Positive arousal / elevated engagement' },
  3:    { label: 'Meditation', badge: 'ok',      desc: 'Deep relaxation & steady autonomic tone' },
};

// Final Status (status): matches fusion_predict.h (-1=Standby, 0=Healthy, 1=Stressed, 2=Cardiac Alert, 3=High Risk)
export const STATUS_MAP = {
  [-1]: { label: 'Waiting for Signal', badge: 'neutral', desc: 'Attach ECG leads or place finger on MAX30102' },
  0:    { label: 'Healthy',            badge: 'ok',      desc: 'All active physiological indicators within normal range' },
  1:    { label: 'Stressed / Monitor', badge: 'warn',    desc: 'Elevated physiological stress detected' },
  2:    { label: 'Cardiac Alert',      badge: 'danger',  desc: 'Irregular cardiac rhythm detected by Edge AI' },
  3:    { label: 'High Risk',          badge: 'danger',  desc: 'Combined cardiac anomaly and acute stress detected' },
};

export function getECGClass(code)    { return ECG_CLASS_MAP[code]    ?? ECG_CLASS_MAP[-1]; }
export function getStressClass(code) { return STRESS_CLASS_MAP[code] ?? STRESS_CLASS_MAP[-1]; }
export function getStatus(code)      { return STATUS_MAP[code]       ?? STATUS_MAP[-1]; }

export function badgeClass(badge) {
  return `badge badge-${badge}`;
}

// Heart rate zones
export function hrZone(hr) {
  if (!hr || hr < 1)  return { label: 'No Signal', badge: 'neutral' };
  if (hr < 60)        return { label: 'Low',        badge: 'warn'    };
  if (hr <= 100)      return { label: 'Normal',     badge: 'ok'      };
  if (hr <= 140)      return { label: 'Elevated',   badge: 'warn'    };
  return              { label: 'High',              badge: 'danger'  };
}

// SpO2 oxygen saturation zones
export function spo2Zone(spo2) {
  if (!spo2 || spo2 < 1) return { label: 'No Signal', badge: 'neutral' };
  if (spo2 >= 95)        return { label: 'Normal',    badge: 'ok'      };
  if (spo2 >= 90)        return { label: 'Low',       badge: 'warn'    };
  return                 { label: 'Critical',         badge: 'danger'  };
}

// Temperature zone (TMP117 precision sensor)
export function tempZone(t) {
  if (!t || t < 1)    return { label: 'No Signal', badge: 'neutral' };
  if (t < 35.0)       return { label: 'Low',       badge: 'warn'    };
  if (t <= 37.5)      return { label: 'Normal',    badge: 'ok'      };
  if (t <= 38.5)      return { label: 'Elevated',  badge: 'warn'    };
  return              { label: 'Fever',            badge: 'danger'  };
}

// Format uptime seconds -> "2h 15m 30s"
export function formatUptime(secs) {
  if (!secs) return '0s';
  const h = Math.floor(secs / 3600);
  const m = Math.floor((secs % 3600) / 60);
  const s = secs % 60;
  if (h > 0) return `${h}h ${m}m ${s}s`;
  if (m > 0) return `${m}m ${s}s`;
  return `${s}s`;
}

// Format ISO timestamp -> "14:32:05"
export function fmtTime(iso) {
  if (!iso) return '--';
  try {
    return new Date(iso).toLocaleTimeString();
  } catch { return '--'; }
}

export function round(v, digits = 1) {
  if (v == null || isNaN(v)) return '--';
  return Number(v).toFixed(digits);
}
