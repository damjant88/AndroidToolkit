import React, { useState, useRef } from 'react';
import { uploadBuild, startInstallJob, startUninstallJob, getJob } from '../api/deviceApi';

const MAX_HISTORY = 5;

function InstallPanel({ devices, selectedDevices, onRefresh }) {
  const [selectedPath, setSelectedPath] = useState('');
  const [selectedName, setSelectedName] = useState('');
  const [message, setMessage] = useState('');
  const [dragging, setDragging] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [jobRunning, setJobRunning] = useState(false);
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

  async function pollJobUntilDone(jobId, serialToName) {
    while (true) {
      await new Promise(resolve => setTimeout(resolve, 1000));
      const job = await getJob(jobId);
      const completed = job.completedCount || 0;
      const total = job.totalCount || 0;
      const percent = job.progressPercent || 0;

      const results = job.deviceResults || {};
      const lines = Object.values(results).map(r =>
        (r.success ? '✅' : '❌') + ' ' + (serialToName[r.serial] || r.serial) + ': ' + r.message
      );
      const progress = `[${percent}%] ${completed}/${total} devices done`;
      setMessage(progress + (lines.length > 0 ? '\n' + lines.join('\n') : ''));

      if (job.status === 'COMPLETED' || job.status === 'FAILED') {
        return job;
      }
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
    setJobRunning(true);
    setMessage(`Starting install on ${selectedDevices.length} device(s)...`);
    try {
      const serials = selectedDevices.map(d => d.deviceInfo.serialNumber);
      const serialToName = {};
      selectedDevices.forEach(d => { serialToName[d.deviceInfo.serialNumber] = d.deviceName; });
      const result = await startInstallJob(selectedPath, serials);
      addToHistory(selectedName, selectedPath);
      await pollJobUntilDone(result.jobId, serialToName);
      if (onRefresh) onRefresh();
    } catch (err) {
      setMessage('❌ ' + (err.response?.data?.message || err.message));
    } finally {
      setJobRunning(false);
    }
  }

  async function handleUninstallAll() {
    const installedDevices = devices.filter(d => d.deviceInfo.appInstalled);
    if (installedDevices.length === 0) {
      setMessage('No devices have the app installed');
      return;
    }
    if (!window.confirm('Uninstall from ' + installedDevices.length + ' device(s)?')) return;
    setJobRunning(true);
    setMessage(`Starting uninstall on ${installedDevices.length} device(s)...`);
    try {
      const serials = installedDevices.map(d => d.deviceInfo.serialNumber);
      const serialToName = {};
      installedDevices.forEach(d => { serialToName[d.deviceInfo.serialNumber] = d.deviceName; });
      const result = await startUninstallJob(serials);
      await pollJobUntilDone(result.jobId, serialToName);
      if (onRefresh) onRefresh();
    } catch (err) {
      setMessage('❌ ' + (err.response?.data?.message || err.message));
    } finally {
      setJobRunning(false);
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
            {jobRunning ? '⏳ Installing...' : `Install on Selected (${selectedDevices.length})`}
          </button>
        </div>
      </div>

      {devices.some(d => d.deviceInfo.appInstalled) && (
        <div className="uninstall-all-row">
          <button onClick={handleUninstallAll} className="uninstall-all-btn" disabled={jobRunning}>
            {jobRunning ? '⏳ Uninstalling...' : 'Uninstall All'}
          </button>
        </div>
      )}

      {message && <p className="install-message" style={{whiteSpace: 'pre-line'}}>{message}</p>}
    </div>
  );
}

export default InstallPanel;
