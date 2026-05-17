import React, { useState, useEffect } from 'react';
import { crashApi } from '../api/crashApi';

/**
 * AlertConfigPanel provides per-project alert configuration UI.
 * Includes toggles for crash detection, auto-pull, notification preferences,
 * buffer size input, and custom pattern management.
 */
function AlertConfigPanel({ projectId }) {
  const [config, setConfig] = useState(null);
  const [patterns, setPatterns] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [newPattern, setNewPattern] = useState({ regex: '', severity: 'WARNING' });
  const [patternError, setPatternError] = useState('');

  useEffect(() => {
    if (!projectId) return;
    loadConfig();
    loadPatterns();
  }, [projectId]); // eslint-disable-line react-hooks/exhaustive-deps

  const loadConfig = async () => {
    try {
      const res = await crashApi.getAlertConfig(projectId);
      setConfig(res.data);
    } catch (err) {
      console.error('Failed to load alert config:', err);
    } finally {
      setLoading(false);
    }
  };

  const loadPatterns = async () => {
    try {
      const res = await crashApi.getPatterns(projectId);
      setPatterns(res.data);
    } catch (err) {
      console.error('Failed to load patterns:', err);
    }
  };

  const handleSave = async () => {
    setSaving(true);
    try {
      const res = await crashApi.updateAlertConfig(projectId, config);
      setConfig(res.data);
    } catch (err) {
      console.error('Failed to save alert config:', err);
    } finally {
      setSaving(false);
    }
  };

  const handleAddPattern = async () => {
    setPatternError('');
    if (!newPattern.regex.trim()) {
      setPatternError('Regex pattern cannot be empty');
      return;
    }
    try {
      await crashApi.addPattern(projectId, newPattern);
      setNewPattern({ regex: '', severity: 'WARNING' });
      loadPatterns();
    } catch (err) {
      setPatternError(err.response?.data?.message || 'Failed to add pattern');
    }
  };

  const handleRemovePattern = async (patternId) => {
    try {
      await crashApi.removePattern(projectId, patternId);
      loadPatterns();
    } catch (err) {
      console.error('Failed to remove pattern:', err);
    }
  };

  if (loading) return <p style={{ padding: '16px', color: '#6b7280' }}>Loading configuration...</p>;
  if (!config) return <p style={{ padding: '16px', color: '#dc2626' }}>Failed to load configuration.</p>;

  return (
    <div className="alert-config-panel" style={{ padding: '16px' }}>
      <h3 style={{ margin: '0 0 16px 0' }}>Alert Configuration</h3>

      {/* Toggles */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', marginBottom: '16px' }}>
        <label style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '13px' }}>
          <input
            type="checkbox"
            checked={config.crashDetectionEnabled}
            onChange={(e) => setConfig({ ...config, crashDetectionEnabled: e.target.checked })}
          />
          Crash Detection Enabled
        </label>

        <label style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '13px' }}>
          <input
            type="checkbox"
            checked={config.autoPullEnabled}
            onChange={(e) => setConfig({ ...config, autoPullEnabled: e.target.checked })}
          />
          Auto-Pull Logs on Crash
        </label>

        <label style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '13px' }}>
          Notifications:
          <select
            value={config.notificationPreference}
            onChange={(e) => setConfig({ ...config, notificationPreference: e.target.value })}
            style={{ padding: '4px 8px', borderRadius: '4px', border: '1px solid #d1d5db', fontSize: '12px' }}
          >
            <option value="BROWSER">Browser</option>
            <option value="IN_APP">In-App</option>
            <option value="BOTH">Both</option>
            <option value="NONE">None</option>
          </select>
        </label>

        <label style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '13px' }}>
          Buffer Size:
          <input
            type="number"
            min="50"
            max="2000"
            value={config.bufferSize}
            onChange={(e) => setConfig({ ...config, bufferSize: parseInt(e.target.value) || 500 })}
            style={{ width: '80px', padding: '4px 8px', borderRadius: '4px', border: '1px solid #d1d5db', fontSize: '12px' }}
            aria-label="Buffer size (50-2000)"
          />
          <span style={{ color: '#6b7280', fontSize: '11px' }}>(50-2000 lines)</span>
        </label>
      </div>

      <button
        onClick={handleSave}
        disabled={saving}
        style={{
          padding: '6px 16px',
          background: '#3b82f6',
          color: 'white',
          border: 'none',
          borderRadius: '4px',
          cursor: 'pointer',
          fontSize: '12px',
          marginBottom: '24px',
        }}
      >
        {saving ? 'Saving...' : 'Save Configuration'}
      </button>

      {/* Custom Patterns */}
      <h4 style={{ margin: '0 0 12px 0', fontSize: '14px' }}>Custom Crash Patterns</h4>

      {/* Add pattern form */}
      <div style={{ display: 'flex', gap: '8px', marginBottom: '12px', alignItems: 'center' }}>
        <input
          type="text"
          placeholder="Regex pattern"
          value={newPattern.regex}
          onChange={(e) => setNewPattern({ ...newPattern, regex: e.target.value })}
          style={{ flex: 1, padding: '4px 8px', borderRadius: '4px', border: '1px solid #d1d5db', fontSize: '12px' }}
          aria-label="New crash pattern regex"
        />
        <select
          value={newPattern.severity}
          onChange={(e) => setNewPattern({ ...newPattern, severity: e.target.value })}
          style={{ padding: '4px 8px', borderRadius: '4px', border: '1px solid #d1d5db', fontSize: '12px' }}
          aria-label="Pattern severity"
        >
          <option value="FATAL">Fatal</option>
          <option value="ANR">ANR</option>
          <option value="WARNING">Warning</option>
        </select>
        <button
          onClick={handleAddPattern}
          style={{
            padding: '4px 12px',
            background: '#10b981',
            color: 'white',
            border: 'none',
            borderRadius: '4px',
            cursor: 'pointer',
            fontSize: '12px',
          }}
        >
          Add
        </button>
      </div>
      {patternError && (
        <p style={{ color: '#dc2626', fontSize: '12px', margin: '0 0 12px 0' }}>{patternError}</p>
      )}

      {/* Pattern list */}
      {patterns.length === 0 ? (
        <p style={{ color: '#6b7280', fontSize: '12px' }}>No custom patterns configured.</p>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
          {patterns.map(pattern => (
            <div
              key={pattern.id}
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                padding: '6px 10px',
                border: '1px solid #e5e7eb',
                borderRadius: '4px',
                fontSize: '12px',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <code style={{ background: '#f3f4f6', padding: '2px 6px', borderRadius: '3px' }}>
                  {pattern.regex}
                </code>
                <span style={{ color: '#6b7280' }}>{pattern.severity}</span>
                {pattern.default && <span style={{ color: '#9ca3af', fontStyle: 'italic' }}>(default)</span>}
              </div>
              {!pattern.default && (
                <button
                  onClick={() => handleRemovePattern(pattern.id)}
                  style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#dc2626', fontSize: '14px' }}
                  aria-label={`Remove pattern ${pattern.regex}`}
                >
                  ✕
                </button>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export default AlertConfigPanel;
