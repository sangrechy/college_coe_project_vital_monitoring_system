import {
  LineChart, Line, XAxis, YAxis, CartesianGrid,
  Tooltip, ResponsiveContainer, ReferenceLine
} from 'recharts';
import { round, getECGClass } from '../../utils/labels';

const ECG_THRESHOLD = 1800; // mirror firmware constant

export default function ECGMonitor({ ecgRawBuffer, ecgFiltBuffer, features, raw, ai }) {
  const ecgResult = getECGClass(ai?.ecg_class);

  // Build combined dataset for dual chart
  const combined = ecgRawBuffer.map((p, i) => ({
    i,
    raw:  p.v,
    filt: ecgFiltBuffer[i]?.v ?? null,
  }));

  const latestRaw  = raw?.ecg        ?? '--';
  const latestFilt = features?.ecg_filt ?? '--';
  const latestRR   = round(features?.rr, 0);
  const latestHR   = round(features?.hr, 0);

  const CustomTooltip = ({ active, payload }) => {
    if (!active || !payload?.length) return null;
    return (
      <div style={{
        background: 'var(--bg-card)', border: '1px solid var(--border)',
        borderRadius: 6, padding: '8px 12px', fontSize: '0.78rem'
      }}>
        {payload.map(p => (
          <div key={p.dataKey} style={{ color: p.color }}>
            {p.name}: <strong>{p.value}</strong>
          </div>
        ))}
      </div>
    );
  };

  return (
    <div>
      <div className="page-header">
        <div>
          <h2>ECG Monitor</h2>
          <p>Live raw and filtered ECG signal from AD8232 sensor</p>
        </div>
        <div className="header-right">
          <span className={`badge badge-${ecgResult.badge}`}>{ecgResult.label}</span>
          <span>HR: <strong className="mono">{latestHR} bpm</strong></span>
          <span>RR: <strong className="mono">{latestRR} ms</strong></span>
        </div>
      </div>

      <div className="page-body">

        {/* Stat strip */}
        <div className="stat-grid stat-grid-4" style={{ marginBottom: 20 }}>
          <div className="stat-tile">
            <div className="stat-tile-label">Raw ECG</div>
            <div className="stat-tile-value mono" style={{ fontSize: '1.5rem' }}>{latestRaw}</div>
            <div className="stat-tile-sub">ADC counts</div>
            <div className="stat-tile-accent" style={{ background: 'var(--ecg-raw)' }} />
          </div>
          <div className="stat-tile">
            <div className="stat-tile-label">Filtered ECG</div>
            <div className="stat-tile-value mono" style={{ fontSize: '1.5rem' }}>{latestFilt}</div>
            <div className="stat-tile-sub">8-pt moving avg</div>
            <div className="stat-tile-accent" style={{ background: 'var(--ecg-filt)' }} />
          </div>
          <div className="stat-tile">
            <div className="stat-tile-label">RR Interval</div>
            <div className="stat-tile-value mono" style={{ fontSize: '1.5rem' }}>{latestRR}</div>
            <div className="stat-tile-sub">ms between beats</div>
            <div className="stat-tile-accent" style={{ background: 'var(--chart-hrv)' }} />
          </div>
          <div className="stat-tile">
            <div className="stat-tile-label">ECG Class</div>
            <div className="stat-tile-value" style={{ fontSize: '1.3rem' }}>{ecgResult.label}</div>
            <div className="stat-tile-sub">{ecgResult.desc.slice(0, 30)}</div>
            <div className="stat-tile-accent" style={{
              background: ecgResult.badge === 'ok' ? 'var(--ok-color)'
                : ecgResult.badge === 'danger' ? 'var(--danger-color)'
                : 'var(--border)'
            }} />
          </div>
        </div>

        {/* Raw ECG */}
        <div className="card" style={{ marginBottom: 16 }}>
          <div className="card-header">
            <div>
              <div className="card-title">Raw ECG Signal</div>
              <div className="card-subtitle">AD8232 analog output · last {ecgRawBuffer.length} samples</div>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <span style={{ display: 'inline-block', width: 14, height: 2, background: 'var(--ecg-raw)', borderRadius: 1 }} />
              <span style={{ fontSize: '0.78rem', color: 'var(--text-secondary)' }}>Raw ADC</span>
            </div>
          </div>
          <div className="chart-wrapper">
            <ResponsiveContainer width="100%" height={200}>
              <LineChart data={combined} margin={{ top: 8, right: 12, bottom: 4, left: 0 }}>
                <CartesianGrid stroke="var(--border)" strokeDasharray="3 3" />
                <XAxis dataKey="i" hide />
                <YAxis
                  domain={['auto', 'auto']}
                  width={50}
                  tick={{ fontSize: 11, fontFamily: 'var(--font-mono)' }}
                />
                <Tooltip content={<CustomTooltip />} />
                <ReferenceLine
                  y={ECG_THRESHOLD}
                  stroke="var(--danger-color)"
                  strokeDasharray="4 3"
                  label={{ value: 'Threshold', position: 'insideTopRight', fontSize: 10, fill: 'var(--danger-color)' }}
                />
                <Line
                  type="monotone"
                  dataKey="raw"
                  name="Raw ECG"
                  stroke="var(--ecg-raw)"
                  strokeWidth={1.5}
                  dot={false}
                  isAnimationActive={false}
                />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Filtered ECG */}
        <div className="card">
          <div className="card-header">
            <div>
              <div className="card-title">Filtered ECG Signal</div>
              <div className="card-subtitle">After 8-point moving average filter (firmware-side)</div>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <span style={{ display: 'inline-block', width: 14, height: 2, background: 'var(--ecg-filt)', borderRadius: 1 }} />
              <span style={{ fontSize: '0.78rem', color: 'var(--text-secondary)' }}>Filtered</span>
            </div>
          </div>
          <div className="chart-wrapper">
            <ResponsiveContainer width="100%" height={200}>
              <LineChart data={combined} margin={{ top: 8, right: 12, bottom: 4, left: 0 }}>
                <CartesianGrid stroke="var(--border)" strokeDasharray="3 3" />
                <XAxis dataKey="i" hide />
                <YAxis
                  domain={['auto', 'auto']}
                  width={50}
                  tick={{ fontSize: 11, fontFamily: 'var(--font-mono)' }}
                />
                <Tooltip content={<CustomTooltip />} />
                <Line
                  type="monotone"
                  dataKey="filt"
                  name="Filtered ECG"
                  stroke="var(--ecg-filt)"
                  strokeWidth={1.5}
                  dot={false}
                  isAnimationActive={false}
                />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Info note */}
        <div className="alert alert-info" style={{ marginTop: 16 }}>
          <span>ℹ</span>
          <span>
            The firmware uses an 8-point moving average (ECG_FILTER_SIZE=8) and detects peaks above
            threshold <strong>1800</strong> with a <strong>300 ms</strong> refractory period.
            RR intervals valid between 300–2000 ms.
          </span>
        </div>

      </div>
    </div>
  );
}
