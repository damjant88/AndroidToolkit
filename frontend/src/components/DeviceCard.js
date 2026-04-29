import React, { useState } from 'react';
import {
  rebootDevice, uninstallApp, enableFirebaseDebug, toggleWifiDebug,
  pullLogs, takeScreenshot, startScreenMirror, startRecording, stopRecording, openFolder,
  getPermissions, enablePermissions, disablePermissions
} from '../api/deviceApi';
import { getIconForPackage, getLabelForPackage } from '../api/packageIcons';

function DeviceCard({ device, selected, onToggleSelect, onRefresh }) {
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(false);
  const [recording, setRecording] = useState(false);
  const [permissionsOpen, setPermissionsOpen] = useState(false);
  const [permissions, setPermissions] = useState(null);
  const [selectedPermIds, setSelectedPermIds] = useState([]);
  const [permLoading, setPermLoading] = useState(false);

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
      const url = `/api/files/screenshot/${device.deviceName}?t=${Date.now()}`;
      const viewerUrl = `/?view=screenshot&url=${encodeURIComponent(url)}&device=${encodeURIComponent(device.deviceName)}`;
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
      const folder = result.exportedLogsFolder || result.selectedFolder;
      if (folder) {
        const openResult = await openFolder(folder);
        if (!openResult.success) setMessage('✅ Logs saved\n⚠️ ' + openResult.message);
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
          const openResult = await openFolder(result.recordingLocation);
          if (!openResult.success) setMessage('Recording saved but: ' + openResult.message);
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

  // --- Permissions inline panel ---
  async function handleOpenPermissions() {
    if (permissionsOpen) {
      setPermissionsOpen(false);
      return;
    }
    setPermLoading(true);
    try {
      const state = await getPermissions(serial, info.safePathPackage);
      setPermissions(state);
      setSelectedPermIds(state.activePermissionIds || []);
      setPermissionsOpen(true);
    } catch (err) {
      setMessage('Error loading permissions: ' + (err.response?.data?.message || err.message));
    } finally {
      setPermLoading(false);
    }
  }

  function togglePermSelection(id) {
    setSelectedPermIds(prev =>
      prev.includes(id) ? prev.filter(x => x !== id) : [...prev, id]
    );
  }

  async function handleEnablePerms() {
    setPermLoading(true);
    try {
      const result = await enablePermissions(serial, info.safePathPackage, selectedPermIds);
      const r = result.updateResult;
      setMessage(r.successful ? `✅ Enabled ${r.appliedCount} permissions` : `⚠️ ${r.appliedCount}/${r.requestedCount} enabled`);
      setPermissions(result.dialogState);
      setSelectedPermIds(result.dialogState.activePermissionIds || []);
    } catch (err) {
      setMessage('Error: ' + (err.response?.data?.message || err.message));
    } finally {
      setPermLoading(false);
    }
  }

  async function handleDisablePerms() {
    setPermLoading(true);
    try {
      const result = await disablePermissions(serial, info.safePathPackage, selectedPermIds);
      const r = result.updateResult;
      setMessage(r.successful ? `✅ Disabled ${r.appliedCount} permissions` : `⚠️ ${r.appliedCount}/${r.requestedCount} disabled`);
      setPermissions(result.dialogState);
      setSelectedPermIds(result.dialogState.activePermissionIds || []);
    } catch (err) {
      setMessage('Error: ' + (err.response?.data?.message || err.message));
    } finally {
      setPermLoading(false);
    }
  }

  return (
    <div className={`device-card ${selected ? 'device-card-selected' : ''}`}>
      <div className="device-card-header" onClick={onToggleSelect}>
        <label className="device-select-checkbox" onClick={e => e.stopPropagation()}>
          <input type="checkbox" checked={selected} onChange={onToggleSelect} />
          <h3>{device.deviceName}</h3>
        </label>
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
      </div>

      <div className="device-actions">
        {info.appInstalled && (
          <>
            <button disabled={loading} onClick={handlePullLogs}>Pull SP Logs</button>
            <button disabled={loading} onClick={() => handleAction(() => enableFirebaseDebug(serial, info.safePathPackage))}>Firebase Debug</button>
            <button disabled={loading} onClick={() => handleAction(() => uninstallApp(serial, info.safePathPackage), 'Are you sure you want to uninstall?')}>Uninstall</button>
            <button disabled={loading || permLoading} onClick={handleOpenPermissions}>{permissionsOpen ? '▼ Permissions' : '▶ Permissions'}</button>
          </>
        )}
        <button disabled={loading} onClick={handleScreenshot}>Screenshot</button>
        <button disabled={loading} onClick={handleScreenMirror}>Screen Mirror</button>
        <button disabled={loading} onClick={handleRecording} className={recording ? 'recording-active' : ''}>{recording ? '⏹ Stop Record' : '⏺ Start Record'}</button>
        <button disabled={loading} onClick={() => handleAction(() => toggleWifiDebug(serial, info.ipAddress, info.wifiDebugSession, !!info.wifiIp))}>{info.wifiDebugSession ? 'Disable WiFi' : 'WiFi Debug'}</button>
        <button disabled={loading} onClick={() => handleAction(() => rebootDevice(serial), 'Are you sure you want to reboot?')}>Reboot</button>
      </div>

      {/* Inline permissions panel */}
      {permissionsOpen && permissions && (
        <div className="permissions-inline">
          <div className="permissions-list">
            {(permissions.definitions || []).map(def => {
              const isUnavailable = (permissions.unavailablePermissionIds || []).includes(def.id);
              const isActive = (permissions.activePermissionIds || []).includes(def.id);
              const isSelected = selectedPermIds.includes(def.id);
              return (
                <div key={def.id} className={`permission-row ${isUnavailable ? 'unavailable' : ''}`}>
                  <label>
                    <input type="checkbox" checked={isSelected} disabled={isUnavailable || permLoading} onChange={() => togglePermSelection(def.id)} />
                    {def.label}
                  </label>
                  <span className={`permission-status ${isUnavailable ? 'status-unavailable' : isActive ? 'status-enabled' : 'status-disabled'}`}>
                    {isUnavailable ? 'N/A' : isActive ? 'On' : 'Off'}
                  </span>
                </div>
              );
            })}
          </div>
          <div className="permissions-inline-actions">
            <button onClick={handleEnablePerms} disabled={permLoading || selectedPermIds.length === 0}>Enable</button>
            <button onClick={handleDisablePerms} disabled={permLoading || selectedPermIds.length === 0}>Disable</button>
          </div>
        </div>
      )}

      {message && <p className="device-message" style={{whiteSpace: 'pre-line'}}>{message}</p>}
    </div>
  );
}

export default DeviceCard;
