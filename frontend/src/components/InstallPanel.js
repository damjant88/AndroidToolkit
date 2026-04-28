import React, { useState, useRef } from 'react';
import { installBuild, uploadBuild, uninstallApp } from '../api/deviceApi';

const MAX_HISTORY = 5;

function InstallPanel({ devices, selectedDevices, onRefresh }) {
  const [selectedPath, setSelectedPath] = useState('');
  const [selectedName, setSelectedName] = useState('');
  const [message, setMessage] = useState('');
  const [installing, setInstalling] = useState({});
  const [dragging, setDragging] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [buildHistory, setBuildHistory] = useState(() => {
    const saved = localStorage.getItem('buildHistory');
    return saved ? JSON.parse(saved) : [];
  });
  const fileInputRef = useRef(null);

  function saveHistory(newHistory) {
    setBuildHistory(newHistory);
    localStorage.setItem('buildHistory', JSON.stringify(newHistory));
  }

  function addToHistory(fileName, path) {
    const entry = { fileName, path, timestamp: Date.now() };
    const filtered = buildHistory.filter(h => h.path !== path);
    const updated = [entry, ...filtered].slice(0, MAX_HISTORY);
    saveHistory(updated);
  }

  async function handleFileSelected(file) {
    if (!file || !file.name.endsWith('.apk')) {
      setMessage('❌ Only .apk files are accepted');
      return;
    }
    setSelectedName(file.name);
    setMessage(`Uploading ${file.name}...`);
    setUploading(true);
    try {
      const result = await uploadBuild(file);
      setSelectedPath(result.path);
      setSelectedName(result.fileName);
      addToHistory(result.fileName, result.path);
      setMessage(`✅ Ready to install: ${result.fileName}`);
    } catch (err) {
      setMessage('❌ Upload failed: ' + (err.response?.data?.message || err.message));
    } finally {
      setUploading(false);
    }
  }

  function handleFileInput(e) {
    const file = e.target.files[0];
    if (file) handleFileSelected(file);
  }

  function handleDrop(e) {
    e.preventDefault();
    setDragging(false);
    const file = e.dataTransfer.files[0];
    if (file) handleFileSelected(file);
  }

  function handleDragOver(e) {
    e.preventDefault();
    setDragging(true);
  }

  function handleDragLeave(e) {
    e.preventDefault();
    setDragging(false);
  }


  function handleHistorySelect(entry) {
    setSelectedPath(entry.path);
    setSelectedName(entry.fileName);
    setMessage(`Selected: ${entry.fileName}`);
  }

  async function handleInstall(serial) {
    if (!selectedPath) {
      setMessage('Select or enter an APK path first');
      return;
    }
    setInstalling((prev) => ({ ...prev, [serial]: true }));
    setMessage(`Installing ${selectedName} on ${serial}...`);
    try {
      const result = await installBuild(serial, selectedPath);
      if (result.success) {
        addToHistory(selectedName, selectedPath);
      }
      setMessage(result.success ? '✅ ' + result.message : '❌ ' + result.message);
      if (result.success && onRefresh) onRefresh();
    } catch (err) {
      setMessage('❌ Install failed: ' + (err.response?.data?.message || err.message));
    } finally {
      setInstalling((prev) => ({ ...prev, [serial]: false }));
    }
  }

  async function handleInstallAll() {
    if (!selectedPath) {
      setMessage('Select an APK first');
      return;
    }
    if (selectedDevices.length === 0) {
      setMessage('No devices selected');
      return;
    }
    setMessage(`Installing ${selectedName} on ${selectedDevices.length} device(s)...`);
    for (const device of selectedDevices) {
      await handleInstall(device.serial);
    }
  }

  async function handleUninstallAll() {
    const installedDevices = devices.filter(d => d.deviceInfo.appInstalled);
    if (installedDevices.length === 0) {
      setMessage('No devices have the app installed');
      return;
    }
    if (!window.confirm(`Uninstall from ${installedDevices.length} device(s)?`)) return;
    setMessage('Uninstalling from all devices...');
    for (const device of installedDevices) {
      const serial = device.deviceInfo.serialNumber;
      const pkg = device.deviceInfo.safePathPackage;
      try {
        const result = await uninstallApp(serial, pkg);
        setMessage(prev => prev + '\n' + (result.message || `Done: ${serial}`));
      } catch (err) {
        setMessage(prev => prev + '\n❌ ' + serial + ': ' + err.message);
      }
    }
    if (onRefresh) onRefresh();
  }

  return (
    <div className="install-panel">
      <h3>📦 Install Build</h3>

      {/* Drag and drop zone */}
      <div
        className={`drop-zone ${dragging ? 'drop-zone-active' : ''}`}
        onDrop={handleDrop}
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onClick={() => fileInputRef.current.click()}
      >
        <input
          ref={fileInputRef}
          type="file"
          accept=".apk"
          onChange={handleFileInput}
          style={{ display: 'none' }}
        />
        {uploading ? (
          <p>⏳ Uploading...</p>
        ) : (
          <p>📂 Drag & drop APK here, or <strong>click to select</strong></p>
        )}
      </div>

      {/* Build history dropdown */}
      {buildHistory.length > 0 && (
        <div className="build-history">
          <label><strong>Recent:</strong></label>
          <select
            value={selectedPath}
            onChange={(e) => {
              const entry = buildHistory.find(h => h.path === e.target.value);
              if (entry) handleHistorySelect(entry);
            }}
          >
            <option value="">-- Select from history --</option>
            {buildHistory.map((entry) => (
              <option key={entry.path} value={entry.path}>
                {entry.fileName}
              </option>
            ))}
          </select>
        </div>
      )}

      {/* Install buttons */}
      {selectedPath && (
        <div className="install-targets">
          <p><strong>Ready to install:</strong> {selectedName}</p>
          <div className="install-buttons">
            <button onClick={handleInstallAll} className="install-all-btn">
              Install on Selected ({selectedDevices.length})
            </button>
          </div>
        </div>
      )}

      {/* Uninstall All - always visible when devices have app installed */}
      {devices.some(d => d.deviceInfo.appInstalled) && (
        <div className="uninstall-all-row">
          <button onClick={handleUninstallAll} className="uninstall-all-btn">
            Uninstall All
          </button>
        </div>
      )}

      {message && <p className="install-message">{message}</p>}
    </div>
  );
}

export default InstallPanel;
