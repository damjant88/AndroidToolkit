import React, { useState } from 'react';
import { rebootDevice, uninstallApp, enableFirebaseDebug, toggleWifiDebug } from '../api/deviceApi';

function DeviceCard({ device, onRefresh }) {
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(false);

  const info = device.deviceInfo;
  const serial = info.serialNumber;

  async function handleAction(actionFn, confirmMessage) {
    if (confirmMessage && !window.confirm(confirmMessage)) return;
    setLoading(true);
    setMessage('');
    try {
      const result = await actionFn();
      setMessage(result.message || 'Done');
      if (onRefresh) onRefresh();
    } catch (err) {
      setMessage('Error: ' + (err.response?.data?.message || err.message));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="device-card">
      <h3>{device.deviceName}</h3>
      <div className="device-info">
        <p><strong>Serial:</strong> {serial}</p>
        <p><strong>Model:</strong> {info.manufacturer} {info.model}</p>
        <p><strong>Android:</strong> {info.osVersion}</p>
        <p><strong>IP:</strong> {info.ipAddress || 'N/A'}</p>
        <p><strong>Package:</strong> {info.safePathPackage || 'Not installed'}</p>
        <p><strong>Status:</strong> {info.appInstalled ? '✅ Installed' : '❌ Not installed'}</p>
      </div>

      <div className="device-actions">
        <button
          disabled={loading}
          onClick={() => handleAction(() => rebootDevice(serial), 'Are you sure you want to reboot?')}
        >
          Reboot
        </button>

        {info.appInstalled && (
          <>
            <button
              disabled={loading}
              onClick={() => handleAction(
                () => uninstallApp(serial, info.safePathPackage),
                'Are you sure you want to uninstall?'
              )}
            >
              Uninstall
            </button>

            <button
              disabled={loading}
              onClick={() => handleAction(() => enableFirebaseDebug(serial, info.safePathPackage))}
            >
              Firebase Debug
            </button>
          </>
        )}

        <button
          disabled={loading}
          onClick={() => handleAction(
            () => toggleWifiDebug(serial, info.ipAddress, info.wifiDebugSession, info.hasWifiIp)
          )}
        >
          {info.wifiDebugSession ? 'Disable WiFi' : 'WiFi Debug'}
        </button>
      </div>

      {message && <p className="device-message">{message}</p>}
    </div>
  );
}

export default DeviceCard;
