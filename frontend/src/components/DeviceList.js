import React, { useState, useEffect, useRef } from 'react';
import { getDevices } from '../api/deviceApi';
import { useDeviceWebSocket } from '../api/useDeviceWebSocket';
import DeviceCard from './DeviceCard';
import InstallPanel from './InstallPanel';


function DeviceList() {
  const [devices, setDevices] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [selectedSerials, setSelectedSerials] = useState(new Set());
  const initialLoadDone = useRef(false);

  // WebSocket: receive real-time device updates from the server
  const deviceUpdate = useDeviceWebSocket();

  // When WebSocket pushes an update, apply it
  useEffect(() => {
    if (deviceUpdate && deviceUpdate.devices) {
      applyDeviceUpdate(deviceUpdate.devices);
    }
  }, [deviceUpdate]);

  function applyDeviceUpdate(fetched) {
    setDevices(fetched);
    const currentSerials = new Set(fetched.map(d => d.serial));

    if (!initialLoadDone.current) {
      initialLoadDone.current = true;
      setSelectedSerials(currentSerials);
    } else {
      setSelectedSerials(prev => {
        const updated = new Set(prev);
        for (const s of updated) {
          if (!currentSerials.has(s)) updated.delete(s);
        }
        return updated;
      });
    }
    setLoading(false);
  }

  // Initial HTTP fetch (in case WebSocket hasn't connected yet)
  async function fetchDevices() {
    setError('');
    try {
      const result = await getDevices();
      const fetched = result.devices || [];
      applyDeviceUpdate(fetched);
    } catch (err) {
      setError('Failed to connect to backend: ' + err.message);
      setLoading(false);
    }
  }

  useEffect(() => {
    fetchDevices();
    // Fallback polling every 30s in case WebSocket disconnects
    const interval = setInterval(fetchDevices, 30000);
    return () => clearInterval(interval);
  }, []);

  function toggleDeviceSelection(serial) {
    setSelectedSerials(prev => {
      const updated = new Set(prev);
      if (updated.has(serial)) {
        updated.delete(serial);
      } else {
        updated.add(serial);
      }
      return updated;
    });
  }

  function selectAllDevices() {
    setSelectedSerials(new Set(devices.map(d => d.serial)));
  }

  function deselectAllDevices() {
    setSelectedSerials(new Set());
  }









  if (loading && devices.length === 0) {
    return <p className="status">Loading devices...</p>;
  }

  if (error) {
    return <p className="status error">{error}</p>;
  }

  if (devices.length === 0) {
    return <p className="status">No devices connected. Connect a device via USB and wait.</p>;
  }

  const selectedDevices = devices.filter(d => selectedSerials.has(d.serial));

  return (
    <div>
      <InstallPanel devices={devices} selectedDevices={selectedDevices} onRefresh={fetchDevices} />
      <div className="toolbar">
        <span>{devices.length} device(s) connected</span>
        <span className="selection-info">
          {selectedSerials.size} selected
        </span>
        <button onClick={selectAllDevices} className="toolbar-small-btn">Select All</button>
        <button onClick={deselectAllDevices} className="toolbar-small-btn">Deselect All</button>
      </div>
      <div className="device-grid">
        {devices.map((device) => (
          <DeviceCard
            key={device.deviceInfo.serialNumber}
            device={device}
            selected={selectedSerials.has(device.serial)}
            onToggleSelect={() => toggleDeviceSelection(device.serial)}
            onRefresh={fetchDevices}

          />
        ))}
      </div>










  );
}

export default DeviceList;
