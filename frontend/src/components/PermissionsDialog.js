import React, { useState, useEffect } from 'react';
import { getPermissions, enablePermissions, disablePermissions } from '../api/deviceApi';

function PermissionsDialog({ serial, packageName, deviceName, onClose }) {
  const [definitions, setDefinitions] = useState([]);
  const [activeIds, setActiveIds] = useState([]);
  const [unavailableIds, setUnavailableIds] = useState([]);
  const [selectedIds, setSelectedIds] = useState([]);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [message, setMessage] = useState('');

  useEffect(() => {
    loadPermissions();
  }, [serial, packageName]);

  async function loadPermissions() {
    setLoading(true);
    try {
      const state = await getPermissions(serial, packageName);
      setDefinitions(state.definitions || []);
      setActiveIds(state.activePermissionIds || []);
      setUnavailableIds(state.unavailablePermissionIds || []);
      // Pre-select active permissions
      setSelectedIds(state.activePermissionIds || []);
    } catch (err) {
      setMessage('Failed to load permissions: ' + (err.response?.data?.message || err.message));
    } finally {
      setLoading(false);
    }
  }

  function toggleSelection(id) {
    setSelectedIds(prev =>
      prev.includes(id) ? prev.filter(x => x !== id) : [...prev, id]
    );
  }

  function selectAll() {
    const available = definitions
      .filter(d => !unavailableIds.includes(d.id))
      .map(d => d.id);
    setSelectedIds(available);
  }

  function deselectAll() {
    setSelectedIds([]);
  }

  async function handleEnable() {
    setActionLoading(true);
    setMessage('');
    try {
      const result = await enablePermissions(serial, packageName, selectedIds);
      const updateResult = result.updateResult;
      setMessage(updateResult.successful
        ? `✅ Enabled ${updateResult.appliedCount} permissions on ${deviceName}`
        : `⚠️ Enabled ${updateResult.appliedCount} of ${updateResult.requestedCount} on ${deviceName}`
      );
      // Refresh state
      const refreshed = result.dialogState;
      setActiveIds(refreshed.activePermissionIds || []);
      setUnavailableIds(refreshed.unavailablePermissionIds || []);
    } catch (err) {
      setMessage('Error: ' + (err.response?.data?.message || err.message));
    } finally {
      setActionLoading(false);
    }
  }

  async function handleDisable() {
    setActionLoading(true);
    setMessage('');
    try {
      const result = await disablePermissions(serial, packageName, selectedIds);
      const updateResult = result.updateResult;
      setMessage(updateResult.successful
        ? `✅ Disabled ${updateResult.appliedCount} permissions on ${deviceName}`
        : `⚠️ Disabled ${updateResult.appliedCount} of ${updateResult.requestedCount} on ${deviceName}`
      );
      // Refresh state
      const refreshed = result.dialogState;
      setActiveIds(refreshed.activePermissionIds || []);
      setUnavailableIds(refreshed.unavailablePermissionIds || []);
    } catch (err) {
      setMessage('Error: ' + (err.response?.data?.message || err.message));
    } finally {
      setActionLoading(false);
    }
  }

  if (loading) {
    return (
      <div className="permissions-overlay">
        <div className="permissions-dialog">
          <p>Loading permissions...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="permissions-overlay">
      <div className="permissions-dialog">
        <div className="permissions-header">
          <h3>Permissions</h3>
          <button className="close-btn" onClick={onClose}>✕</button>
        </div>

        <div className="permissions-info">
          <p><strong>Device:</strong> {deviceName}</p>
          <p><strong>Package:</strong> {packageName}</p>
        </div>

        <div className="permissions-select-all">
          <button onClick={selectAll}>Select All</button>
          <button onClick={deselectAll}>Deselect All</button>
        </div>

        <div className="permissions-list">
          {definitions.map(def => {
            const isUnavailable = unavailableIds.includes(def.id);
            const isActive = activeIds.includes(def.id);
            const isSelected = selectedIds.includes(def.id);

            return (
              <div key={def.id} className={`permission-row ${isUnavailable ? 'unavailable' : ''}`}>
                <label>
                  <input
                    type="checkbox"
                    checked={isSelected}
                    disabled={isUnavailable || actionLoading}
                    onChange={() => toggleSelection(def.id)}
                  />
                  {def.label}
                </label>
                <span className={`permission-status ${isUnavailable ? 'status-unavailable' : isActive ? 'status-enabled' : 'status-disabled'}`}>
                  {isUnavailable ? 'Unavailable' : isActive ? 'Enabled' : 'Disabled'}
                </span>
              </div>
            );
          })}
        </div>

        {message && <p className="permissions-message">{message}</p>}

        <div className="permissions-actions">
          <button onClick={handleEnable} disabled={actionLoading || selectedIds.length === 0}>
            Enable
          </button>
          <button onClick={handleDisable} disabled={actionLoading || selectedIds.length === 0}>
            Disable
          </button>
          <button onClick={onClose}>Cancel</button>
        </div>
      </div>
    </div>
  );
}

export default PermissionsDialog;
