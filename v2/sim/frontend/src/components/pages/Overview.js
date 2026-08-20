import { useNavigate } from 'react-router-dom';
import {
  LineChart, Line, ResponsiveContainer, XAxis, YAxis, Tooltip
} from 'recharts';
import {
  getECGClass, getStressClass, getStatus, hrZone, tempZone, round, badgeClass
} from '../../utils/labels';

function StatTile({ label, value, unit, sub, accentColor, onClick }) {
  return (
    <div className="stat-tile" style={{ cursor: onClick ? 'pointer' : 'default' }} onClick={onClick}>
      <div className="stat-tile-label">{label}</div>
      <div className="stat-tile-value">
        {value}
        {unit && <span className="stat-tile-unit">{unit}</span>}
      </div>
      {sub && <div className="stat-tile-sub">{sub}</div>}
      {accentColor && (
        <div className="stat-tile-accent" style={{ background: accentColor }} />
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
  const tmpInfo      = tempZone(features ? features : raw ? raw.temp : null);

  // Spark line data
  const spark = ecgRawBuffer?.slice(-60).map((p, i) => ({ i, v: p.v })) || [];

  return (
    <div>
      {/* Page header */}
      <div className="page-header">
        <div>
          <h2>System Overview</h2>
          <p>Live snapshot of all sensor channels and AI classifications</p>
        </div>
        <div className="header-right">
          <span>Packet rate: <strong className="mono">{packetRate} Hz</strong></span>
          <span className={badgeClass(bleConnected ? 'ok' : 'danger')}>
            {bleConnected ? '● Connected' : '○ Disconnected'}
          </span>
        </div>
      </div>

      <div className="page-body">

        {/* Alert banner when disconnected */}
        {!bleConnected && (
          <div className="alert alert-warn">
            <span>⚠</span>
            <span>BLE device not connected. Go to <strong>Device Status</strong> to connect.</span>
          </div>
        )}

        {/* Final Status Banner */}
        <div style={{
          padding: '14px 20px',
          borderRadius: 'var(--radius-lg)',
          marginBottom: 20,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          border: '1px solid',
          background: ai?.status === 2 ? 'var(--danger-bg)'
            : ai?.status === 1 ? 'var(--warn-bg)'
            : 'var(--ok-bg)',
          borderColor: ai?.status === 2 ? '#fca5a5'
            : ai?.status === 1 ? '#fcd34d'
            : '#86efac',
        }}>
          <div>
            <div style={{ fontSize: '0.75rem', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.06em', opacity: 0.7 }}>
              Overall Health Status
            </div>
            <div style={{ fontSize: '1.3rem', fontWeight: 700, marginTop: 2 }}>
              {statusResult.label}
            </div>
            <div style={{ fontSize: '0.8rem', marginTop: 2 }}>{statusResult.desc}</div>
          </div>
          <span style={{ fontSize: '2rem' }}>
            {ai?.status === 2 ? '⚠' : ai?.status === 1 ? '◎' : '✓'}
          </span>
        </div>

        {/* Vitals row */}
        <div className="section-title">Vital Signs</div>
        <div className="stat-grid stat-grid-4" style={{ marginBottom: 20 }}>
          <StatTile
            label="Heart Rate"
            value={round(features?.hr, 0)}
            unit="bpm"
            sub={hrInfo.label}
            accentColor="var(--chart-hr)"
            onClick={() => navigate('/vitals')}
          />
          <StatTile
            label="HRV"
            value={round(features?.hrv, 1)}
            unit="ms"
            sub="RR variability"
            accentColor="var(--chart-hrv)"
            onClick={() => navigate('/vitals')}
          />
          <StatTile
            label="Temperature"
            value={round(raw?.temp ?? 0, 1)}
            unit="°C"
            sub={tempZone(raw?.temp).label}
            accentColor="var(--chart-temp)"
            onClick={() => navigate('/vitals')}
          />
          <StatTile
            label="Motion Level"
            value={round(features?.motion, 2)}
            unit="g"
            sub="Accelerometer RMS"
            accentColor="var(--chart-motion)"
            onClick={() => navigate('/vitals')}
          />
        </div>

        {/* AI + ECG mini row */}
        <div className="grid-2" style={{ marginBottom: 20 }}>
          {/* ECG sparkline */}
          <div className="card" style={{ cursor: 'pointer' }} onClick={() => navigate('/ecg')}>
            <div className="card-header">
              <div>
                <div className="card-title">ECG Signal</div>
                <div className="card-subtitle">Raw ADC · click to open full view</div>
              </div>
              <span className={badgeClass(ecgResult.badge)}>{ecgResult.label}</span>
            </div>
            <div className="chart-wrapper" style={{ height: 80 }}>
              <ResponsiveContainer width="100%" height={80}>
                <LineChart data={spark}>
                  <Line
                    type="monotone"
                    dataKey="v"
                    stroke="var(--ecg-raw)"
                    strokeWidth={1.5}
                    dot={false}
                    isAnimationActive={false}
                  />
                </LineChart>
              </ResponsiveContainer>
            </div>
            <div style={{ display: 'flex', gap: 12, marginTop: 10, fontSize: '0.8rem', color: 'var(--text-secondary)' }}>
              <span>RR: <strong className="mono">{round(features?.rr, 0)} ms</strong></span>
              <span>ECG Raw: <strong className="mono">{raw?.ecg ?? '--'}</strong></span>
              <span>ECG Filt: <strong className="mono">{features?.ecg_filt ?? '--'}</strong></span>
            </div>
          </div>

          {/* AI Results summary */}
          <div className="card" style={{ cursor: 'pointer' }} onClick={() => navigate('/ai')}>
            <div className="card-header">
              <div>
                <div className="card-title">AI Classifications</div>
                <div className="card-subtitle">Edge inference results · click to open</div>
              </div>
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
              <div className="ai-result-card">
                <div className="ai-result-icon" style={{ background: 'var(--accent-light)', color: 'var(--accent-dark)' }}>〜</div>
                <div>
                  <div className="ai-result-label">ECG Classification</div>
                  <div className="ai-result-value">{ecgResult.label}</div>
                  <div className="ai-result-desc">{ecgResult.desc}</div>
                </div>
                <span className={badgeClass(ecgResult.badge)} style={{ marginLeft: 'auto', alignSelf: 'center' }}>{ecgResult.label}</span>
              </div>
              <div className="ai-result-card">
                <div className="ai-result-icon" style={{ background: '#ede9fe', color: '#6d28d9' }}>◈</div>
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
        <div className="card">
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(7, 1fr)', gap: 16 }}>
            {[
              ['ECG ADC', raw?.ecg, ''],
              ['IR',      raw?.ir,  ''],
              ['RED',     raw?.red, ''],
              ['Accel X', raw?.ax,  'LSB'],
              ['Accel Y', raw?.ay,  'LSB'],
              ['Accel Z', raw?.az,  'LSB'],
              ['Temp',    round(raw?.temp, 1), '°C'],
            ].map(([label, val, unit]) => (
              <div key={label} style={{ textAlign: 'center' }}>
                <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: 4 }}>{label}</div>
                <div className="mono" style={{ fontSize: '1rem', fontWeight: 600 }}>
                  {val ?? '--'}{unit ? <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginLeft: 2 }}>{unit}</span> : null}
                </div>
              </div>
            ))}
          </div>
        </div>

      </div>
    </div>
  );
}
