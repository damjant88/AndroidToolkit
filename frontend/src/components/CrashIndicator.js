import React, { useState, useEffect, useCallback } from 'react';
import { useCrashWebSocket } from '../api/useCrashWebSocket';
import { crashApi } from '../api/crashApi';

/**
 * CrashIndicator component renders on DeviceCard when crashes are detected.
 * Shows a red highlight border, crash badge with type/timestamp, and unacknowledged count.
 * Highlight persists for 30 seconds or until acknowledged.
 */
function CrashIndicator({ deviceSerial, projectId, onNavigateToCrash }) {
  const { crashes, acknowledge } = useCrashWebSocket(deviceSerial);
  const [highlighted, setHighlighted] = useState(false);
  const [highlightTimer, setHighlightTimer] = useState(null);

  // Trigger highlight on new crash
  useEffect(() => {
    if (crashes.length > 0) {
      setHighlighted(true);

      // Request browser notification
      sendBrowserNotification(crashes[0]);

      // Clear highlight after 30 seconds
      if (highlightTimer) clearTimeout(highlightTimer);
      const timer = setTimeout(() => setHighlighted(false), 30000);
      setHighlightTimer(timer);
    }
    return () => {
      if (highlightTimer) clearTimeout(highlightTimer);
    };
  }, [crashes.length]); // eslint-disable-line react-hooks/exhaustive-deps

  const handleAcknowledge = useCallback(async (crashId) => {
    try {
      await crashApi.acknowledgeCrash(projectId, crashId);
      acknowledge(crashId);
      if (crashes.length <= 1) {
        setHighlighted(false);
      }
    } catch (err) {
      console.error('Failed to acknowledge crash:', err);
    }
  }, [projectId, acknowledge, crashes.length]);

  const handleBadgeClick = useCallback((crashId) => {
    if (onNavigateToCrash) {
      onNavigateToCrash(crashId);
    }
  }, [onNavigateToCrash]);

  if (crashes.length === 0) return null;

  const latestCrash = crashes[0];

  return (
    <div
      className={`crash-indicator ${highlighted ? 'crash-highlighted' : ''}`}
      style={{
        border: highlighted ? '2px solid #ef4444' : 'none',
        borderRadius: '8px',
        padding: '4px',
        transition: 'border-color 0.3s ease',
      }}
    >
      {/* Crash count badge */}
      <div
        className="crash-badge"
        onClick={() => handleBadgeClick(latestCrash.id)}
        style={{
          display: 'inline-flex',
          alignItems: 'center',
          gap: '6px',
          background: '#fef2f2',
          border: '1px solid #fecaca',
          borderRadius: '6px',
          padding: '4px 8px',
          cursor: 'pointer',
          fontSize: '12px',
        }}
        role="button"
        aria-label={`${crashes.length} unacknowledged crash${crashes.length > 1 ? 'es' : ''}`}
        tabIndex={0}
        onKeyDown={(e) => { if (e.key === 'Enter') handleBadgeClick(latestCrash.id); }}
      >
        <span style={{ color: '#dc2626', fontWeight: 'bold' }}>⚠</span>
        <span style={{ color: '#991b1b' }}>
          {latestCrash.crashType} • {new Date(latestCrash.timestamp).toLocaleTimeString()}
        </span>
        {crashes.length > 1 && (
          <span
            style={{
              background: '#dc2626',
              color: 'white',
              borderRadius: '50%',
              width: '18px',
              height: '18px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: '10px',
              fontWeight: 'bold',
            }}
          >
            {crashes.length}
          </span>
        )}
        <button
          onClick={(e) => { e.stopPropagation(); handleAcknowledge(latestCrash.id); }}
          style={{
            background: 'none',
            border: 'none',
            cursor: 'pointer',
            fontSize: '14px',
            color: '#6b7280',
            padding: '0 2px',
          }}
          aria-label="Acknowledge crash"
          title="Acknowledge"
        >
          ✕
        </button>
      </div>
    </div>
  );
}

/**
 * Sends a browser notification for a crash event.
 * Falls back to console log if notifications are not supported or denied.
 */
function sendBrowserNotification(crashEvent) {
  if (!('Notification' in window)) return;

  if (Notification.permission === 'granted') {
    const notification = new Notification('Crash Detected', {
      body: `${crashEvent.deviceName || crashEvent.deviceSerial}: ${crashEvent.crashType} (${crashEvent.packageName})`,
      icon: '/favicon.ico',
      tag: `crash-${crashEvent.id}`,
    });
    notification.onclick = () => {
      window.focus();
      notification.close();
    };
  } else if (Notification.permission !== 'denied') {
    Notification.requestPermission();
  }
}

export default CrashIndicator;
