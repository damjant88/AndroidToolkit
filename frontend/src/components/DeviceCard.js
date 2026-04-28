import React, { useState } from 'react';
import {
  rebootDevice, uninstallApp, enableFirebaseDebug, toggleWifiDebug,
  pullLogs, takeScreenshot, downloadLogs
} from '../api/deviceApi';
import { getIconForPackage } from '../api/packageIcons';

function DeviceCard({ device, onRefresh, onOpenPermissions }) {
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(false);
  const [screenshotUrl, setScreenshotUrl] = useState(null);

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

  async function handleScreenshot() {
    setLoading(true);
    setMessage('Capturing screenshot...');
    try {
      await takeScreenshot(serial, device.deviceName);
      // Add timestamp to force browser to reload the image
      setScreenshotUrl(`/api/files/screenshot/${device.deviceName}?t=${Date.now()}`);
      setMessage('Screenshot captured');
    } catch (err) {
      setMessage('Error: ' + (err.response?.data?.message || err.message));
    } finally {
      setLoading(false);
    }
  }

  async function handlePullLogs() {
    setLoading(true);
    setMessage('Pulling logs...');
    try {
      await pullLogs(serial);
      setMessage('Logs pulled. Downloading zip...');
      const blob = await downloadLogs(device.deviceName);
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `logs_${device.deviceName}.zip`;
      link.click();
      window.URL.revokeObjectURL(url);
      setMessage('✅ Logs downloaded');
    } catch (err) {
      setMessage('Error: ' + (err.response?.data?.message || err.message));
    } finally {
      setLoading(false);
    }
  }

  async function handleCopyScreenshot() {
    if (!screenshotUrl) return;
    try {
      const response = await fetch(screenshotUrl);
      const blob = await response.blob();
      await navigator.clipboard.write([
        new ClipboardItem({ 'image/png': blob })
      ]);
      setMessage('Screenshot copied to clipboard');
    } catch (err) {
      setMessage('Copy failed: ' + err.message);
    }
  }

  async function handleDownloadScreenshot() {
    if (!screenshotUrl) return;
    try {
      const response = await fetch(screenshotUrl);
      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `screenshot_${device.deviceName}.png`;
      link.click();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      setMessage('Download failed: ' + err.message);
    }
  }

  return (
    <div className="device-card">
      <div className="device-card-header">
        <h3>{device.deviceName}</h3>
        <img
          className="device-icon"
          src={`/icons/${getIconForPackage(info.safePathPackage)}`}
          alt="app icon"
        />
      </div>
      <div className="device-info">
        <p><strong>Serial:</strong> {serial}</p>
        <p><strong>Model:</strong> {info.manufacturer} {info.model}</p>
        <p><strong>OS Version:</strong> Android {info.osVersion}</p>
        <p><strong>IP:</strong> {info.ipAddress || 'N/A'}</p>
        <p><strong>Package:</strong> {info.safePathPackage || 'Not installed'}</p>
        <p><strong>Status:</strong> {info.appInstalled ? '✅ Installed' : '❌ Not installed'}</p>
      </div>

      <div className="device-actions">
        {info.appInstalled && (
          <>
            <button disabled={loading} onClick={handlePullLogs}>
              Pull SP Logs
            </button>

            <button
              disabled={loading}
              onClick={() => handleAction(() => enableFirebaseDebug(serial, info.safePathPackage))}
            >
              Firebase Debug
            </button>

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
              onClick={() => onOpenPermissions && onOpenPermissions(serial, info.safePathPackage)}
            >
              Permissions
            </button>
          </>
        )}

        <button disabled={loading} onClick={handleScreenshot}>
          Screenshot
        </button>

        <button
          disabled={loading}
          onClick={() => handleAction(
            () => toggleWifiDebug(serial, info.ipAddress, info.wifiDebugSession, !!info.wifiIp)
          )}
        >
          {info.wifiDebugSession ? 'Disable WiFi' : 'WiFi Debug'}
        </button>

        <button
          disabled={loading}
          onClick={() => handleAction(() => rebootDevice(serial), 'Are you sure you want to reboot?')}
        >
          Reboot
        </button>
      </div>

      {message && <p className="device-message" style={{whiteSpace: 'pre-line'}}>{message}</p>}

      {screenshotUrl && (
        <div className="screenshot-preview">
          <img src={screenshotUrl} alt="device screenshot" />
          <div className="screenshot-actions">
            <button onClick={handleCopyScreenshot}>📋 Copy to Clipboard</button>
            <button onClick={handleDownloadScreenshot}>💾 Download</button>
          </div>
        </div>
      )}
    </div>
  );
}

export default DeviceCard;
