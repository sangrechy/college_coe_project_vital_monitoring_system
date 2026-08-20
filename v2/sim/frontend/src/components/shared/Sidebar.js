import { useNavigate, useLocation } from 'react-router-dom';

const NAV_ITEMS = [
  {
    section: 'Monitoring',
    items: [
      { path: '/',         icon: '⊞', label: 'Overview' },
      { path: '/ecg',      icon: '〜', label: 'ECG Monitor' },
      { path: '/vitals',   icon: '♥', label: 'Vital Signs' },
      { path: '/ai',       icon: '◈', label: 'AI Results' },
    ]
  },
  {
    section: 'Data',
    items: [
      { path: '/history',  icon: '▦', label: 'Historical Trends' },
    ]
  },
  {
    section: 'System',
    items: [
      { path: '/device',   icon: '⊙', label: 'Device Status' },
      { path: '/settings', icon: '⚙', label: 'Settings' },
    ]
  }
];

export default function Sidebar({ bleConnected, bleDeviceName, wsStatus }) {
  const navigate  = useNavigate();
  const location  = useLocation();

  const dotClass = bleConnected
    ? 'ble-status-dot connected'
    : wsStatus === 'connecting'
    ? 'ble-status-dot connecting'
    : 'ble-status-dot disconnected';

  const dotLabel = bleConnected
    ? `Connected${bleDeviceName ? ` · ${bleDeviceName}` : ''}`
    : 'Not connected';

  return (
    <aside className="sidebar">
      <div className="sidebar-brand">
        <h1>Health Monitor</h1>
        <span>Edge AI Wearable</span>
      </div>

      <nav className="sidebar-nav">
        {NAV_ITEMS.map(section => (
          <div key={section.section}>
            <div className="nav-section-label">{section.section}</div>
            {section.items.map(item => (
              <button
                key={item.path}
                className={`nav-item${location.pathname === item.path ? ' active' : ''}`}
                onClick={() => navigate(item.path)}
              >
                <span className="nav-icon">{item.icon}</span>
                {item.label}
              </button>
            ))}
          </div>
        ))}
      </nav>

      <div className="sidebar-footer">
        <div style={{ display: 'flex', alignItems: 'center' }}>
          <span className={dotClass} />
          <span className="ble-status-text">{dotLabel}</span>
        </div>
      </div>
    </aside>
  );
}
