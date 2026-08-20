import { useState } from 'react';

const API_URL = 'http://localhost:8000';

export default function Settings({ clearHistory, dbCount }) {
  const [confirmClear, setConfirmClear] = useState(false);
  const [clearing, setClearing] = useState(false);
  const [message, setMessage] = useState(null);

  const handleClear = async () => {
    if (!confirmClear) {
      setConfirmClear(true);
      return;
    }
    setClearing(true);
    try {
      await clearHistory();
      setMessage({ type: 'ok', text: 'All historical records cleared.' });
    } catch {
      setMessage({ type: 'danger', text: 'Failed to clear records.' });
    }
    setClearing(false);
    setConfirmClear(false);
  };

  return (
    <div>
      <div className="page-header">
        <div>
          <h2>Settings</h2>
          <p>Application configuration and data management</p>
        </div>
      </div>

      <div className="page-body">

        {message && (
          <div className={`alert alert-${message.type}`} style={{ marginBottom: 16 }}>
            {message.text}
          </div>
        )}

        {/* API & Connection config */}
        <div className="card" style={{ marginBottom: 16 }}>
          <div className="card-header">
            <div className="card-title">Backend Configuration</div>
            <div className="card-subtitle">These values match the current deployment</div>
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0 24px' }}>
            {[
              ['Backend URL',     'http://localhost:8000'],
              ['WebSocket URL',   'ws://localhost:8000/ws'],
              ['REST API prefix', '/api'],
              ['Database',        'SQLite · health_data.db'],
              ['BLE Library',     'Bleak (Python)'],
              ['Framework',       'FastAPI + Uvicorn'],
            ].map(([k, v]) => (
              <div key={k} className="status-row">
                <span className="status-label">{k}</span>
                <span className="status-value mono" style={{ fontSize: '0.78rem' }}>{v}</span>
              </div>
            ))}
          </div>
          <div className="alert alert-info" style={{ marginTop: 14, marginBottom: 0 }}>
            <span>ℹ</span>
            <span>To change the backend host or port, edit <strong>frontend/src/hooks/useWearable.js</strong> and <strong>backend/main.py</strong>.</span>
          </div>
        </div>

        {/* Data management */}
        <div className="card" style={{ marginBottom: 16 }}>
          <div className="card-header">
            <div className="card-title">Data Management</div>
          </div>
          <div className="status-row">
            <span className="status-label">Records stored</span>
            <span className="status-value mono">{dbCount?.toLocaleString() ?? '—'}</span>
          </div>
          <div className="status-row">
            <span className="status-label">Database file</span>
            <span className="status-value mono">backend/health_data.db</span>
          </div>
          <div style={{ marginTop: 16, display: 'flex', alignItems: 'center', gap: 12 }}>
            <button
              className="btn btn-danger btn-sm"
              onClick={handleClear}
              disabled={clearing}
            >
              {clearing ? 'Clearing…' : confirmClear ? '⚠ Confirm — this cannot be undone' : 'Clear All History'}
            </button>
            {confirmClear && (
              <button className="btn btn-outline btn-sm" onClick={() => setConfirmClear(false)}>
                Cancel
              </button>
            )}
          </div>
        </div>

        {/* Architecture reference */}
        <div className="card" style={{ marginBottom: 16 }}>
          <div className="card-header">
            <div className="card-title">System Architecture</div>
          </div>
          <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', lineHeight: 1.8 }}>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 12, marginBottom: 12 }}>
              {[
                { title: 'ESP32-S3', items: ['AD8232 ECG', 'MAX30102 SpO₂', 'MLX90614 IR Temp', 'MPU6050 IMU', 'Edge AI inference', 'BLE Notifications @ 10Hz'] },
                { title: 'Backend (Python)', items: ['Bleak BLE client', 'FastAPI REST', 'WebSocket broadcast', 'SQLite storage', 'Auto-reconnect', 'JSON packet parse'] },
                { title: 'Frontend (React)', items: ['7-page dashboard', 'WebSocket live feed', 'Recharts graphs', 'REST data polling', 'BLE scanner UI', 'No cloud dependency'] },
              ].map(sec => (
                <div key={sec.title} style={{
                  border: '1px solid var(--border)', borderRadius: 'var(--radius)', padding: '12px 14px'
                }}>
                  <div style={{ fontWeight: 600, color: 'var(--text-primary)', marginBottom: 8, fontSize: '0.85rem' }}>{sec.title}</div>
                  {sec.items.map(item => (
                    <div key={item} style={{ fontSize: '0.78rem', marginBottom: 3, color: 'var(--text-secondary)' }}>· {item}</div>
                  ))}
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Quick start */}
        <div className="card">
          <div className="card-header">
            <div className="card-title">Quick Start Reference</div>
          </div>
          <div style={{ background: 'var(--bg-sidebar)', borderRadius: 'var(--radius)', padding: '14px 18px' }}>
            <pre style={{ color: '#c8d3e0', fontFamily: 'var(--font-mono)', fontSize: '0.8rem', lineHeight: 1.8, margin: 0, whiteSpace: 'pre-wrap' }}>
{`# 1. Install backend dependencies
cd backend
pip install -r requirements.txt

# 2. Start the backend
python main.py

# 3. Install and start frontend
cd ../frontend
npm install
npm start

# 4. Open browser
http://localhost:3000

# 5. Power on ESP32-S3, then go to
#    Device Status → Connect`}
            </pre>
          </div>
        </div>

      </div>
    </div>
  );
}
