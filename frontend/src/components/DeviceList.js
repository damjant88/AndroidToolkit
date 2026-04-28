import React, { useState, useEffect } from 'react';
import { getDevices } from '../api/deviceApi';
import DeviceCard from './DeviceCard';
import InstallPanel from './InstallPanel';
import PermissionsDialog from './PermissionsDialog';

function DeviceList() {
  const [devices, setDevices] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [permissionsTarget, setPermissionsTarget] = useState(null);

  async function fetchDevices() {
    setLoading(true);
    setError('');
    try {
      const result = await getDevices();
      setDevices(result.devices || []);
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

  return (
    <div>
      <InstallPanel devices={devices} onRefresh={fetchDevices} />
      <div className="toolbar">
        <button onClick={fetchDevices}>🔄 Refresh Devices</button>
        <span>{devices.length} device(s) connected</span>
      </div>
      <div className="device-grid">
        {devices.map((device) => (
          <DeviceCard
            key={device.serial}
            device={device}
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
