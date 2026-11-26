import {
  AreaChart, Area, XAxis, YAxis, CartesianGrid,
  Tooltip, ResponsiveContainer, ReferenceLine
} from 'recharts';
import { round, getECGClass } from '../../utils/labels';
import Icon from '../shared/Icon';
import ChartTooltip from '../shared/ChartTooltip';

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

  const classColor = ecgResult.badge === 'ok' ? 'var(--ok-color)'
    : ecgResult.badge === 'danger' ? 'var(--danger-color)'
    : ecgResult.badge === 'warn' ? 'var(--warn-color)'
    : 'var(--border-strong)';

  return (
    <div>
      <div className="page-header">
        <div>
          <h2>ECG Monitor</h2>
          <p>Live raw and filtered ECG signal from AD8232 sensor</p>
        </div>
        <div className="header-right">
          <span className={`badge badge-${ecgResult.badge}`}>{ecgResult.label}</span>
          <span className="header-stat">HR <strong className="mono">{latestHR} bpm</strong></span>
          <span className="header-stat">RR <strong className="mono">{latestRR} ms</strong></span>
        </div>
      </div>

      <div className="page-body">

        {/* Stat strip */}
        <div className="stat-grid stat-grid-4" style={{ marginBottom: 24 }}>
          <div className="stat-tile" style={{ '--tile-color': 'var(--ecg-raw)' }}>
            <div className="stat-tile-top"><div className="stat-tile-label"><span>Raw ECG</span></div></div>
            <div className="stat-tile-value stat-tile-value-md">{latestRaw}</div>
            <div className="stat-tile-sub">ADC counts</div>
          </div>
          <div className="stat-tile" style={{ '--tile-color': 'var(--ecg-filt)' }}>
            <div className="stat-tile-top"><div className="stat-tile-label"><span>Filtered ECG</span></div></div>
            <div className="stat-tile-value stat-tile-value-md">{latestFilt}</div>
            <div className="stat-tile-sub">8-pt moving avg</div>
          </div>
          <div className="stat-tile" style={{ '--tile-color': 'var(--chart-hrv)' }}>
            <div className="stat-tile-top"><div className="stat-tile-label"><span>RR Interval</span></div></div>
            <div className="stat-tile-value stat-tile-value-md">{latestRR}</div>
            <div className="stat-tile-sub">ms between beats</div>
          </div>
          <div className="stat-tile" style={{ '--tile-color': classColor }}>
            <div className="stat-tile-top"><div className="stat-tile-label"><span>ECG Class</span></div></div>
            <div className="stat-tile-value stat-tile-value-text">{ecgResult.label}</div>
            <div className="stat-tile-sub">{ecgResult.desc}</div>
          </div>
        </div>

        {/* Raw ECG */}
        <div className="card" style={{ marginBottom: 20 }}>
          <div className="card-header">
            <div>
              <div className="card-title">Raw ECG Signal</div>
              <div className="card-subtitle">AD8232 analog output · last {ecgRawBuffer.length} samples</div>
            </div>
            <div className="legend">
              <div className="legend-item"><span className="legend-swatch" style={{ background: 'var(--ecg-raw)' }} />Raw ADC</div>
            </div>
          </div>
          <div className="chart-wrapper">
            <ResponsiveContainer width="100%" height={220}>
              <AreaChart data={combined} margin={{ top: 8, right: 12, bottom: 4, left: 0 }}>
                <defs>
                  <linearGradient id="ecgRawFill" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%"   stopColor="var(--ecg-raw)" stopOpacity={0.22} />
                    <stop offset="100%" stopColor="var(--ecg-raw)" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid stroke="var(--grid)" vertical={false} />
                <XAxis dataKey="i" hide />
                <YAxis
                  domain={['auto', 'auto']}
                  width={50}
                  axisLine={false}
                  tickLine={false}
                  tick={{ fontSize: 11, fontFamily: 'var(--font-mono)' }}
                />
                <Tooltip content={<ChartTooltip />} cursor={{ stroke: 'var(--border-strong)', strokeWidth: 1 }} />
                <ReferenceLine
                  y={ECG_THRESHOLD}
                  stroke="var(--danger-color)"
                  strokeDasharray="4 4"
                  strokeOpacity={0.6}
                  label={{ value: 'Threshold', position: 'insideTopRight', fontSize: 10, fill: 'var(--danger-color)' }}
                />
                <Area
                  type="monotone"
                  dataKey="raw"
                  name="Raw ECG"
                  stroke="var(--ecg-raw)"
                  strokeWidth={2}
                  fill="url(#ecgRawFill)"
                  dot={false}
                  isAnimationActive={false}
                />
              </AreaChart>
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
            <div className="legend">
              <div className="legend-item"><span className="legend-swatch" style={{ background: 'var(--ecg-filt)' }} />Filtered</div>
            </div>
          </div>
          <div className="chart-wrapper">
            <ResponsiveContainer width="100%" height={220}>
              <AreaChart data={combined} margin={{ top: 8, right: 12, bottom: 4, left: 0 }}>
                <defs>
                  <linearGradient id="ecgFiltFill" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%"   stopColor="var(--ecg-filt)" stopOpacity={0.2} />
                    <stop offset="100%" stopColor="var(--ecg-filt)" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid stroke="var(--grid)" vertical={false} />
                <XAxis dataKey="i" hide />
                <YAxis
                  domain={['auto', 'auto']}
                  width={50}
                  axisLine={false}
                  tickLine={false}
                  tick={{ fontSize: 11, fontFamily: 'var(--font-mono)' }}
                />
                <Tooltip content={<ChartTooltip />} cursor={{ stroke: 'var(--border-strong)', strokeWidth: 1 }} />
                <Area
                  type="monotone"
                  dataKey="filt"
                  name="Filtered ECG"
                  stroke="var(--ecg-filt)"
                  strokeWidth={2}
                  fill="url(#ecgFiltFill)"
                  dot={false}
                  isAnimationActive={false}
                />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Info note */}
        <div className="alert alert-info" style={{ marginTop: 20 }}>
          <Icon name="info" size={18} />
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
