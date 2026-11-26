import { useNavigate } from 'react-router-dom';
import {
  AreaChart, Area, YAxis, ResponsiveContainer
} from 'recharts';
import {
  getECGClass, getStressClass, getStatus, hrZone, spo2Zone, tempZone, round, badgeClass
} from '../../utils/labels';
import Icon from '../shared/Icon';

function StatTile({ label, value, unit, sub, subBadge, accentColor, icon, livePulse, onClick }) {
  return (
    <div
      className={`stat-tile${onClick ? ' clickable' : ''}`}
      style={{ '--tile-color': accentColor }}
      onClick={onClick}
    >
      <div className="stat-tile-top">
        <div className="stat-tile-label">
          {livePulse && <span className="live-pulse-dot" />}
          <span>{label}</span>
        </div>
        {icon && <span className="stat-tile-icon"><Icon name={icon} size={17} /></span>}
      </div>
      <div className="stat-tile-value">
        {value}
        {unit && <span className="stat-tile-unit">{unit}</span>}
      </div>
      {sub && (
        <div className="stat-tile-sub">
          {subBadge ? <span className={badgeClass(subBadge)}>{sub}</span> : sub}
        </div>
      )}
    </div>
  );
}

export default function Overview({ features, ai, raw, ecgRawBuffer, packetRate, bleConnected, signalQuality }) {
  const navigate = useNavigate();

  const ecgResult    = getECGClass(ai?.ecg_class);
  const stressResult = getStressClass(ai?.stress_class);
  const statusResult = getStatus(ai?.status);
  const hrInfo       = hrZone(features?.hr);
  const spo2Info     = spo2Zone(raw?.spo2);
  const tmpInfo      = tempZone(raw?.temp);

  // Spark line data
  const spark = ecgRawBuffer?.slice(-60).map((p, i) => ({ i, v: p.v })) || [];

  const st = ai?.status ?? -1;
  const tone = st < 0 ? 'neutral' : st >= 2 ? 'danger' : st === 1 ? 'warn' : 'ok';
  const toneIcon = st < 0 ? 'idle' : st >= 2 ? 'alert' : st === 1 ? 'eye' : 'check';

  return (
    <div>
      {/* Page header */}
      <div className="page-header">
        <div>
          <h2>System Overview</h2>
          <p>Live snapshot of AD8232, MAX30102, TMP117 &amp; MPU6500 telemetry with Edge AI</p>
        </div>
        <div className="header-right">
          <span className="header-stat">Rate <strong className="mono">{packetRate} Hz</strong></span>
          <span className={badgeClass(bleConnected ? 'ok' : 'danger')}>
            <span className="badge-dot" />
            {bleConnected ? 'Connected' : 'Disconnected'}
          </span>
        </div>
      </div>

      <div className="page-body">

        {/* Alert banner when disconnected */}
        {!bleConnected && (
          <div className="alert alert-warn">
            <Icon name="alert" size={18} />
            <span>BLE device not connected. Go to <strong>Device Status</strong> to connect.</span>
          </div>
        )}

        {/* Final Status Banner */}
        <div className={`status-hero tone-${tone}`}>
          <div className="status-hero-icon"><Icon name={toneIcon} size={26} stroke={2} /></div>
          <div className="status-hero-text">
            <div className="status-hero-eyebrow">Overall Health Status</div>
            <div className="status-hero-title">{statusResult.label}</div>
            <div className="status-hero-desc">{statusResult.desc}</div>
          </div>
        </div>

        {/* Vitals row */}
        <div className="section-title">Vital Signs</div>
        <div className="stat-grid stat-grid-5" style={{ marginBottom: 28 }}>
          <StatTile
            label="Heart Rate"
            value={round(features?.hr, 0)}
            unit="bpm"
            sub={hrInfo.label}
            subBadge={hrInfo.badge}
            accentColor="var(--chart-hr)"
            icon="heart"
            livePulse={bleConnected && features?.hr > 0}
            onClick={() => navigate('/vitals')}
          />
          <StatTile
            label="SpO₂"
            value={round(raw?.spo2 ?? 0, 1)}
            unit="%"
            sub={spo2Info.label}
            subBadge={spo2Info.badge}
            accentColor="var(--chart-spo2)"
            icon="drop"
            onClick={() => navigate('/vitals')}
          />
          <StatTile
            label="HRV"
            value={round(features?.hrv, 1)}
            unit="ms"
            sub="RR variability"
            accentColor="var(--chart-hrv)"
            icon="wave"
            onClick={() => navigate('/vitals')}
          />
          <StatTile
            label="Temp (TMP117)"
            value={round(raw?.temp ?? 0, 2)}
            unit="°C"
            sub={tmpInfo.label}
            subBadge={tmpInfo.badge}
            accentColor="var(--chart-temp)"
            icon="thermo"
            onClick={() => navigate('/vitals')}
          />
          <StatTile
            label="Motion (MPU6500)"
            value={round(features?.motion, 2)}
            unit="g"
            sub="Resultant G"
            accentColor="var(--chart-motion)"
            icon="motion"
            onClick={() => navigate('/vitals')}
          />
        </div>

        {/* AI + ECG mini row */}
        <div className="grid-2" style={{ marginBottom: 28 }}>
          {/* ECG sparkline */}
          <div className="card clickable" onClick={() => navigate('/ecg')}>
            <div className="card-header">
              <div>
                <div className="card-title">ECG Signal</div>
                <div className="card-subtitle">Raw ADC · click to open full view</div>
              </div>
              <span className={badgeClass(ecgResult.badge)}>{ecgResult.label}</span>
            </div>
            <div className="chart-wrapper ecg-well" style={{ height: 150 }}>
              <ResponsiveContainer width="100%" height={150}>
                <AreaChart data={spark} margin={{ top: 6, right: 0, bottom: 6, left: 0 }}>
                  <defs>
                    <linearGradient id="ovEcgFill" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="0%"   stopColor="var(--ecg-raw)" stopOpacity={0.28} />
                      <stop offset="100%" stopColor="var(--ecg-raw)" stopOpacity={0} />
                    </linearGradient>
                  </defs>
                  <YAxis hide domain={['auto', 'auto']} />
                  <Area
                    type="monotone"
                    dataKey="v"
                    stroke="var(--ecg-raw)"
                    strokeWidth={2}
                    fill="url(#ovEcgFill)"
                    dot={false}
                    isAnimationActive={false}
                  />
                </AreaChart>
              </ResponsiveContainer>
            </div>
            <div className="meta-row">
              <span>RR <strong className="mono">{round(features?.rr, 0)} ms</strong></span>
              <span>ECG Raw <strong className="mono">{raw?.ecg ?? '--'}</strong></span>
              <span>ECG Filt <strong className="mono">{features?.ecg_filt ?? '--'}</strong></span>
            </div>
          </div>

          {/* AI Results summary */}
          <div className="card clickable" onClick={() => navigate('/ai')}>
            <div className="card-header">
              <div>
                <div className="card-title">AI Classifications</div>
                <div className="card-subtitle">Edge inference results · click to open</div>
              </div>
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
              <div className="ai-result-card">
                <div className="ai-result-icon" style={{ background: 'var(--accent-light)', color: 'var(--accent-dark)' }}>
                  <Icon name="pulse" size={20} />
                </div>
                <div>
                  <div className="ai-result-label">ECG Classification</div>
                  <div className="ai-result-value">{ecgResult.label}</div>
                  <div className="ai-result-desc">{ecgResult.desc}</div>
                </div>
                <span className={badgeClass(ecgResult.badge)} style={{ marginLeft: 'auto', alignSelf: 'center' }}>{ecgResult.label}</span>
              </div>
              <div className="ai-result-card">
                <div className="ai-result-icon" style={{ background: '#ece8f5', color: '#6b5aa6' }}>
                  <Icon name="bolt" size={20} />
                </div>
                <div>
                  <div className="ai-result-label">Stress Level</div>
                  <div className="ai-result-value">{stressResult.label}</div>
                  <div className="ai-result-desc">{stressResult.desc}</div>
                </div>
                <span className={badgeClass(stressResult.badge)} style={{ marginLeft: 'auto', alignSelf: 'center' }}>{stressResult.label}</span>
              </div>
            </div>
          </div>
        </div>

        {/* Raw sensor values */}
        <div className="section-title">Raw Sensor Readings</div>
        <div className="raw-grid">
          {[
            ['ECG ADC', raw?.ecg, ''],
            ['IR',      raw?.ir,  ''],
            ['RED',     raw?.red, ''],
            ['SpO₂',    round(raw?.spo2, 1), '%'],
            ['Accel X', raw?.ax,  'LSB'],
            ['Accel Y', raw?.ay,  'LSB'],
            ['Accel Z', raw?.az,  'LSB'],
            ['TMP117',  round(raw?.temp, 2), '°C'],
          ].map(([label, val, unit]) => (
            <div key={label} className="raw-cell">
              <div className="raw-cell-label">{label}</div>
              <div className="raw-cell-value mono">
                {val ?? '--'}{unit ? <span className="raw-cell-unit">{unit}</span> : null}
              </div>
            </div>
          ))}
        </div>

      </div>
    </div>
  );
}
