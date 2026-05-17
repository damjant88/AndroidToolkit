import React, { useState, useEffect, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import axios from 'axios';

function EventTracker({ serial, onClose }) {
  const [keyword, setKeyword] = useState('');
  const [tracking, setTracking] = useState(false);
  const [events, setEvents] = useState([]);
  const [selectedEvent, setSelectedEvent] = useState(null);
  const clientRef = useRef(null);

  useEffect(() => {
    // Subscribe to event matches via WebSocket
    const client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      reconnectDelay: 5000,
      onConnect: () => {
        client.subscribe(`/topic/events/${serial}`, (message) => {
          const event = JSON.parse(message.body);
          setEvents(prev => [...prev, event]);
        });
      },
    });
    client.activate();
    clientRef.current = client;

    return () => {
      if (clientRef.current) clientRef.current.deactivate();
      // Stop tracking on unmount
      axios.post(`/api/devices/${encodeURIComponent(serial)}/stop-tracking`).catch(() => {});
    };
  }, [serial]);

  async function handleStartTracking() {
    if (!keyword.trim()) return;
    try {
      await axios.post(`/api/devices/${encodeURIComponent(serial)}/track-event`, { keyword: keyword.trim() });
      setTracking(true);
      setEvents([]);
      setSelectedEvent(null);
    } catch (err) {
      alert('Failed to start tracking: ' + (err.response?.data?.message || err.message));
    }
  }

  async function handleStopTracking() {
    try {
      await axios.post(`/api/devices/${encodeURIComponent(serial)}/stop-tracking`);
      setTracking(false);
    } catch (err) {
      // ignore
    }
  }

  return (
    <div className="event-tracker-overlay">
      <div className="event-tracker-dialog">
        <div className="event-tracker-header">
          <h3>Event Tracker — {serial}</h3>
          <button className="close-btn" onClick={onClose}>✕</button>
        </div>

        <div className="event-tracker-controls">
          <input
            type="text"
            placeholder="Enter event string to track..."
            value={keyword}
            onChange={e => setKeyword(e.target.value)}
            onKeyDown={e => e.key === 'Enter' && !tracking && handleStartTracking()}
            disabled={tracking}
          />
          {!tracking ? (
            <button onClick={handleStartTracking} disabled={!keyword.trim()}>Start Tracking</button>
          ) : (
            <button onClick={handleStopTracking} className="stop-btn">Stop</button>
          )}
        </div>

        {tracking && <p className="tracking-status">🔴 Tracking: "{keyword}"</p>}

        <div className="event-matches">
          {events.length === 0 && tracking && <p className="event-empty">Waiting for matches...</p>}
          {events.map((event, idx) => (
            <div
              key={idx}
              className={`event-match-line ${selectedEvent === idx ? 'selected' : ''}`}
              onClick={() => setSelectedEvent(selectedEvent === idx ? null : idx)}
            >
              <span className="event-match-text">{event.matchedLine}</span>
              <span className="event-match-time">{new Date(event.timestamp).toLocaleTimeString()}</span>
            </div>
          ))}
        </div>

        {selectedEvent !== null && events[selectedEvent] && (
          <div className="event-context">
            <h4>Context ({events[selectedEvent].contextLines.length} lines)</h4>
            <pre className="event-context-lines">
              {events[selectedEvent].contextLines.map((line, i) => (
                <div key={i} className={line === events[selectedEvent].matchedLine ? 'highlighted-line' : ''}>
                  {line}
                </div>
              ))}
            </pre>
          </div>
        )}
      </div>
    </div>
  );
}

export default EventTracker;
