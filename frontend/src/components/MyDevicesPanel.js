import React, { useState } from 'react';

function MyDevicesPanel() {
  const [manualDevices, setManualDevices] = useState(() => {
    const saved = localStorage.getItem('manualDevices');
    return saved ? JSON.parse(saved) : [];
  });
  const [newDevice, setNewDevice] = useState('');

  function handleAdd() {
    if (!newDevice.trim()) return;
    const updated = [...manualDevices, newDevice.trim()];
    setManualDevices(updated);
    localStorage.setItem('manualDevices', JSON.stringify(updated));
    setNewDevice('');
  }

  function handleRemove(idx) {
    const updated = manualDevices.filter((_, i) => i !== idx);
    setManualDevices(updated);
    localStorage.setItem('manualDevices', JSON.stringify(updated));
  }

  return (
    <div className="allowed-users-panel">
      <h4>📱 My Devices</h4>
      <p style={{ fontSize: '0.8rem', color: '#666', marginBottom: '8px' }}>
        Add info about your devices. These appear in bug report DEVICE INFO.
      </p>
      <div className="grant-form">
        <input value={newDevice} onChange={e => setNewDevice(e.target.value)}
          placeholder="e.g. iPhone SE (16.3)" onKeyDown={e => e.key === 'Enter' && handleAdd()} />
        <button onClick={handleAdd}>Add</button>
      </div>
      {manualDevices.length === 0 ? (
        <p className="grant-empty">No manual devices added yet.</p>
      ) : (
        <ul className="grant-list">
          {manualDevices.map((md, i) => (
            <li key={i}><span>{md}</span><button onClick={() => handleRemove(i)}>✕</button></li>
          ))}
        </ul>
      )}
    </div>
  );
}

export default MyDevicesPanel;
