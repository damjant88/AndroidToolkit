import React, { useState } from 'react';
import { uploadBuild, installBuild } from '../api/deviceApi';

function InstallPanel({ devices, onRefresh }) {
  const [selectedFile, setSelectedFile] = useState(null);
  const [uploadedPath, setUploadedPath] = useState('');
  const [uploadedName, setUploadedName] = useState('');
  const [message, setMessage] = useState('');
  const [installing, setInstalling] = useState({});

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
        <input
          type="file"
          accept=".apk"
          onChange={(e) => setSelectedFile(e.target.files[0])}
        />
        <button onClick={handleUpload} disabled={!selectedFile}>
          Upload APK
        </button>
      </div>

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
