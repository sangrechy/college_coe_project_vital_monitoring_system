import { useState } from 'react';
import { formatUptime, badgeClass } from '../../utils/labels';

export default function DeviceStatus({
  bleConnected, bleDeviceName, bleDeviceAddress,
  packetRate, signalQuality, uptime, dbCount,
  wsStatus, scanDevices, connectBLE, disconnectBLE
}) {
  const [scanning, setScanning] = useState(false);
  const [connecting, setConnecting] = useState(false);
  const [devices, setDevices] = useState([]);
  const [selectedAddr, setSelectedAddr] = useState('');
  const [message, setMessage] = useState(null);

  const handleScan = async () => {
    setScanning(true);
    setMessage(null);
    try {
      const result = await scanDevices();
      setDevices(result.devices || []);
      if (result.devices?.length === 0) {
        setMessage({ type: 'warn', text: 'No BLE devices found nearby.' });
      }
    } catch {
      setMessage({ type: 'danger', text: 'Scan failed. Is the backend running?' });
    }
    setScanning(false);
  };

  const handleConnect = async () => {
    setConnecting(true);
    setMessage(null);
    try {
      const result = await connectBLE(selectedAddr || null);
      if (result.connected) {
        setMessage({ type: 'ok', text: 'Connected to device successfully.' });
      } else {
        setMessage({ type: 'danger', text: 'Connection failed. Make sure the device is powered on and in range.' });
      }
    } catch {
      setMessage({ type: 'danger', text: 'Connect request failed.' });
    }
    setConnecting(false);
  };

  const handleDisconnect = async () => {
    try {
      await disconnectBLE();
      setMessage({ type: 'info', text: 'Disconnected.' });
    } catch {}
  };

  const wsLabel = { connected: 'Connected', connecting: 'Connecting', disconnected: 'Disconnected' }[wsStatus] || wsStatus;
  const wsBadge = { connected: 'ok', connecting: 'warn', disconnected: 'danger' }[wsStatus] || 'neutral';

  return (
    <div>
      <div className="page-header">
        <div>
          <h2>Device Status</h2>
          <p>BLE connection management and system diagnostics</p>
        </div>
        <div className="header-right">
          <span className={badgeClass(bleConnected ? 'ok' : 'danger')}>
            BLE {bleConnected ? 'Connected' : 'Disconnected'}
          </span>
        </div>
      </div>

      <div className="page-body">

        {message && (
          <div className={`alert alert-${message.type}`} style={{ marginBottom: 16 }}>
            {message.text}
          </div>
        )}

        {/* Connection status card */}
        <div className="grid-2" style={{ marginBottom: 20 }}>
          <div className="card">
            <div className="card-header">
              <div className="card-title">BLE Connection</div>
              <span className={badgeClass(bleConnected ? 'ok' : 'danger')}>
                {bleConnected ? 'Active' : 'Inactive'}
              </span>
            </div>
            <div>
              <div className="status-row">
                <span className="status-label">Status</span>
                <span className="status-value">{bleConnected ? 'Connected' : 'Disconnected'}</span>
              </div>
              <div className="status-row">
                <span className="status-label">Device Name</span>
                <span className="status-value mono">{bleDeviceName || '—'}</span>
              </div>
              <div className="status-row">
                <span className="status-label">MAC Address</span>
                <span className="status-value mono">{bleDeviceAddress || '—'}</span>
              </div>
              <div className="status-row">
                <span className="status-label">Service UUID</span>
                <span className="status-value mono" style={{ fontSize: '0.72rem' }}>…789001</span>
              </div>
              <div className="status-row">
                <span className="status-label">Characteristic UUID</span>
                <span className="status-value mono" style={{ fontSize: '0.72rem' }}>…789002</span>
              </div>
              <div className="status-row">
                <span className="status-label">Update Rate</span>
                <span className="status-value mono">~100 ms (10 Hz)</span>
              </div>
            </div>
            <div style={{ display: 'flex', gap: 8, marginTop: 16 }}>
              {!bleConnected ? (
                <button className="btn btn-primary" onClick={handleConnect} disabled={connecting}>
                  {connecting ? 'Connecting…' : '⊙ Connect'}
                </button>
              ) : (
                <button className="btn btn-outline" onClick={handleDisconnect}>
                  Disconnect
                </button>
              )}
            </div>
          </div>

          <div className="card">
            <div className="card-header">
              <div className="card-title">System Diagnostics</div>
            </div>
            <div>
              <div className="status-row">
                <span className="status-label">WebSocket</span>
                <span className={badgeClass(wsBadge)}>{wsLabel}</span>
              </div>
              <div className="status-row">
                <span className="status-label">Packet Rate</span>
                <span className="status-value mono">{packetRate} Hz</span>
              </div>
              <div className="status-row">
                <span className="status-label">Signal Quality</span>
                <span className="status-value">{signalQuality}</span>
              </div>
              <div className="status-row">
                <span className="status-label">Backend Uptime</span>
                <span className="status-value mono">{formatUptime(uptime)}</span>
              </div>
              <div className="status-row">
                <span className="status-label">Records in DB</span>
                <span className="status-value mono">{dbCount?.toLocaleString() ?? '—'}</span>
              </div>
              <div className="status-row">
                <span className="status-label">Storage</span>
                <span className="status-value mono">SQLite (local)</span>
              </div>
            </div>
          </div>
        </div>

        {/* BLE Scanner */}
        <div className="card" style={{ marginBottom: 16 }}>
          <div className="card-header">
            <div className="card-title">BLE Device Scanner</div>
            <div className="card-subtitle">Scan for nearby BLE devices and connect by address</div>
          </div>
          <div style={{ display: 'flex', gap: 10, marginBottom: 16, flexWrap: 'wrap' }}>
            <button className="btn btn-outline" onClick={handleScan} disabled={scanning}>
              {scanning ? 'Scanning (10s)…' : '⊙ Scan for Devices'}
            </button>
            {devices.length > 0 && (
              <>
                <select
                  className="form-select"
                  style={{ maxWidth: 320 }}
                  value={selectedAddr}
                  onChange={e => setSelectedAddr(e.target.value)}
                >
                  <option value="">— select device —</option>
                  {devices.map(d => (
                    <option key={d.address} value={d.address}>
                      {d.name || 'Unknown'} · {d.address}
                      {d.rssi != null ? ` · RSSI ${d.rssi} dBm` : ''}
                    </option>
                  ))}
                </select>
                <button
                  className="btn btn-primary"
                  onClick={handleConnect}
                  disabled={connecting}
                >
                  {connecting ? 'Connecting…' : 'Connect to Selected'}
                </button>
              </>
            )}
          </div>
          {devices.length > 0 && (
            <div style={{ fontSize: '0.82rem' }}>
              <div style={{ fontWeight: 600, color: 'var(--text-secondary)', marginBottom: 6 }}>
                {devices.length} device{devices.length !== 1 ? 's' : ''} found:
              </div>
              {devices.map(d => (
                <div key={d.address} className="status-row" style={{ fontFamily: 'var(--font-mono)', fontSize: '0.78rem' }}>
                  <span>{d.name || 'Unknown Device'}</span>
                  <span style={{ color: 'var(--text-muted)' }}>{d.address}</span>
                  {d.rssi != null && <span style={{ color: 'var(--text-muted)' }}>{d.rssi} dBm</span>}
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Quick reference */}
        <div className="card">
          <div className="card-header">
            <div className="card-title">ESP32 Configuration Reference</div>
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: '0 24px', fontSize: '0.82rem' }}>
            {[
              ['Device Name',      'EdgeAI_Wearable'],
              ['Service UUID',     '12345678-1234-1234-1234-123456789001'],
              ['Char UUID',        '12345678-1234-1234-1234-123456789002'],
              ['Comm Method',      'BLE Notifications'],
              ['Update Rate',      '100 ms (10 Hz)'],
              ['SDA Pin',          'GPIO 8'],
              ['SCL Pin',          'GPIO 9'],
              ['ECG Pin',          'GPIO 4 (ADC)'],
              ['ECG Filter Size',  '8 samples (moving avg)'],
              ['ECG Threshold',    '1800 ADC counts'],
              ['Refractory',       '300 ms'],
              ['MTU',              '247 bytes'],
            ].map(([k, v]) => (
              <div key={k} className="status-row">
                <span className="status-label">{k}</span>
                <span className="status-value mono" style={{ fontSize: '0.78rem' }}>{v}</span>
              </div>
            ))}
          </div>
        </div>

      </div>
    </div>
  );
}
