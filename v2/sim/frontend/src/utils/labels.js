/**
 * Utility helpers for label mapping and value classification.
 * These mirror the ESP32 firmware AI output codes exactly.
 */

// ECG Class (ecg_class): 0=Normal, 1=Arrhythmia, 2=Unknown
export const ECG_CLASS_MAP = {
  0: { label: 'Normal',      badge: 'ok',      desc: 'Regular cardiac rhythm detected' },
  1: { label: 'Arrhythmia',  badge: 'danger',  desc: 'Irregular rhythm pattern detected' },
  2: { label: 'Unknown',     badge: 'neutral', desc: 'Insufficient data for classification' },
};

// Stress Class (stress_class): 0=Low, 1=Medium, 2=High
export const STRESS_CLASS_MAP = {
  0: { label: 'Low',    badge: 'ok',    desc: 'Physiological indicators normal' },
  1: { label: 'Medium', badge: 'warn',  desc: 'Elevated stress markers observed' },
  2: { label: 'High',   badge: 'danger',desc: 'High stress physiological state' },
};

// Final Status (status): 0=Normal, 1=Monitor, 2=Alert
export const STATUS_MAP = {
  0: { label: 'Normal',  badge: 'ok',    desc: 'All indicators within normal range' },
  1: { label: 'Monitor', badge: 'warn',  desc: 'One or more indicators require monitoring' },
  2: { label: 'Alert',   badge: 'danger',desc: 'Immediate attention may be required' },
};

export function getECGClass(code)    { return ECG_CLASS_MAP[code]    || ECG_CLASS_MAP[2]; }
export function getStressClass(code) { return STRESS_CLASS_MAP[code] || STRESS_CLASS_MAP[0]; }
export function getStatus(code)      { return STATUS_MAP[code]        || STATUS_MAP[0]; }

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

// Temperature zone
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
