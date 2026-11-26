import { useState, useEffect } from 'react';
import {
  LineChart, Line, XAxis, YAxis, CartesianGrid,
  Tooltip, ResponsiveContainer, ReferenceLine
} from 'recharts';
import { round, hrZone, spo2Zone, tempZone, badgeClass } from '../../utils/labels';
import Icon from '../shared/Icon';
import ChartTooltip from '../shared/ChartTooltip';

const API_URL = 'http://localhost:8000';

function GaugeBar({ value, min, max, color, label, unit }) {
  const pct = Math.min(100, Math.max(0, ((value - min) / (max - min)) * 100));
  const hasValue = Number.isFinite(value);
  return (
    <div className="gauge">
      <div className="gauge-head">
        <span>{label}</span>
        <strong className="mono">{hasValue ? value : '--'} <em>{unit}</em></strong>
      </div>
      <div className="gauge-track">
        <div className="gauge-fill" style={{ width: `${hasValue ? pct : 0}%`, background: color }} />
      </div>
      <div className="gauge-scale">
        <span>{min}</span><span>{max} {unit}</span>
      </div>
    </div>
  );
}

export default function VitalSigns({ features, raw }) {
  const [trend, setTrend] = useState([]);

  const hr     = round(features?.hr,     0);
  const spo2   = round(raw?.spo2,        1);
  const hrv    = round(features?.hrv,    1);
  const temp   = round(raw?.temp,        2);
  const motion = round(features?.motion, 2);
  const rr     = round(features?.rr,     0);

  const hrInfo   = hrZone(features?.hr);
  const spo2Info = spo2Zone(raw?.spo2);
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

  return (
    <div>
      <div className="page-header">
        <div>
          <h2>Vital Signs</h2>
          <p>Heart rate, SpO₂, HRV, TMP117 temperature &amp; MPU6500 motion — live and 10-minute trend</p>
        </div>
        <div className="header-right">
          <span className={badgeClass(hrInfo.badge)}>HR {hrInfo.label}</span>
          <span className={badgeClass(spo2Info.badge)}>SpO₂ {spo2Info.label}</span>
          <span className={badgeClass(tmpInfo.badge)}>Temp {tmpInfo.label}</span>
        </div>
      </div>

      <div className="page-body">

        {/* Big stat tiles */}
        <div className="stat-grid stat-grid-5" style={{ marginBottom: 24 }}>
          {[
            { label: 'Heart Rate',       value: hr,     unit: 'bpm', color: 'var(--chart-hr)',     icon: 'heart',  info: hrInfo },
            { label: 'SpO₂',             value: spo2,   unit: '%',   color: 'var(--chart-spo2)',   icon: 'drop',   info: spo2Info },
            { label: 'HRV',              value: hrv,    unit: 'ms',  color: 'var(--chart-hrv)',    icon: 'wave',   info: { badge: 'info' } },
            { label: 'Temp (TMP117)',    value: temp,   unit: '°C',  color: 'var(--chart-temp)',   icon: 'thermo', info: tmpInfo },
            { label: 'Motion (MPU6500)', value: motion, unit: 'g',   color: 'var(--chart-motion)', icon: 'motion', info: { badge: 'neutral' } },
          ].map(({ label, value, unit, color, icon, info }) => (
            <div key={label} className="stat-tile" style={{ '--tile-color': color }}>
              <div className="stat-tile-top">
                <div className="stat-tile-label"><span>{label}</span></div>
                <span className="stat-tile-icon"><Icon name={icon} size={17} /></span>
              </div>
              <div className="stat-tile-value">
                {value}
                <span className="stat-tile-unit">{unit}</span>
              </div>
              {info.label && <div className="stat-tile-sub"><span className={badgeClass(info.badge)}>{info.label}</span></div>}
            </div>
          ))}
        </div>

        {/* Gauge bars */}
        <div className="grid-2" style={{ marginBottom: 24 }}>
          <div className="card">
            <div className="card-header">
              <div className="card-title">Cardiac &amp; Oxygenation Indicators</div>
            </div>
            <GaugeBar value={Number(hr)}     min={40}  max={180} color="var(--chart-hr)"   label="Heart Rate"   unit="bpm" />
            <GaugeBar value={Number(spo2)}   min={85}  max={100} color="var(--chart-spo2)" label="SpO₂"         unit="%" />
            <GaugeBar value={Number(hrv)}    min={0}   max={100} color="var(--chart-hrv)"  label="HRV"          unit="ms"  />
            <GaugeBar value={Number(rr)}     min={300} max={1400}color="var(--accent)"      label="RR Interval"  unit="ms"  />
          </div>
          <div className="card">
            <div className="card-header">
              <div className="card-title">Physical Indicators (TMP117 &amp; MPU6500)</div>
            </div>
            <GaugeBar value={Number(temp)}   min={34}  max={41}  color="var(--chart-temp)"   label="Body Temp"    unit="°C"  />
            <GaugeBar value={Number(motion)} min={0}   max={5}   color="var(--chart-motion)" label="Motion"       unit="g"   />
            <div className="inset-note">
              <div className="inset-note-label">Accel raw (X / Y / Z)</div>
              <div className="mono inset-note-value">
                {raw?.ax ?? '--'} / {raw?.ay ?? '--'} / {raw?.az ?? '--'} LSB
              </div>
              <div className="inset-note-sub">
                IR: {raw?.ir ?? '--'} · RED: {raw?.red ?? '--'}
              </div>
            </div>
          </div>
        </div>

        {/* Trend chart */}
        <div className="card">
          <div className="card-header">
            <div>
              <div className="card-title">10-Minute Vitals Trend</div>
              <div className="card-subtitle">Sampled from SQLite every 5 s</div>
            </div>
            <div className="legend">
              {[
                ['HR (bpm)', 'var(--chart-hr)'],
                ['HRV (ms)', 'var(--chart-hrv)'],
                ['Temp (°C)', 'var(--chart-temp)'],
                ['Motion (g)', 'var(--chart-motion)'],
              ].map(([label, color]) => (
                <div key={label} className="legend-item">
                  <span className="legend-swatch" style={{ background: color }} />
                  {label}
                </div>
              ))}
            </div>
          </div>
          {trendLabels.length < 2 ? (
            <div className="empty-state">
              Collecting data… ({trendLabels.length} records so far)
            </div>
          ) : (
            <ResponsiveContainer width="100%" height={280}>
              <LineChart data={trendLabels} margin={{ top: 8, right: 12, bottom: 4, left: 0 }}>
                <CartesianGrid stroke="var(--grid)" vertical={false} />
                <XAxis dataKey="idx" hide />
                <YAxis width={45} axisLine={false} tickLine={false} tick={{ fontSize: 11, fontFamily: 'var(--font-mono)' }} />
                <Tooltip content={<ChartTooltip digits={1} />} cursor={{ stroke: 'var(--border-strong)', strokeWidth: 1 }} />
                <Line type="monotone" dataKey="hr"     name="HR (bpm)"  stroke="var(--chart-hr)"     strokeWidth={2.2} dot={false} isAnimationActive={false} />
                <Line type="monotone" dataKey="hrv"    name="HRV (ms)"  stroke="var(--chart-hrv)"    strokeWidth={2.2} dot={false} isAnimationActive={false} />
                <Line type="monotone" dataKey="temp"   name="Temp (°C)" stroke="var(--chart-temp)"   strokeWidth={2.2} dot={false} isAnimationActive={false} />
                <Line type="monotone" dataKey="motion" name="Motion (g)"stroke="var(--chart-motion)" strokeWidth={2.2} dot={false} isAnimationActive={false} />
                <ReferenceLine y={60}  stroke="var(--chart-hr)" strokeDasharray="4 4" opacity={0.35} />
                <ReferenceLine y={100} stroke="var(--chart-hr)" strokeDasharray="4 4" opacity={0.35} />
              </LineChart>
            </ResponsiveContainer>
          )}
        </div>

      </div>
    </div>
  );
}
