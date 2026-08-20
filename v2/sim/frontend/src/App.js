import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { useWearable } from './hooks/useWearable';
import Sidebar from './components/shared/Sidebar';
import Overview from './components/pages/Overview';
import ECGMonitor from './components/pages/ECGMonitor';
import VitalSigns from './components/pages/VitalSigns';
import AIResults from './components/pages/AIResults';
import HistoricalTrends from './components/pages/HistoricalTrends';
import DeviceStatus from './components/pages/DeviceStatus';
import Settings from './components/pages/Settings';
import './styles/global.css';

export default function App() {
  const data = useWearable();

  const sharedProps = {
    raw:          data.raw,
    features:     data.features,
    ai:           data.ai,
    ecgRawBuffer:  data.ecgRawBuffer,
    ecgFiltBuffer: data.ecgFiltBuffer,
    bleConnected:  data.bleConnected,
    packetRate:    data.packetRate,
    signalQuality: data.signalQuality,
  };

  return (
    <BrowserRouter>
      <div className="app-shell">
        <Sidebar
          bleConnected={data.bleConnected}
          bleDeviceName={data.bleDeviceName}
          wsStatus={data.wsStatus}
        />
        <main className="main-content">
          <Routes>
            <Route path="/" element={<Overview {...sharedProps} />} />
            <Route path="/ecg" element={<ECGMonitor {...sharedProps} />} />
            <Route path="/vitals" element={<VitalSigns features={data.features} raw={data.raw} />} />
            <Route path="/ai" element={<AIResults ai={data.ai} />} />
            <Route path="/history" element={<HistoricalTrends />} />
            <Route
              path="/device"
              element={
                <DeviceStatus
                  bleConnected={data.bleConnected}
                  bleDeviceName={data.bleDeviceName}
                  bleDeviceAddress={data.bleDeviceAddress}
                  packetRate={data.packetRate}
                  signalQuality={data.signalQuality}
                  uptime={data.uptime}
                  dbCount={data.dbCount}
                  wsStatus={data.wsStatus}
                  scanDevices={data.scanDevices}
                  connectBLE={data.connectBLE}
                  disconnectBLE={data.disconnectBLE}
                />
              }
            />
            <Route
              path="/settings"
              element={
                <Settings
                  clearHistory={data.clearHistory}
                  dbCount={data.dbCount}
                />
              }
            />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  );
}
