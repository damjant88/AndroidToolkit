import React, { useState, useEffect } from 'react';
import { crashApi } from '../api/crashApi';

/**
 * CrashTrendChart displays daily crash frequency as a simple bar/table visualization.
 * Supports grouping by severity level or device, with preset date range selectors.
 */
function CrashTrendChart({ projectId }) {
  const [trendData, setTrendData] = useState([]);
  const [loading, setLoading] = useState(false);
  const [presetDays, setPresetDays] = useState(7);
  const [groupBy, setGroupBy] = useState('SEVERITY');

  useEffect(() => {
    if (!projectId) return;
    fetchTrends();
  }, [projectId, presetDays, groupBy]); // eslint-disable-line react-hooks/exhaustive-deps

  const fetchTrends = async () => {
    setLoading(true);
    try {
      const res = await crashApi.getCrashTrends(projectId, { presetDays, groupBy });
      setTrendData(res.data || []);
    } catch (err) {
      console.error('Failed to fetch crash trends:', err);
    } finally {
      setLoading(false);
    }
  };

  // Group data by date for display
  const groupedByDate = trendData.reduce((acc, item) => {
    const dateStr = item.date;
    if (!acc[dateStr]) acc[dateStr] = {};
    acc[dateStr][item.groupKey] = item.count;
    return acc;
  }, {});

  // Get all unique group keys
  const groupKeys = [...new Set(trendData.map(d => d.groupKey))].sort();

  const severityColor = (key) => {
    switch (key) {
      case 'FATAL': return '#dc2626';
      case 'ANR': return '#ea580c';
      case 'WARNING': return '#ca8a04';
      default: return '#6b7280';
    }
  };

  return (
    <div className="crash-trend-chart" style={{ padding: '16px' }}>
      <h3 style={{ margin: '0 0 12px 0' }}>Crash Trends</h3>

      {/* Controls */}
      <div style={{ display: 'flex', gap: '8px', marginBottom: '16px', alignItems: 'center' }}>
        <span style={{ fontSize: '12px', color: '#6b7280' }}>Period:</span>
        {[7, 14, 30].map(days => (
          <button
            key={days}
            onClick={() => setPresetDays(days)}
            style={{
              padding: '4px 10px',
              borderRadius: '4px',
              border: '1px solid #d1d5db',
              background: presetDays === days ? '#3b82f6' : 'white',
              color: presetDays === days ? 'white' : '#374151',
              cursor: 'pointer',
              fontSize: '12px',
            }}
          >
            {days}d
          </button>
        ))}
        <span style={{ fontSize: '12px', color: '#6b7280', marginLeft: '12px' }}>Group by:</span>
        <select
          value={groupBy}
          onChange={(e) => setGroupBy(e.target.value)}
          style={{ padding: '4px 8px', borderRadius: '4px', border: '1px solid #d1d5db', fontSize: '12px' }}
          aria-label="Group by"
        >
          <option value="SEVERITY">Severity</option>
          <option value="DEVICE">Device</option>
        </select>
      </div>

      {loading ? (
        <p style={{ color: '#6b7280' }}>Loading trends...</p>
      ) : Object.keys(groupedByDate).length === 0 ? (
        <p style={{ color: '#6b7280' }}>No crash data for this period.</p>
      ) : (
        <div style={{ overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '12px' }}>
            <thead>
              <tr>
                <th style={{ textAlign: 'left', padding: '6px 8px', borderBottom: '1px solid #e5e7eb' }}>Date</th>
                {groupKeys.map(key => (
                  <th
                    key={key}
                    style={{
                      textAlign: 'center',
                      padding: '6px 8px',
                      borderBottom: '1px solid #e5e7eb',
                      color: groupBy === 'SEVERITY' ? severityColor(key) : '#374151',
                    }}
                  >
                    {key}
                  </th>
                ))}
                <th style={{ textAlign: 'center', padding: '6px 8px', borderBottom: '1px solid #e5e7eb' }}>Total</th>
              </tr>
            </thead>
            <tbody>
              {Object.entries(groupedByDate).map(([date, counts]) => {
                const total = Object.values(counts).reduce((sum, c) => sum + c, 0);
                return (
                  <tr key={date}>
                    <td style={{ padding: '4px 8px', borderBottom: '1px solid #f3f4f6' }}>
                      {new Date(date).toLocaleDateString(undefined, { month: 'short', day: 'numeric' })}
                    </td>
                    {groupKeys.map(key => (
                      <td key={key} style={{ textAlign: 'center', padding: '4px 8px', borderBottom: '1px solid #f3f4f6' }}>
                        {counts[key] || 0}
                      </td>
                    ))}
                    <td style={{ textAlign: 'center', padding: '4px 8px', borderBottom: '1px solid #f3f4f6', fontWeight: 'bold' }}>
                      {total}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

export default CrashTrendChart;
