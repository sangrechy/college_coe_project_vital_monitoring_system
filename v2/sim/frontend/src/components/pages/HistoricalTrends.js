import { useState, useEffect } from 'react';
import {
  LineChart, Line, XAxis, YAxis, CartesianGrid,
  Tooltip, ResponsiveContainer, Legend
} from 'recharts';

const API_URL = 'http://localhost:8000';

const WINDOWS = [
  { label: '2 min',  value: 2 },
  { label: '5 min',  value: 5 },
  { label: '10 min', value: 10 },
  { label: '30 min', value: 30 },
];

function TrendChart({ title, subtitle, data, lines, height = 200 }) {
  const CustomTooltip = ({ active, payload }) => {
    if (!active || !payload?.length) return null;
    return (
      <div style={{
        background: 'var(--bg-card)', border: '1px solid var(--border)',
        borderRadius: 6, padding: '8px 12px', fontSize: '0.78rem'
      }}>
        {payload.map(p => (
          <div key={p.dataKey} style={{ color: p.color }}>
            {p.name}: <strong>{typeof p.value === 'number' ? p.value.toFixed(1) : p.value}</strong>
          </div>
        ))}
      </div>
    );
  };

  return (
    <div className="card">
      <div className="card-header">
        <div>
          <div className="card-title">{title}</div>
          {subtitle && <div className="card-subtitle">{subtitle}</div>}
        </div>
      </div>
      {data.length < 2 ? (
        <div style={{ textAlign: 'center', color: 'var(--text-muted)', padding: '40px 0', fontSize: '0.85rem' }}>
          Not enough data yet. Keep the device connected to accumulate history.
        </div>
      ) : (
        <ResponsiveContainer width="100%" height={height}>
          <LineChart data={data} margin={{ top: 8, right: 12, bottom: 4, left: 0 }}>
            <CartesianGrid stroke="var(--border)" strokeDasharray="3 3" />
            <XAxis dataKey="idx" hide />
            <YAxis width={45} tick={{ fontSize: 11, fontFamily: 'var(--font-mono)' }} />
            <Tooltip content={<CustomTooltip />} />
            {lines.map(l => (
              <Line
                key={l.key}
                type="monotone"
                dataKey={l.key}
                name={l.name}
                stroke={l.color}
                strokeWidth={1.5}
                dot={false}
                isAnimationActive={false}
              />
            ))}
          </LineChart>
        </ResponsiveContainer>
      )}
    </div>
  );
}

export default function HistoricalTrends() {
  const [window, setWindow] = useState(10);
  const [vitals, setVitals] = useState([]);
  const [ecg,    setECG]    = useState([]);
  const [loading, setLoading] = useState(false);

  const load = async () => {
    setLoading(true);
    try {
      const [vRes, eRes] = await Promise.all([
        fetch(`${API_URL}/api/history/vitals?minutes=${window}`),
        fetch(`${API_URL}/api/history/ecg?minutes=${window}`),
      ]);
      const vData = await vRes.json();
      const eData = await eRes.json();
      setVitals((vData.data || []).map((r, i) => ({ ...r, idx: i })));
      setECG((eData.data   || []).map((r, i) => ({ idx: i, v: r.v })));
    } catch {}
    setLoading(false);
  };

  useEffect(() => { load(); }, [window]);
  useEffect(() => {
    const id = setInterval(load, 10000);
    return () => clearInterval(id);
  }, [window]);

  return (
    <div>
      <div className="page-header">
        <div>
          <h2>Historical Trends</h2>
          <p>Time-series data from local SQLite database</p>
        </div>
        <div className="header-right">
          <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>Window:</span>
          {WINDOWS.map(w => (
            <button
              key={w.value}
              className={`btn btn-sm ${window === w.value ? 'btn-primary' : 'btn-outline'}`}
              onClick={() => setWindow(w.value)}
            >
              {w.label}
            </button>
          ))}
        </div>
      </div>

      <div className="page-body">

        {loading && (
          <div className="alert alert-info" style={{ marginBottom: 12 }}>Loading data…</div>
        )}

        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>

          {/* ECG trend */}
          <TrendChart
            title="ECG Raw Trend"
            subtitle={`Raw ADC values over the last ${window} minutes`}
            data={ecg}
            lines={[{ key: 'v', name: 'ECG Raw', color: 'var(--ecg-raw)' }]}
            height={180}
          />

          {/* Heart Rate + HRV */}
          <TrendChart
            title="Heart Rate & HRV Trend"
            subtitle="bpm and ms variability"
            data={vitals}
            lines={[
              { key: 'hr',  name: 'HR (bpm)', color: 'var(--chart-hr)' },
              { key: 'hrv', name: 'HRV (ms)', color: 'var(--chart-hrv)' },
            ]}
            height={200}
          />

          {/* Temperature */}
          <TrendChart
            title="Temperature Trend"
            subtitle="Body temperature from MLX90614 IR sensor (°C)"
            data={vitals}
            lines={[{ key: 'temp', name: 'Temp (°C)', color: 'var(--chart-temp)' }]}
            height={160}
          />

          {/* Motion */}
          <TrendChart
            title="Motion Level Trend"
            subtitle="MPU6050 accelerometer RMS (g)"
            data={vitals}
            lines={[{ key: 'motion', name: 'Motion (g)', color: 'var(--chart-motion)' }]}
            height={160}
          />

          {/* AI Class timeline */}
          <TrendChart
            title="AI Classification Timeline"
            subtitle="ECG class · Stress class · Overall status (0=normal, 1=warn, 2=alert)"
            data={vitals}
            lines={[
              { key: 'ecg_class',    name: 'ECG Class',    color: 'var(--ecg-raw)' },
              { key: 'stress_class', name: 'Stress Class', color: 'var(--chart-hrv)' },
              { key: 'status',       name: 'Status',       color: 'var(--chart-hr)' },
            ]}
            height={160}
          />

        </div>

        <div style={{ marginTop: 16 }}>
          <button className="btn btn-outline" onClick={load}>
            ↺ Refresh now
          </button>
          <span style={{ marginLeft: 12, fontSize: '0.78rem', color: 'var(--text-muted)' }}>
            Auto-refreshes every 10 seconds · {vitals.length} vitals records · {ecg.length} ECG records
          </span>
        </div>

      </div>
    </div>
  );
}
