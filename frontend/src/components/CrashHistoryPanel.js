import React, { useState, useEffect, useCallback } from 'react';
import { crashApi } from '../api/crashApi';

/**
 * CrashHistoryPanel displays a paginated list of crash events for a project
 * with filter controls for device serial, date range, severity, and crash type.
 */
function CrashHistoryPanel({ projectId, onSelectCrash }) {
  const [crashes, setCrashes] = useState([]);
  const [totalPages, setTotalPages] = useState(0);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(false);
  const [filters, setFilters] = useState({
    deviceSerial: '',
    fromDate: '',
    toDate: '',
    severity: '',
    crashType: '',
  });

  const fetchCrashes = useCallback(async () => {
    setLoading(true);
    try {
      const params = { page, size: 20 };
      if (filters.deviceSerial) params.deviceSerial = filters.deviceSerial;
      if (filters.fromDate) params.fromDate = filters.fromDate;
      if (filters.toDate) params.toDate = filters.toDate;
      if (filters.severity) params.severity = filters.severity;
      if (filters.crashType) params.crashType = filters.crashType;

      const response = await crashApi.getCrashes(projectId, params);
      setCrashes(response.data.content || []);
      setTotalPages(response.data.totalPages || 0);
    } catch (err) {
      console.error('Failed to fetch crash history:', err);
    } finally {
      setLoading(false);
    }
  }, [projectId, page, filters]);

  useEffect(() => {
    if (projectId) fetchCrashes();
  }, [fetchCrashes, projectId]);

  const handleFilterChange = (field, value) => {
    setFilters(prev => ({ ...prev, [field]: value }));
    setPage(0);
  };

  const severityColor = (severity) => {
    switch (severity) {
      case 'FATAL': return '#dc2626';
      case 'ANR': return '#ea580c';
      case 'WARNING': return '#ca8a04';
      default: return '#6b7280';
    }
  };

  return (
    <div className="crash-history-panel" style={{ padding: '16px' }}>
      <h3 style={{ margin: '0 0 12px 0' }}>Crash History</h3>

      {/* Filters */}
      <div style={{ display: 'flex', gap: '8px', marginBottom: '12px', flexWrap: 'wrap' }}>
        <input
          type="text"
          placeholder="Device Serial"
          value={filters.deviceSerial}
          onChange={(e) => handleFilterChange('deviceSerial', e.target.value)}
          style={{ padding: '4px 8px', borderRadius: '4px', border: '1px solid #d1d5db', fontSize: '12px' }}
          aria-label="Filter by device serial"
        />
        <input
          type="date"
          value={filters.fromDate}
          onChange={(e) => handleFilterChange('fromDate', e.target.value)}
          style={{ padding: '4px 8px', borderRadius: '4px', border: '1px solid #d1d5db', fontSize: '12px' }}
          aria-label="From date"
        />
        <input
          type="date"
          value={filters.toDate}
          onChange={(e) => handleFilterChange('toDate', e.target.value)}
          style={{ padding: '4px 8px', borderRadius: '4px', border: '1px solid #d1d5db', fontSize: '12px' }}
          aria-label="To date"
        />
        <select
          value={filters.severity}
          onChange={(e) => handleFilterChange('severity', e.target.value)}
          style={{ padding: '4px 8px', borderRadius: '4px', border: '1px solid #d1d5db', fontSize: '12px' }}
          aria-label="Filter by severity"
        >
          <option value="">All Severities</option>
          <option value="FATAL">Fatal</option>
          <option value="ANR">ANR</option>
          <option value="WARNING">Warning</option>
        </select>
      </div>

      {/* Crash list */}
      {loading ? (
        <p style={{ color: '#6b7280' }}>Loading...</p>
      ) : crashes.length === 0 ? (
        <p style={{ color: '#6b7280' }}>No crash events found.</p>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
          {crashes.map(crash => (
            <div
              key={crash.id}
              onClick={() => onSelectCrash && onSelectCrash(crash.id)}
              style={{
                padding: '10px 12px',
                border: '1px solid #e5e7eb',
                borderRadius: '6px',
                cursor: 'pointer',
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
              }}
              role="button"
              tabIndex={0}
              onKeyDown={(e) => { if (e.key === 'Enter' && onSelectCrash) onSelectCrash(crash.id); }}
              aria-label={`Crash event: ${crash.crashType} on ${crash.deviceSerial}`}
            >
              <div>
                <span style={{ color: severityColor(crash.severity), fontWeight: 'bold', fontSize: '12px' }}>
                  {crash.severity}
                </span>
                <span style={{ marginLeft: '8px', fontSize: '13px' }}>{crash.crashType}</span>
                <span style={{ marginLeft: '8px', color: '#6b7280', fontSize: '12px' }}>
                  {crash.deviceSerial}
                </span>
              </div>
              <span style={{ color: '#9ca3af', fontSize: '11px' }}>
                {new Date(crash.timestamp).toLocaleString()}
              </span>
            </div>
          ))}
        </div>
      )}

      {/* Pagination */}
      {totalPages > 1 && (
        <div style={{ display: 'flex', justifyContent: 'center', gap: '8px', marginTop: '12px' }}>
          <button
            onClick={() => setPage(p => Math.max(0, p - 1))}
            disabled={page === 0}
            style={{ padding: '4px 12px', borderRadius: '4px', border: '1px solid #d1d5db', cursor: 'pointer' }}
          >
            Previous
          </button>
          <span style={{ padding: '4px 8px', fontSize: '13px' }}>
            Page {page + 1} of {totalPages}
          </span>
          <button
            onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
            disabled={page >= totalPages - 1}
            style={{ padding: '4px 12px', borderRadius: '4px', border: '1px solid #d1d5db', cursor: 'pointer' }}
          >
            Next
          </button>
        </div>
      )}
    </div>
  );
}

export default CrashHistoryPanel;
