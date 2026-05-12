import React, { useState, useEffect, useRef } from 'react';
import { getDevices } from '../api/deviceApi';
import { useDeviceWebSocket } from '../api/useDeviceWebSocket';
import { useAuth } from '../api/AuthContext';
import DeviceCard from './DeviceCard';
import InstallPanel from './InstallPanel';
import PermissionsDialog from './PermissionsDialog';

function DeviceList() {
  const { user } = useAuth();
  const maxDevices = user?.tier === 'BASIC' ? 1 : Infinity;
  const [devices, setDevices] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [selectedSerials, setSelectedSerials] = useState(new Set());
  const [permissionsTarget, setPermissionsTarget] = useState(null);
  const initialLoadDone = useRef(false);
  const knownOrder = useRef([]); // stable serial order

  const { deviceUpdate, connected } = useDeviceWebSocket();

  useEffect(() => {
    if (deviceUpdate && deviceUpdate.devices) {
      applyDeviceUpdate(deviceUpdate.devices);
    }
  }, [deviceUpdate]);

  function applyDeviceUpdate(fetched) {
    const fetchedMap = new Map(fetched.map(d => [d.serial, d]));
    const currentSerials = new Set(fetched.map(d => d.serial));

    // Update stable order: keep existing serials in position, remove disconnected, append new to end
    const prevOrder = knownOrder.current;
    const stableSerials = prevOrder.filter(s => currentSerials.has(s));
    const newSerials = fetched.filter(d => !prevOrder.includes(d.serial)).map(d => d.serial);
    const finalOrder = [...stableSerials, ...newSerials];
    knownOrder.current = finalOrder;

    // Build device list in stable order
    const ordered = finalOrder.map(s => fetchedMap.get(s)).filter(Boolean);
    setDevices(ordered);

    if (!initialLoadDone.current) {
      initialLoadDone.current = true;
      setSelectedSerials(currentSerials);
    } else {
      setSelectedSerials(prev => {
        const updated = new Set(prev);
        // Remove disconnected devices
        for (const s of updated) {
          if (!currentSerials.has(s)) updated.delete(s);
        }
        // Auto-select newly connected devices
        for (const s of newSerials) {
          updated.add(s);
        }
        return updated;
      });
    }
    setLoading(false);
  }

  async function fetchDevices() {
    setError('');
    try {
      const result = await getDevices();
      applyDeviceUpdate(result.devices || []);
    } catch (err) {
      setError('Failed to connect to backend: ' + err.message);
      setLoading(false);
    }
  }

  useEffect(() => {
    fetchDevices();
    const interval = setInterval(fetchDevices, 30000);
    return () => clearInterval(interval);
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  function toggleDeviceSelection(serial) {
    setSelectedSerials(prev => {
      const updated = new Set(prev);
      if (updated.has(serial)) { updated.delete(serial); } else { updated.add(serial); }
      return updated;
    });
  }

  function selectAllDevices() { setSelectedSerials(new Set(devices.map(d => d.serial))); }
  function deselectAllDevices() { setSelectedSerials(new Set()); }

  function openPermissions(serial, packageName, deviceName) {
    setPermissionsTarget({ serial, packageName, deviceName });
  }

  function closePermissions() {
    setPermissionsTarget(null);
  }

  if (loading && devices.length === 0) return <p className="status">Loading devices...</p>;
  if (error) return <p className="status error">{error}</p>;
  if (devices.length === 0) return <p className="status">No devices connected.</p>;

  const visibleDevices = devices.slice(0, maxDevices);
  const selectedDevices = visibleDevices.filter(d => selectedSerials.has(d.serial));

  // For BASIC tier, always show 1 connected / 1 selected
  const displayCount = maxDevices < Infinity ? Math.min(visibleDevices.length, maxDevices) : visibleDevices.length;
  const displaySelected = maxDevices < Infinity ? Math.min(selectedSerials.size, maxDevices) : selectedSerials.size;

  return (
    <div>
      <InstallPanel devices={visibleDevices} selectedDevices={selectedDevices} onRefresh={fetchDevices} />
      <div className="toolbar">
        <span className={`connection-status ${connected || devices.length > 0 ? 'connected' : 'disconnected'}`}>
          {connected || devices.length > 0 ? '🟢' : '🔴'}
        </span>
        <span>{displayCount} device(s) connected</span>
        <span className="selection-info">{displaySelected} selected</span>
        <button onClick={selectAllDevices} className="toolbar-small-btn">Select All</button>
        <button onClick={deselectAllDevices} className="toolbar-small-btn">Deselect All</button>
      </div>
      <div className="device-grid">
        {visibleDevices.map((device) => (
          <DeviceCard
            key={device.deviceInfo.serialNumber}
            device={device}
            selected={selectedSerials.has(device.serial)}
            onToggleSelect={() => toggleDeviceSelection(device.serial)}
            onRefresh={fetchDevices}
            onOpenPermissions={(serial, packageName) => openPermissions(serial, packageName, device.deviceName)}
            tier={user?.tier}
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
