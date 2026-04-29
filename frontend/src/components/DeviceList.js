import React, { useState, useEffect, useRef } from 'react';
import { getDevices } from '../api/deviceApi';
import DeviceCard from './DeviceCard';
import InstallPanel from './InstallPanel';
import PermissionsDialog from './PermissionsDialog';

function DeviceList() {
  const [devices, setDevices] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [permissionsTarget, setPermissionsTarget] = useState(null);
  // Track which devices are selected for install (by serial)
  const [selectedSerials, setSelectedSerials] = useState(new Set());
  const initialLoadDone = useRef(false);

  async function fetchDevices() {
    setLoading(true);
    setError('');
    try {
      const result = await getDevices();
      const fetched = result.devices || [];
      setDevices(fetched);

      const currentSerials = new Set(fetched.map(d => d.serial));

      if (!initialLoadDone.current) {
        // First load — select all devices
        initialLoadDone.current = true;
        setSelectedSerials(currentSerials);
      } else {
        // Subsequent refreshes — only remove disconnected devices
        setSelectedSerials(prev => {
          const updated = new Set(prev);
          for (const s of updated) {
            if (!currentSerials.has(s)) updated.delete(s);
          }
          return updated;
        });
      }
    } catch (err) {
      setError('Failed to connect to backend: ' + err.message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    fetchDevices();
    const interval = setInterval(fetchDevices, 5000);
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

  function openPermissions(serial, packageName, deviceName) {
    setPermissionsTarget({ serial, packageName, deviceName });
  }

  function closePermissions() {
    setPermissionsTarget(null);
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
            key={device.deviceInfo.serialNumber || device.serial || device.index}
            device={device}
            selected={selectedSerials.has(device.serial)}
            onToggleSelect={() => toggleDeviceSelection(device.serial)}
            onRefresh={fetchDevices}
            onOpenPermissions={(serial, packageName) => openPermissions(serial, packageName, device.deviceName)}
          />
        ))}
      </div>

      {permissionsTarget && (
        <PermissionsDialog
          serial={permissionsTarget.serial}
          packageName={permissionsTarget.packageName}
          deviceName={permissionsTarget.deviceName}
          onClose={closePermissions}
        />
      )}
    </div>
  );
}

export default DeviceList;
