/**
 * Shared chart tooltip (presentational only).
 */
export default function ChartTooltip({ active, payload, digits }) {
  if (!active || !payload?.length) return null;
  return (
    <div className="chart-tooltip">
      {payload.map(p => (
        <div key={p.dataKey} className="chart-tooltip-row">
          <span className="chart-tooltip-dot" style={{ background: p.color || p.stroke }} />
          <span className="chart-tooltip-name">{p.name}</span>
          <strong className="mono">
            {typeof p.value === 'number' && digits != null ? p.value.toFixed(digits) : p.value}
          </strong>
        </div>
      ))}
    </div>
  );
}
