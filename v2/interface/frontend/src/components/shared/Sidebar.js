import { useNavigate, useLocation } from 'react-router-dom';
import Icon from './Icon';

const NAV_ITEMS = [
  {
    section: 'Monitoring',
    items: [
      { path: '/',         icon: 'grid',   label: 'Overview' },
      { path: '/ecg',      icon: 'pulse',  label: 'ECG Monitor' },
      { path: '/vitals',   icon: 'heart',  label: 'Vital Signs' },
      { path: '/ai',       icon: 'chip',   label: 'AI Results' },
    ]
  },
  {
    section: 'Data',
    items: [
      { path: '/history',  icon: 'trend',  label: 'Historical Trends' },
    ]
  },
  {
    section: 'System',
    items: [
      { path: '/device',   icon: 'device', label: 'Device Status' },
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
        <div className="brand-mark"><Icon name="pulse" size={20} stroke={2.2} /></div>
        <div>
          <h1>Health Monitor</h1>
          <span>Edge AI Wearable</span>
        </div>
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
                <span className="nav-icon"><Icon name={item.icon} size={18} /></span>
                {item.label}
              </button>
            ))}
          </div>
        ))}
      </nav>

      <div className="sidebar-footer">
        <div className="sidebar-footer-label">Device link</div>
        <div style={{ display: 'flex', alignItems: 'center' }}>
          <span className={dotClass} />
          <span className="ble-status-text">{dotLabel}</span>
        </div>
      </div>
    </aside>
  );
}
