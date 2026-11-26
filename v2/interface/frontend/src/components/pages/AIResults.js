import { getECGClass, getStressClass, getStatus, badgeClass } from '../../utils/labels';
import Icon from '../shared/Icon';

function ClassCard({ icon, iconBg, label, result, code, codeLabel }) {
  const colorMap = {
    ok:      { bg: 'var(--ok-bg)',      border: '#c3e0cd', icon: '#2f7d4f' },
    warn:    { bg: 'var(--warn-bg)',    border: '#efd9a6', icon: '#a8610a' },
    danger:  { bg: 'var(--danger-bg)', border: '#efc3be', icon: '#b4352c' },
    neutral: { bg: '#efece6',          border: 'var(--border)', icon: 'var(--text-muted)' },
    info:    { bg: 'var(--accent-light)', border: '#b9dcd6', icon: 'var(--accent)' },
  };
  const colors = colorMap[result.badge] || colorMap.neutral;

  return (
    <div className="class-card" style={{ background: colors.bg, borderColor: colors.border }}>
      <div className="class-card-icon" style={{ color: colors.icon }}>
        {icon}
      </div>
      <div className="class-card-body">
        <div className="class-card-label" style={{ color: colors.icon }}>
          {label}
        </div>
        <div className="class-card-title">
          {result.label}
        </div>
        <p className="class-card-desc">
          {result.desc}
        </p>
      </div>
      <span className={badgeClass(result.badge)}>
        Code {code}
      </span>
    </div>
  );
}

function ConfidenceBar({ label, active }) {
  return (
    <div className={`legend-row${active ? ' active' : ''}`}>
      <span className="legend-row-dot" />
      {label}
    </div>
  );
}

export default function AIResults({ ai }) {
  const ecg_class    = ai?.ecg_class    ?? -1;
  const stress_class = ai?.stress_class ?? -1;
  const status       = ai?.status       ?? -1;

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
        <div className="alert alert-info" style={{ marginBottom: 24 }}>
          <Icon name="info" size={18} />
          <span>
            All inference runs entirely on the ESP32-S3. The laptop only displays the decoded results.
            Three AI models run on-device: <strong>ecg_predict</strong>, <strong>stress_predict</strong>, and <strong>fusion_predict</strong>.
          </span>
        </div>

        {/* Three result cards */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16, marginBottom: 24 }}>
          <ClassCard
            icon={<Icon name="pulse" size={22} />}
            iconBg="var(--accent-light)"
            label="ECG Classification · ecg_predict()"
            result={ecgResult}
            code={ecg_class}
          />
          <ClassCard
            icon={<Icon name="bolt" size={22} />}
            iconBg="#ece8f5"
            label="Stress Level · stress_predict()"
            result={stressResult}
            code={stress_class}
          />
          <ClassCard
            icon={<Icon name="chip" size={22} />}
            iconBg="#fbf1de"
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
            <div>
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
            <div>
              <div className="status-row"><span className="status-label">Heart Rate</span><span className="status-value mono">features.hr</span></div>
              <div className="status-row"><span className="status-label">HRV</span><span className="status-value mono">features.hrv</span></div>
              <div className="status-row"><span className="status-label">Body Temperature</span><span className="status-value mono">raw.temp (MLX90614)</span></div>
              <div className="status-row"><span className="status-label">Motion Level</span><span className="status-value mono">features.motion (MPU6050 RMS)</span></div>
            </div>
          </div>
        </div>

        {/* Class legend */}
        <div className="grid-2" style={{ marginTop: 20 }}>
          <div className="card">
            <div className="card-header"><div className="card-title">ECG Class Legend</div></div>
            <div className="legend-list">
              <ConfidenceBar label="0 → Normal — Regular cardiac rhythm" active={ecg_class === 0} />
              <ConfidenceBar label="1 → Arrhythmia — Irregular pattern detected" active={ecg_class === 1} />
              <ConfidenceBar label="2 → Unknown — Insufficient data" active={ecg_class === 2} />
            </div>
          </div>
          <div className="card">
            <div className="card-header"><div className="card-title">Stress Class Legend</div></div>
            <div className="legend-list">
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
