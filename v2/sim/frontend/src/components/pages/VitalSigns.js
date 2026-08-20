import { useState, useEffect } from 'react';
import {
  LineChart, Line, XAxis, YAxis, CartesianGrid,
  Tooltip, ResponsiveContainer, ReferenceLine
} from 'recharts';
import { round, hrZone, tempZone, badgeClass } from '../../utils/labels';

const API_URL = 'http://localhost:8000';

function GaugeBar({ value, min, max, color, label, unit }) {
  const pct = Math.min(100, Math.max(0, ((value - min) / (max - min)) * 100));
  return (
    <div style={{ marginBottom: 4 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.75rem', color: 'var(--text-secondary)', marginBottom: 4 }}>
        <span>{label}</span>
        <strong className="mono">{value ?? '--'} {unit}</strong>
      </div>
      <div style={{ height: 8, background: 'var(--bg-page)', borderRadius: 4, overflow: 'hidden' }}>
        <div style={{ width: `${pct}%`, height: '100%', background: color, borderRadius: 4, transition: 'width 0.3s' }} />
      </div>
      <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.68rem', color: 'var(--text-muted)', marginTop: 2 }}>
        <span>{min}</span><span>{max} {unit}</span>
      </div>
    </div>
  );
}

export default function VitalSigns({ features, raw }) {
  const [trend, setTrend] = useState([]);

  const hr     = round(features?.hr,     0);
  const hrv    = round(features?.hrv,    1);
  const temp   = round(raw?.temp,        1);
  const motion = round(features?.motion, 2);
  const rr     = round(features?.rr,     0);

  const hrInfo   = hrZone(features?.hr);
  const tmpInfo  = tempZone(raw?.temp);

  useEffect(() => {
    const load = async () => {
      try {
        const res  = await fetch(`${API_URL}/api/history/vitals?minutes=10`);
        const data = await res.json();
        setTrend(data.data || []);
      } catch {}
    };
    load();
    const id = setInterval(load, 5000);
    return () => clearInterval(id);
  }, []);

  const trendLabels = trend.map((r, i) => ({ ...r, idx: i }));

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
    <div>
      <div className="page-header">
        <div>
          <h2>Vital Signs</h2>
          <p>Heart rate, HRV, temperature and motion — live and 10-minute trend</p>
        </div>
        <div className="header-right">
          <span className={badgeClass(hrInfo.badge)}>HR {hrInfo.label}</span>
          <span className={badgeClass(tmpInfo.badge)}>Temp {tmpInfo.label}</span>
        </div>
      </div>

      <div className="page-body">

        {/* Big stat tiles */}
        <div className="stat-grid stat-grid-4" style={{ marginBottom: 20 }}>
          {[
            { label: 'Heart Rate',    value: hr,     unit: 'bpm', color: 'var(--chart-hr)',     info: hrInfo },
            { label: 'HRV',           value: hrv,    unit: 'ms',  color: 'var(--chart-hrv)',    info: { badge: 'info' } },
            { label: 'Temperature',   value: temp,   unit: '°C',  color: 'var(--chart-temp)',   info: tmpInfo },
            { label: 'Motion Level',  value: motion, unit: 'g',   color: 'var(--chart-motion)', info: { badge: 'neutral' } },
          ].map(({ label, value, unit, color, info }) => (
            <div key={label} className="stat-tile">
              <div className="stat-tile-label">{label}</div>
              <div className="stat-tile-value">
                {value}
                <span className="stat-tile-unit">{unit}</span>
              </div>
              {info.label && <div className="stat-tile-sub"><span className={badgeClass(info.badge)}>{info.label}</span></div>}
              <div className="stat-tile-accent" style={{ background: color }} />
            </div>
          ))}
        </div>

        {/* Gauge bars */}
        <div className="grid-2" style={{ marginBottom: 20 }}>
          <div className="card">
            <div className="card-header">
              <div className="card-title">Cardiac Indicators</div>
            </div>
            <GaugeBar value={Number(hr)}     min={40}  max={180} color="var(--chart-hr)"   label="Heart Rate"   unit="bpm" />
            <GaugeBar value={Number(hrv)}    min={0}   max={100} color="var(--chart-hrv)"  label="HRV"          unit="ms"  />
            <GaugeBar value={Number(rr)}     min={300} max={1400}color="var(--accent)"      label="RR Interval"  unit="ms"  />
          </div>
          <div className="card">
            <div className="card-header">
              <div className="card-title">Physical Indicators</div>
            </div>
            <GaugeBar value={Number(temp)}   min={34}  max={41}  color="var(--chart-temp)"   label="Body Temp"    unit="°C"  />
            <GaugeBar value={Number(motion)} min={0}   max={5}   color="var(--chart-motion)" label="Motion"       unit="g"   />
            <div style={{ marginTop: 12, padding: '10px 0', borderTop: '1px solid var(--border)' }}>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Accel raw (X / Y / Z)</div>
              <div className="mono" style={{ fontSize: '0.9rem', fontWeight: 600, marginTop: 4 }}>
                {raw?.ax ?? '--'} / {raw?.ay ?? '--'} / {raw?.az ?? '--'} LSB
              </div>
              <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)', marginTop: 2 }}>
                IR: {raw?.ir ?? '--'} · RED: {raw?.red ?? '--'}
              </div>
            </div>
          </div>
        </div>

        {/* Trend chart */}
        <div className="card">
          <div className="card-header">
            <div className="card-title">10-Minute Vitals Trend</div>
            <div className="card-subtitle">Sampled from SQLite every 5 s</div>
          </div>
          {trendLabels.length < 2 ? (
            <div style={{ textAlign: 'center', color: 'var(--text-muted)', padding: '40px 0', fontSize: '0.85rem' }}>
              Collecting data… ({trendLabels.length} records so far)
            </div>
          ) : (
            <ResponsiveContainer width="100%" height={260}>
              <LineChart data={trendLabels} margin={{ top: 8, right: 12, bottom: 4, left: 0 }}>
                <CartesianGrid stroke="var(--border)" strokeDasharray="3 3" />
                <XAxis dataKey="idx" hide />
                <YAxis width={45} tick={{ fontSize: 11, fontFamily: 'var(--font-mono)' }} />
                <Tooltip content={<CustomTooltip />} />
                <Line type="monotone" dataKey="hr"     name="HR (bpm)"  stroke="var(--chart-hr)"     strokeWidth={1.5} dot={false} isAnimationActive={false} />
                <Line type="monotone" dataKey="hrv"    name="HRV (ms)"  stroke="var(--chart-hrv)"    strokeWidth={1.5} dot={false} isAnimationActive={false} />
                <Line type="monotone" dataKey="temp"   name="Temp (°C)" stroke="var(--chart-temp)"   strokeWidth={1.5} dot={false} isAnimationActive={false} />
                <Line type="monotone" dataKey="motion" name="Motion (g)"stroke="var(--chart-motion)" strokeWidth={1.5} dot={false} isAnimationActive={false} />
                <ReferenceLine y={60}  stroke="var(--chart-hr)" strokeDasharray="4 3" opacity={0.4} />
                <ReferenceLine y={100} stroke="var(--chart-hr)" strokeDasharray="4 3" opacity={0.4} />
              </LineChart>
            </ResponsiveContainer>
          )}
          {/* Legend */}
          <div style={{ display: 'flex', gap: 18, marginTop: 10, flexWrap: 'wrap' }}>
            {[
              ['HR (bpm)', 'var(--chart-hr)'],
              ['HRV (ms)', 'var(--chart-hrv)'],
              ['Temp (°C)', 'var(--chart-temp)'],
              ['Motion (g)', 'var(--chart-motion)'],
            ].map(([label, color]) => (
              <div key={label} style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: '0.78rem', color: 'var(--text-secondary)' }}>
                <span style={{ display: 'inline-block', width: 14, height: 2, background: color, borderRadius: 1 }} />
                {label}
              </div>
            ))}
          </div>
        </div>

      </div>
    </div>
  );
}
