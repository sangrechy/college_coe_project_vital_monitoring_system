import { getECGClass, getStressClass, getStatus, badgeClass } from '../../utils/labels';

function ClassCard({ icon, iconBg, label, result, code, codeLabel }) {
  const colorMap = {
    ok:      { bg: 'var(--ok-bg)',      border: '#86efac', icon: '#16a34a' },
    warn:    { bg: 'var(--warn-bg)',    border: '#fcd34d', icon: '#d97706' },
    danger:  { bg: 'var(--danger-bg)', border: '#fca5a5', icon: '#dc2626' },
    neutral: { bg: '#f1f5f9',          border: 'var(--border)', icon: 'var(--text-muted)' },
    info:    { bg: 'var(--accent-light)', border: '#93c5fd', icon: 'var(--accent)' },
  };
  const colors = colorMap[result.badge] || colorMap.neutral;

  return (
    <div style={{
      background: colors.bg,
      border: `1px solid ${colors.border}`,
      borderRadius: 'var(--radius-lg)',
      padding: '24px',
      display: 'flex',
      flexDirection: 'column',
      gap: 12,
    }}>
      <div style={{ display: 'flex', alignItems: 'flex-start', gap: 14 }}>
        <div style={{
          width: 44, height: 44,
          background: iconBg,
          borderRadius: 'var(--radius)',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          fontSize: '1.2rem', flexShrink: 0,
        }}>
          {icon}
        </div>
        <div>
          <div style={{ fontSize: '0.72rem', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.07em', color: colors.icon, opacity: 0.8 }}>
            {label}
          </div>
          <div style={{ fontSize: '1.6rem', fontWeight: 700, color: 'var(--text-primary)', lineHeight: 1.2, marginTop: 2 }}>
            {result.label}
          </div>
        </div>
        <span className={badgeClass(result.badge)} style={{ marginLeft: 'auto' }}>
          Code {code}
        </span>
      </div>
      <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', lineHeight: 1.5, margin: 0 }}>
        {result.desc}
      </p>
    </div>
  );
}

function ConfidenceBar({ label, active }) {
  return (
    <div style={{
      display: 'flex', alignItems: 'center', gap: 10,
      padding: '8px 12px',
      background: active ? 'var(--accent-light)' : 'var(--bg-page)',
      border: `1px solid ${active ? '#93c5fd' : 'var(--border)'}`,
      borderRadius: 'var(--radius)',
      fontSize: '0.82rem',
      color: active ? 'var(--accent-dark)' : 'var(--text-muted)',
    }}>
      <span style={{
        width: 8, height: 8, borderRadius: '50%',
        background: active ? 'var(--accent)' : 'var(--border-strong)',
        flexShrink: 0,
      }} />
      {label}
    </div>
  );
}

export default function AIResults({ ai }) {
  const ecg_class    = ai?.ecg_class    ?? 2;
  const stress_class = ai?.stress_class ?? 0;
  const status       = ai?.status       ?? 0;

  const ecgResult    = getECGClass(ecg_class);
  const stressResult = getStressClass(stress_class);
  const statusResult = getStatus(status);

  return (
    <div>
      <div className="page-header">
        <div>
          <h2>AI Results</h2>
          <p>On-device edge inference output from ESP32-S3 AI models</p>
        </div>
        <div className="header-right">
          <span className={badgeClass(statusResult.badge)}>Status: {statusResult.label}</span>
        </div>
      </div>

      <div className="page-body">

        {/* Info banner */}
        <div className="alert alert-info" style={{ marginBottom: 20 }}>
          <span>ℹ</span>
          <span>
            All inference runs entirely on the ESP32-S3. The laptop only displays the decoded results.
            Three AI models run on-device: <strong>ecg_predict</strong>, <strong>stress_predict</strong>, and <strong>fusion_predict</strong>.
          </span>
        </div>

        {/* Three result cards */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 14, marginBottom: 20 }}>
          <ClassCard
            icon="〜"
            iconBg="var(--accent-light)"
            label="ECG Classification · ecg_predict()"
            result={ecgResult}
            code={ecg_class}
          />
          <ClassCard
            icon="◈"
            iconBg="#ede9fe"
            label="Stress Level · stress_predict()"
            result={stressResult}
            code={stress_class}
          />
          <ClassCard
            icon="⊕"
            iconBg="#fef3c7"
            label="Fusion Status · fusion_predict()"
            result={statusResult}
            code={status}
          />
        </div>

        {/* Model inputs reference */}
        <div className="grid-2">
          <div className="card">
            <div className="card-header">
              <div className="card-title">ECG Model Inputs</div>
              <div className="card-subtitle">ecg_predict(rr, peak_amp, beat_std, beat_energy)</div>
            </div>
            <div style={{ fontSize: '0.82rem', color: 'var(--text-secondary)', lineHeight: 1.8 }}>
              <div className="status-row"><span className="status-label">RR Interval</span><span className="status-value mono">features.rr</span></div>
              <div className="status-row"><span className="status-label">Peak Amplitude</span><span className="status-value mono">peak_amp (rolling max)</span></div>
              <div className="status-row"><span className="status-label">Beat Std Dev</span><span className="status-value mono">beat_std (EMA)</span></div>
              <div className="status-row"><span className="status-label">Beat Energy</span><span className="status-value mono">beat_energy (EMA)</span></div>
            </div>
          </div>
          <div className="card">
            <div className="card-header">
              <div className="card-title">Stress Model Inputs</div>
              <div className="card-subtitle">stress_predict(hr, hrv, temp, motion)</div>
            </div>
            <div style={{ fontSize: '0.82rem', color: 'var(--text-secondary)', lineHeight: 1.8 }}>
              <div className="status-row"><span className="status-label">Heart Rate</span><span className="status-value mono">features.hr</span></div>
              <div className="status-row"><span className="status-label">HRV</span><span className="status-value mono">features.hrv</span></div>
              <div className="status-row"><span className="status-label">Body Temperature</span><span className="status-value mono">raw.temp (MLX90614)</span></div>
              <div className="status-row"><span className="status-label">Motion Level</span><span className="status-value mono">features.motion (MPU6050 RMS)</span></div>
            </div>
          </div>
        </div>

        {/* Class legend */}
        <div className="grid-2" style={{ marginTop: 16 }}>
          <div className="card">
            <div className="card-header"><div className="card-title">ECG Class Legend</div></div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
              <ConfidenceBar label="0 → Normal — Regular cardiac rhythm" active={ecg_class === 0} />
              <ConfidenceBar label="1 → Arrhythmia — Irregular pattern detected" active={ecg_class === 1} />
              <ConfidenceBar label="2 → Unknown — Insufficient data" active={ecg_class === 2} />
            </div>
          </div>
          <div className="card">
            <div className="card-header"><div className="card-title">Stress Class Legend</div></div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
              <ConfidenceBar label="0 → Low — Physiological indicators normal" active={stress_class === 0} />
              <ConfidenceBar label="1 → Medium — Elevated stress markers" active={stress_class === 1} />
              <ConfidenceBar label="2 → High — High stress physiological state" active={stress_class === 2} />
            </div>
          </div>
        </div>

      </div>
    </div>
  );
}
