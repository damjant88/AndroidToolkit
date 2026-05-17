import React, { useState, useEffect } from 'react';
import { crashApi } from '../api/crashApi';

/**
 * CrashDetailView shows full crash event details including stack trace,
 * metadata, crash log download link, and acknowledge button.
 */
function CrashDetailView({ projectId, crashEventId, onBack }) {
  const [crash, setCrash] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!crashEventId) return;
    setLoading(true);
    crashApi.getCrashDetail(projectId, crashEventId)
      .then(res => setCrash(res.data))
      .catch(err => setError(err.message))
      .finally(() => setLoading(false));
  }, [projectId, crashEventId]);

  const handleDownloadLog = async () => {
    try {
      const response = await crashApi.downloadCrashLog(projectId, crashEventId);
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `crash-${crashEventId}.log`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      console.error('Failed to download crash log:', err);
    }
  };

  const handleAcknowledge = async () => {
    try {
      const res = await crashApi.acknowledgeCrash(projectId, crashEventId);
      setCrash(res.data);
    } catch (err) {
      console.error('Failed to acknowledge crash:', err);
    }
  };

  if (loading) return <p style={{ padding: '16px', color: '#6b7280' }}>Loading crash details...</p>;
  if (error) return <p style={{ padding: '16px', color: '#dc2626' }}>Error: {error}</p>;
  if (!crash) return <p style={{ padding: '16px', color: '#6b7280' }}>Crash event not found.</p>;

  const severityColor = {
    FATAL: '#dc2626',
    ANR: '#ea580c',
    WARNING: '#ca8a04',
  }[crash.severity] || '#6b7280';

  return (
    <div className="crash-detail-view" style={{ padding: '16px' }}>
      {onBack && (
        <button
          onClick={onBack}
          style={{ marginBottom: '12px', background: 'none', border: 'none', cursor: 'pointer', color: '#3b82f6', fontSize: '13px' }}
        >
          ← Back to crash history
        </button>
      )}

      <h3 style={{ margin: '0 0 16px 0' }}>Crash Event Detail</h3>

      {/* Metadata */}
      <div style={{ display: 'grid', gridTemplateColumns: '140px 1fr', gap: '8px', fontSize: '13px', marginBottom: '16px' }}>
        <span style={{ color: '#6b7280' }}>Severity:</span>
        <span style={{ color: severityColor, fontWeight: 'bold' }}>{crash.severity}</span>

        <span style={{ color: '#6b7280' }}>Crash Type:</span>
        <span>{crash.crashType}</span>

        <span style={{ color: '#6b7280' }}>Device:</span>
        <span>{crash.deviceName || crash.deviceSerial}</span>

        <span style={{ color: '#6b7280' }}>Serial:</span>
        <span style={{ fontFamily: 'monospace', fontSize: '12px' }}>{crash.deviceSerial}</span>

        <span style={{ color: '#6b7280' }}>Package:</span>
        <span>{crash.packageName || 'unknown'}</span>

        <span style={{ color: '#6b7280' }}>Timestamp:</span>
        <span>{new Date(crash.timestamp).toLocaleString()}</span>

        <span style={{ color: '#6b7280' }}>Acknowledged:</span>
        <span>{crash.acknowledged ? `Yes (${new Date(crash.acknowledgedAt).toLocaleString()})` : 'No'}</span>
      </div>

      {/* Stack trace */}
      {crash.stackTraceSnippet && (
        <div style={{ marginBottom: '16px' }}>
          <h4 style={{ margin: '0 0 8px 0', fontSize: '13px' }}>Stack Trace</h4>
          <pre style={{
            background: '#1f2937',
            color: '#f9fafb',
            padding: '12px',
            borderRadius: '6px',
            fontSize: '11px',
            overflow: 'auto',
            maxHeight: '300px',
            whiteSpace: 'pre-wrap',
            wordBreak: 'break-all',
          }}>
            {crash.stackTraceSnippet}
          </pre>
        </div>
      )}

      {/* Actions */}
      <div style={{ display: 'flex', gap: '8px' }}>
        {crash.crashLogPath && (
          <button
            onClick={handleDownloadLog}
            style={{
              padding: '6px 12px',
              background: '#3b82f6',
              color: 'white',
              border: 'none',
              borderRadius: '4px',
              cursor: 'pointer',
              fontSize: '12px',
            }}
          >
            Download Crash Log
          </button>
        )}
        {!crash.acknowledged && (
          <button
            onClick={handleAcknowledge}
            style={{
              padding: '6px 12px',
              background: '#10b981',
              color: 'white',
              border: 'none',
              borderRadius: '4px',
              cursor: 'pointer',
              fontSize: '12px',
            }}
          >
            Acknowledge
          </button>
        )}
      </div>
    </div>
  );
}

export default CrashDetailView;
