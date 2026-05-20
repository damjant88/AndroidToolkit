import React, { useState, useRef } from 'react';
import { startInstallJob, startUninstallJob, getJob } from '../api/deviceApi';
import axios from 'axios';

const MAX_HISTORY = 5;

function InstallPanel({ devices, selectedDevices, onRefresh }) {
  const [selectedPath, setSelectedPath] = useState(() => {
    const saved = localStorage.getItem('buildHistory');
    const history = saved ? JSON.parse(saved) : [];
    return history.length > 0 ? history[0].path : '';
  });
  const [selectedName, setSelectedName] = useState(() => {
    const saved = localStorage.getItem('buildHistory');
    const history = saved ? JSON.parse(saved) : [];
    return history.length > 0 ? history[0].fileName : '';
  });
  const [message, setMessage] = useState('');
  const [dragging, setDragging] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [installing, setInstalling] = useState(false);
  const [uninstalling, setUninstalling] = useState(false);
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
    const filtered = buildHistory.filter(h => h.path !== path && h.fileName !== fileName);
    const updated = [entry, ...filtered].slice(0, MAX_HISTORY);
    saveHistory(updated);
  }

  async function handleFileSelected(file) {
    if (!file || !file.name.endsWith('.apk')) {
      setMessage('❌ Only .apk files are accepted');
      return;
    }
    setSelectedName(file.name);
    setMessage(`Sending ${file.name} to agent...`);
    setUploading(true);
    try {
      // Send APK directly to agent — it saves locally and installs via adb
      const formData = new FormData();
      formData.append('file', file);
      const result = await axios.post('http://localhost:8081/api/agent/devices/upload-apk', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
        timeout: 120000
      });
      if (result.data.success) {
        setSelectedPath(result.data.path);
        addToHistory(file.name, result.data.path);
        setMessage(`✅ Ready to install: ${file.name}`);
      } else {
        setMessage('❌ ' + result.data.message);
      }
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

  async function pollJobUntilDone(jobId, serialToName) {
    while (true) {
      await new Promise(resolve => setTimeout(resolve, 1000));
      const job = await getJob(jobId);
      const completed = job.completedCount || 0;
      const total = job.totalCount || 0;
      const results = Object.values(job.deviceResults || {}).map(r => ({
        label: serialToName[r.serial] || r.serial,
        success: r.success,
        message: r.message
      }));
      const lines = results.map(r => (r.success ? '✅' : '❌') + ' ' + r.label + ': ' + r.message);
      setMessage(`⏳ ${completed}/${total} devices done` + (lines.length > 0 ? '\n' + lines.join('\n') : ''));

      if (job.status === 'COMPLETED' || job.status === 'FAILED') {
        setMessage(lines.join('\n'));
        return job;
      }
    }
  }

  const jobRunning = installing || uninstalling;
  const hasInstalledDevices = devices.some(d => d.deviceInfo.appInstalled);

  async function handleInstallAll() {
    // Always use the most recent build from history if selectedPath is empty
    const pathToInstall = selectedPath || (buildHistory.length > 0 ? buildHistory[0].path : '');
    if (!pathToInstall) {
      setMessage('Select an APK first');
      return;
    }
    if (selectedDevices.length === 0) {
      setMessage('No devices selected');
      return;
    }
    const devicesWithApp = selectedDevices.filter(d => d.deviceInfo.appInstalled);
    if (devicesWithApp.length > 0) {
      if (!window.confirm('Are you sure you want to install APK on top of the existing one?')) return;
    }
    setInstalling(true);
    setMessage('⏳ Installing...');
    try {
      const serials = selectedDevices.map(d => d.deviceInfo.serialNumber);
      const serialToName = {};
      selectedDevices.forEach(d => { serialToName[d.deviceInfo.serialNumber] = d.deviceName; });
      const result = await startInstallJob(pathToInstall, serials);
      addToHistory(selectedName || buildHistory[0]?.fileName || 'unknown.apk', pathToInstall);

      // Display results directly (agent returns complete results, no polling needed)
      const results = Object.values(result.deviceResults || {});
      const lines = results.map(r => (r.success ? '✅' : '❌') + ' ' + (serialToName[r.serial] || r.serial) + ': ' + r.message);
      setMessage(lines.join('\n') || '✅ Install complete');
      if (onRefresh) onRefresh();
    } catch (err) {
      setMessage('❌ ' + (err.response?.data?.message || err.message));
    } finally {
      setInstalling(false);
    }
  }

  async function handleUninstallAll() {
    const installedDevices = devices.filter(d => d.deviceInfo.appInstalled);
    if (installedDevices.length === 0) return;
    if (!window.confirm('Uninstall from ' + installedDevices.length + ' device(s)?')) return;
    setUninstalling(true);
    setMessage('⏳ Uninstalling...');
    try {
      const serials = installedDevices.map(d => d.deviceInfo.serialNumber);
      const serialToName = {};
      installedDevices.forEach(d => { serialToName[d.deviceInfo.serialNumber] = d.deviceName; });
      const result = await startUninstallJob(serials, installedDevices);
      const results = Object.values(result.deviceResults || {});
      const lines = results.map(r => (r.success ? '✅' : '❌') + ' ' + (serialToName[r.serial] || r.serial) + ': ' + r.message);
      setMessage(lines.join('\n') || '✅ Uninstall complete');
      if (onRefresh) onRefresh();
    } catch (err) {
      setMessage('❌ ' + (err.response?.data?.message || err.message));
    } finally {
      setUninstalling(false);
    }
  }

  return (
    <div className="install-panel">
      <h3>📦 Install Build</h3>

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

      <div className="install-targets">
        <div className="install-buttons">
          <button
            onClick={handleInstallAll}
            className="install-all-btn"
            disabled={!selectedPath || selectedDevices.length === 0 || jobRunning}
          >
            {installing ? '⏳ Installing...' : `Install on Selected (${selectedDevices.length})`}
          </button>
        </div>
      </div>

      <div className="uninstall-all-row">
        <button onClick={handleUninstallAll} className="uninstall-all-btn" disabled={!hasInstalledDevices || jobRunning}>
          {uninstalling ? '⏳ Uninstalling...' : 'Uninstall All'}
        </button>
      </div>

      {message && <p className="install-message" style={{whiteSpace: 'pre-line'}}>{message}</p>}
    </div>
  );
}

export default InstallPanel;
