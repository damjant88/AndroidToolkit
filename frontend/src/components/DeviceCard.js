import React, { useState, useEffect } from 'react';
import {
  rebootDevice, uninstallApp, enableFirebaseDebug, toggleWifiDebug,
  pullLogs, takeScreenshot, startScreenMirror, startRecording, stopRecording, openFolder, setMockLocation, getDeviceLocation
} from '../api/deviceApi';
import { getIconForPackage, getLabelForPackage } from '../api/packageIcons';
import { useLogcatWebSocket } from '../api/useLogcatWebSocket';
import MockLocationMap from './MockLocationMap';
import EventTracker from './EventTracker';

function DeviceCard({ device, selected, onToggleSelect, onRefresh, onOpenPermissions, tier }) {
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(false);
  const [recording, setRecording] = useState(false);
  const [showMap, setShowMap] = useState(false);
  const [mockAddress, setMockAddress] = useState('');
  const [mocking, setMocking] = useState(false);
  const [showEventTracker, setShowEventTracker] = useState(false);

  const isLocal = window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1';

  const info = device.deviceInfo;
  const serial = info.serialNumber;
  const { logcatData } = useLogcatWebSocket(serial, info.appInstalled);

  // Clear message when a new app is installed
  useEffect(() => {
    if (info.appInstalled) {
      setMessage('');
    }
  }, [info.appInstalled]);

  useEffect(() => {
    getDeviceLocation(serial)
      .then(data => {
        if (data.lat && data.lng && data.found) {
          fetch(`https://nominatim.openstreetmap.org/reverse?lat=${data.lat}&lon=${data.lng}&format=json&addressdetails=1`)
            .then(r => r.json())
            .then(geo => {
              const a = geo.address || {};
              const parts = [a.road, a.city || a.town || a.village, a.state].filter(Boolean);
              setMockAddress(parts.join(', ') || geo.display_name || '');
            })
            .catch(() => {});
        }
      })
      .catch(() => {});
  }, [serial]); // eslint-disable-line react-hooks/exhaustive-deps

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
      const result = await takeScreenshot(serial, device.deviceName);
      // Create a blob URL from the image data returned directly by the agent
      const blobUrl = URL.createObjectURL(result.imageBlob);
      const viewerUrl = `/?view=screenshot&url=${encodeURIComponent(blobUrl)}&device=${encodeURIComponent(device.deviceName)}`;
      const w = 420;
      const h = 800;
      const left = window.screenX + Math.round((window.outerWidth - w) / 2);
      const top = window.screenY + Math.round((window.outerHeight - h) / 2);
      window.open(viewerUrl, `screenshot_${device.deviceName}`, `width=${w},height=${h},left=${left},top=${top},resizable=yes`);
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
      const result = await pullLogs(serial);
      setMessage('✅ Logs saved');
      if (isLocal) {
        const folder = result.exportedLogsFolder || result.selectedFolder;
        if (folder) {
          const openResult = await openFolder(folder);
          if (!openResult.success) setMessage('✅ Logs saved\n⚠️ ' + openResult.message);
        }
      }
    } catch (err) {
      setMessage('Error: ' + (err.response?.data?.message || err.message));
    } finally {
      setLoading(false);
    }
  }

  async function handleScreenMirror() {
    setLoading(true);
    setMessage('');
    try {
      const result = await startScreenMirror(serial);
      setMessage(result.message);
    } catch (err) {
      setMessage('Error: ' + (err.response?.data?.message || err.message));
    } finally {
      setLoading(false);
    }
  }

  async function handleRecording() {
    setLoading(true);
    setMessage('');
    try {
      if (!recording) {
        const result = await startRecording(serial);
        setMessage(result.message);
        if (result.success) setRecording(true);
      } else {
        setMessage('⏹ Stopping recording...');
        const result = await stopRecording(serial, info.pid || '');
        setRecording(false);
        if (result.success && result.recordingLocation) {
          setMessage('Recording saved');
          if (isLocal) {
            const openResult = await openFolder(result.recordingLocation);
            if (!openResult.success) setMessage('Recording saved but: ' + openResult.message);
          }
        } else {
          setMessage(result.message || 'No active recording');
        }
      }
    } catch (err) {
      setMessage('Error: ' + (err.response?.data?.message || err.message));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className={`device-card ${selected ? 'device-card-selected' : ''}`}>
      <div className="device-card-header" onClick={onToggleSelect}>
        <label className="device-select-checkbox" onClick={e => e.stopPropagation()}>
          <input type="checkbox" checked={selected} onChange={onToggleSelect} />
          <h3>{device.deviceName}</h3>
        </label>
        {info.appInstalled && logcatData?.tokenType && <h3 className="token-type-header">{logcatData.tokenType === 'godevice' ? (info.safePathPackage === 'com.smithmicro.cci.test' ? 'Senior' : 'Child') : logcatData.tokenType === 'admin' ? 'Adult' : logcatData.tokenType}</h3>}
        <div className="device-icon-group">
          <img className="device-icon" src={`/icons/${getIconForPackage(info.safePathPackage)}`} alt="app icon" />
          <span className="device-icon-label">{getLabelForPackage(info.safePathPackage)}</span>
        </div>
      </div>

      <div className="device-info">
        <p><strong>Serial:</strong> {serial}</p>
        <p><strong>Manufacturer:</strong> {info.manufacturer}</p>
        <p><strong>Model:</strong> {info.model}</p>
        <p><strong>OS Version:</strong> Android {info.osVersion}</p>
        <p><strong>IP:</strong> {info.ipAddress || 'N/A'}</p>
        {mockAddress && <p><strong>Location:</strong> {mockAddress}</p>}
        {info.appInstalled && <>
          <p><strong>Client Version:</strong> {logcatData?.clientVersion || '—'}</p>
          <p><strong>Environment:</strong> {logcatData?.environment || '—'}</p>
          <p><strong>Server Product Version:</strong> {logcatData?.serverProductVersion || '—'}</p>
          <p><strong>Server Project Version:</strong> {logcatData?.serverProjectVersion || '—'}</p>
          <p className="token-row"><strong>Access Token:</strong> {logcatData?.accessToken
            ? <><span className="token-value">{logcatData.accessToken.substring(0, 20)}...</span><button className="copy-token-btn" onClick={() => navigator.clipboard.writeText(logcatData.accessToken)}>Copy Token</button></>
            : '—'
          }</p>
        </>}
      </div>

      <div className="device-actions-grouped">
        <div className="action-group">
          <span className="action-group-label">App</span>
          <div className="action-group-buttons">
            <button disabled={loading || !info.appInstalled} onClick={handlePullLogs}>Pull SP Logs</button>
            <button disabled={loading || !info.appInstalled} onClick={() => handleAction(() => uninstallApp(serial, info.safePathPackage), 'Are you sure you want to uninstall?')}>Uninstall</button>
            <button disabled={loading || !info.appInstalled} onClick={() => handleAction(() => enableFirebaseDebug(serial, info.safePathPackage))}>Firebase Debug</button>
            <button disabled={loading || !info.appInstalled} onClick={() => onOpenPermissions && onOpenPermissions(serial, info.safePathPackage)}>Permissions</button>
            <button disabled={loading || !info.appInstalled} onClick={() => setShowEventTracker(true)}>Event Tracker</button>
          </div>
        </div>
        <div className="action-group">
          <span className="action-group-label">Screen</span>
          <div className="action-group-buttons">
            <button disabled={loading} onClick={handleScreenshot}>Screenshot</button>
            <button disabled={loading} onClick={handleScreenMirror}>Screen Mirror</button>
            <button disabled={loading} onClick={handleRecording} className={recording ? 'recording-active' : ''}>{recording ? '⏹ Stop Record' : '⏺ Start Record'}</button>
          </div>
        </div>
        <div className="action-group">
          <span className="action-group-label">Device</span>
          <div className="action-group-buttons">
            <button disabled={loading || tier === 'BASIC'} onClick={() => handleAction(() => toggleWifiDebug(serial, info.ipAddress, info.wifiDebugSession, !!info.wifiIp))}>{info.wifiDebugSession ? 'Disable WiFi' : 'WiFi Debug'}</button>
            <button disabled={loading} onClick={() => handleAction(() => rebootDevice(serial), 'Are you sure you want to reboot?')}>Reboot</button>
            <button disabled={loading} onClick={() => setShowMap(true)} className={mocking ? 'recording-active' : ''}>{mocking ? '📍 Mocking...' : 'Mock Location'}</button>
          </div>
        </div>
      </div>

      {message && <p className="device-message" style={{whiteSpace: 'pre-line'}}>{message}</p>}

      {showMap && (
        <MockLocationMap
          serial={serial}
          onClose={() => setShowMap(false)}
          onSetLocation={setMockLocation}
          onAddressResolved={(addr) => setMockAddress(addr)}
          mocking={mocking}
          onMockingChange={setMocking}
        />
      )}

      {showEventTracker && (
        <EventTracker serial={serial} onClose={() => setShowEventTracker(false)} />
      )}
    </div>
  );
}

export default DeviceCard;
