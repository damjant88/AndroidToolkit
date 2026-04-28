import React, { useState, useRef } from 'react';
import { uploadBuild, installBuild } from '../api/deviceApi';

const MAX_HISTORY = 5;

function InstallPanel({ devices, onRefresh }) {
  const [selectedFile, setSelectedFile] = useState(null);
  const [uploadedPath, setUploadedPath] = useState('');
  const [uploadedName, setUploadedName] = useState('');
  const [message, setMessage] = useState('');
  const [installing, setInstalling] = useState({});
  const [buildHistory, setBuildHistory] = useState(() => {
    // Load history from localStorage so it persists across page reloads
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

  function handleFileSelect(e) {
    const file = e.target.files[0];
    if (file) {
      setSelectedFile(file);
      setUploadedPath('');
      setUploadedName('');
    }
  }

  function handleHistorySelect(entry) {
    setSelectedFile(null);
    setUploadedPath(entry.path);
    setUploadedName(entry.fileName);
    setMessage(`Selected from history: ${entry.fileName}`);
  }

  async function handleUpload() {
    if (!selectedFile) {
      setMessage('Select an APK file first');
      return;
    }
    setMessage('Uploading...');
    try {
      const result = await uploadBuild(selectedFile);
      setUploadedPath(result.path);
      setUploadedName(result.fileName);
      addToHistory(result.fileName, result.path);
      setMessage('✅ ' + result.message);
    } catch (err) {
      setMessage('❌ Upload failed: ' + (err.response?.data?.message || err.message));
    }
  }

  async function handleInstall(serial) {
    if (!uploadedPath) {
      setMessage('Upload an APK first');
      return;
    }
    setInstalling((prev) => ({ ...prev, [serial]: true }));
    setMessage(`Installing ${uploadedName} on ${serial}...`);
    try {
      const result = await installBuild(serial, uploadedPath);
      setMessage(result.success ? '✅ ' + result.message : '❌ ' + result.message);
      if (result.success && onRefresh) onRefresh();
    } catch (err) {
      setMessage('❌ Install failed: ' + (err.response?.data?.message || err.message));
    } finally {
      setInstalling((prev) => ({ ...prev, [serial]: false }));
    }
  }

  async function handleInstallAll() {
    if (!uploadedPath) {
      setMessage('Upload an APK first');
      return;
    }
    setMessage(`Installing ${uploadedName} on all devices...`);
    for (const device of devices) {
      await handleInstall(device.serial);
    }
  }

  return (
    <div className="install-panel">
      <h3>📦 Install Build</h3>

      <div className="install-controls">
        {/* Hidden native file input */}
        <input
          ref={fileInputRef}
          type="file"
          accept=".apk"
          onChange={handleFileSelect}
          style={{ display: 'none' }}
        />

        {/* Custom styled button */}
        <button className="select-build-btn" onClick={() => fileInputRef.current.click()}>
          Select Build
        </button>

        <button onClick={handleUpload} disabled={!selectedFile}>
          Upload APK
        </button>
      </div>

      {/* Show selected file name */}
      {selectedFile && (
        <p className="selected-file-name">📄 {selectedFile.name}</p>
      )}

      {/* Build history dropdown — like the Swing app's FileTextFieldBox */}
      {buildHistory.length > 0 && (
        <div className="build-history">
          <label><strong>Recent builds:</strong></label>
          <select
            value={uploadedPath}
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

      {/* Ready to install section */}
      {uploadedPath && (
        <div className="install-targets">
          <p><strong>Ready to install:</strong> {uploadedName}</p>
          <div className="install-buttons">
            <button onClick={handleInstallAll} className="install-all-btn">
              Install on All Devices
            </button>
            {devices.map((device) => (
              <button
                key={device.serial}
                disabled={installing[device.serial]}
                onClick={() => handleInstall(device.serial)}
              >
                {installing[device.serial] ? 'Installing...' : `Install → ${device.deviceName}`}
              </button>
            ))}
          </div>
        </div>
      )}

      {message && <p className="install-message">{message}</p>}
    </div>
  );
}

export default InstallPanel;
