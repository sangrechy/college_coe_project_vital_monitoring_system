/**
 * useWearable – custom hook for WebSocket + REST integration.
 * Connects to the FastAPI backend, receives live BLE data,
 * and exposes the latest snapshot to all pages.
 */

import { useState, useEffect, useRef, useCallback } from 'react';

const WS_URL  = 'ws://localhost:8000/ws';
const API_URL = 'http://localhost:8000';

const PING_INTERVAL = 20_000;  // 20 s
const MAX_ECG_BUFFER = 200;    // points kept in memory for live ECG graph

const DEFAULT_STATE = {
  bleConnected: false,
  bleDeviceName: null,
  bleDeviceAddress: null,
  packetRate: 0,
  signalQuality: 'Unknown',
  uptime: 0,
  dbCount: 0,
  // latest readings
  raw: { ecg: 0, temp: 0, ir: 0, red: 0, ax: 0, ay: 0, az: 0 },
  features: { ecg_filt: 0, rr: 800, hr: 0, hrv: 0, motion: 0 },
  ai: { ecg_class: 0, stress_class: 0, status: 0 },
  // rolling ECG buffers
  ecgRawBuffer:  [],
  ecgFiltBuffer: [],
};

export function useWearable() {
  const [state, setState] = useState(DEFAULT_STATE);
  const [wsStatus, setWsStatus] = useState('disconnected'); // connecting | connected | disconnected
  const wsRef   = useRef(null);
  const pingRef = useRef(null);

  /* ── helpers ─────────────────────────────────────────────── */

  const pushECG = useCallback((ecgRaw, ecgFilt, ts) => {
    setState(prev => {
      const point = { t: ts || Date.now(), raw: ecgRaw, filt: ecgFilt };
      const rawBuf  = [...prev.ecgRawBuffer,  { t: point.t, v: ecgRaw  }].slice(-MAX_ECG_BUFFER);
      const filtBuf = [...prev.ecgFiltBuffer, { t: point.t, v: ecgFilt }].slice(-MAX_ECG_BUFFER);
      return { ...prev, ecgRawBuffer: rawBuf, ecgFiltBuffer: filtBuf };
    });
  }, []);

  const processMessage = useCallback((msg) => {
    try {
      const payload = JSON.parse(msg);
      if (payload.type === 'pong') return;

      if (payload.type === 'live_data' || payload.data) {
        const d = payload.data || {};
        setState(prev => ({
          ...prev,
          bleConnected:   payload.ble_connected  ?? prev.bleConnected,
          packetRate:     payload.packet_rate     ?? prev.packetRate,
          signalQuality:  payload.signal_quality  ?? prev.signalQuality,
          raw:      d.raw      || prev.raw,
          features: d.features || prev.features,
          ai:       d.ai       || prev.ai,
        }));
        if (d.raw && d.features) {
          pushECG(d.raw.ecg, d.features.ecg_filt);
        }
      }

      if (payload.type === 'status') {
        setState(prev => ({
          ...prev,
          bleConnected: payload.ble_connected ?? prev.bleConnected,
          packetRate:   payload.packet_rate   ?? prev.packetRate,
        }));
      }
    } catch (e) {
      // ignore malformed messages
    }
  }, [pushECG]);

  /* ── WebSocket lifecycle ─────────────────────────────────── */

  const connect = useCallback(() => {
    if (wsRef.current && wsRef.current.readyState < 2) return;

    setWsStatus('connecting');
    const ws = new WebSocket(WS_URL);
    wsRef.current = ws;

    ws.onopen = () => {
      setWsStatus('connected');
      // heartbeat ping
      pingRef.current = setInterval(() => {
        if (ws.readyState === WebSocket.OPEN) ws.send('ping');
      }, PING_INTERVAL);
    };

    ws.onmessage = (e) => processMessage(e.data);

    ws.onclose = () => {
      setWsStatus('disconnected');
      clearInterval(pingRef.current);
      // auto-reconnect after 3 s
      setTimeout(connect, 3000);
    };

    ws.onerror = () => {
      ws.close();
    };
  }, [processMessage]);

  useEffect(() => {
    connect();
    return () => {
      clearInterval(pingRef.current);
      wsRef.current?.close();
    };
  }, [connect]);

  /* ── Fetch system status periodically ───────────────────── */
  useEffect(() => {
    const poll = async () => {
      try {
        const res = await fetch(`${API_URL}/api/status`);
        const data = await res.json();
        setState(prev => ({
          ...prev,
          bleConnected:     data.ble_connected,
          bleDeviceName:    data.ble_device_name,
          bleDeviceAddress: data.ble_device_address,
          packetRate:       data.packet_rate,
          uptime:           data.uptime_seconds,
          dbCount:          data.db_record_count,
        }));
      } catch { /* backend not up yet */ }
    };
    poll();
    const id = setInterval(poll, 5000);
    return () => clearInterval(id);
  }, []);

  /* ── BLE Control ─────────────────────────────────────────── */

  const scanDevices = useCallback(async () => {
    const res = await fetch(`${API_URL}/api/ble/scan`, { method: 'POST' });
    return res.json();
  }, []);

  const connectBLE = useCallback(async (address = null) => {
    const url = address
      ? `${API_URL}/api/ble/connect?address=${encodeURIComponent(address)}`
      : `${API_URL}/api/ble/connect`;
    const res = await fetch(url, { method: 'POST' });
    return res.json();
  }, []);

  const disconnectBLE = useCallback(async () => {
    const res = await fetch(`${API_URL}/api/ble/disconnect`, { method: 'POST' });
    return res.json();
  }, []);

  const clearHistory = useCallback(async () => {
    const res = await fetch(`${API_URL}/api/history`, { method: 'DELETE' });
    return res.json();
  }, []);

  return {
    ...state,
    wsStatus,
    scanDevices,
    connectBLE,
    disconnectBLE,
    clearHistory,
    API_URL,
  };
}
