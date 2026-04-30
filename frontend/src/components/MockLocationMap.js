import React, { useState, useEffect } from 'react';
import { MapContainer, TileLayer, Marker, useMap, useMapEvents } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { getDeviceLocation } from '../api/deviceApi';

delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
  iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
});

const blueIcon = new L.Icon({
  iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-blue.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
  shadowSize: [41, 41]
});

function LocationPicker({ position, setPosition }) {
  useMapEvents({
    click(e) {
      setPosition([e.latlng.lat, e.latlng.lng]);
    },
  });
  return position ? <Marker position={position} /> : null;
}

function FlyToLocation({ center }) {
  const map = useMap();
  useEffect(() => {
    if (center) map.flyTo(center, 14);
  }, [center, map]);
  return null;
}

function MockLocationMap({ serial, onClose, onSetLocation, onAddressResolved, mocking, onMockingChange }) {
  const [position, setPosition] = useState(null);
  const [deviceCenter, setDeviceCenter] = useState(null);
  const [sending, setSending] = useState(false);
  const [message, setMessage] = useState('');
  const [address, setAddress] = useState('');

  useEffect(() => {
    getDeviceLocation(serial)
      .then(data => {
        if (data.lat && data.lng) {
          setDeviceCenter([data.lat, data.lng]);
        }
      })
      .catch(() => {});
  }, [serial]);

  useEffect(() => {
    if (!position) return;
    setAddress('Loading...');
    fetch(`https://nominatim.openstreetmap.org/reverse?lat=${position[0]}&lon=${position[1]}&format=json&addressdetails=1`)
      .then(r => r.json())
      .then(data => {
        const a = data.address || {};
        const parts = [a.road, a.city || a.town || a.village, a.state].filter(Boolean);
        setAddress(parts.join(', ') || data.display_name || '');
      })
      .catch(() => setAddress(''));
  }, [position]);

  async function handleStart() {
    if (!position) return;
    setSending(true);
    setMessage('');
    try {
      const result = await onSetLocation(serial, position[0], position[1], true);
      setMessage(result.message);
      if (onMockingChange) onMockingChange(true);
      if (onAddressResolved && address && address !== 'Loading...') {
        onAddressResolved(address);
      }
    } catch (err) {
      setMessage('\u274c ' + (err.response?.data?.message || err.message));
    } finally {
      setSending(false);
    }
  }

  async function handleStop() {
    setSending(true);
    setMessage('');
    try {
      const result = await onSetLocation(serial, 0, 0, false);
      setMessage(result.message);
      if (onMockingChange) onMockingChange(false);
    } catch (err) {
      setMessage('\u274c ' + (err.response?.data?.message || err.message));
    } finally {
      setSending(false);
    }
  }

  return (
    <div className="permissions-overlay" onClick={onClose}>
      <div className="mock-location-dialog" onClick={e => e.stopPropagation()}>
        <div className="permissions-header">
          <h3>{'\ud83d\udccd'} Mock Location</h3>
          <button className="close-btn" onClick={onClose}>{'\u2715'}</button>
        </div>
        <p style={{ fontSize: '0.85rem', marginBottom: 8, color: '#666' }}>
          Click on the map to select a location, then click Start Mocking.
        </p>
        <div className="map-container">
          <MapContainer center={[37.7749, -122.4194]} zoom={4} style={{ height: '100%', width: '100%' }}>
            <TileLayer
              attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
              url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            />
            <LocationPicker position={position} setPosition={setPosition} />
            {deviceCenter && <Marker position={deviceCenter} icon={blueIcon} />}
            <FlyToLocation center={deviceCenter} />
          </MapContainer>
        </div>
        {position && (
          <div style={{ fontSize: '0.8rem', marginTop: 8, color: '#333' }}>
            <p>Selected: {position[0].toFixed(6)}, {position[1].toFixed(6)}</p>
            {address && <p>{'\ud83d\udccd'} {address}</p>}
          </div>
        )}
        <div className="permissions-actions">
          {!mocking ? (
            <button disabled={!position || sending} onClick={handleStart}>
              {sending ? '\u23f3 Starting...' : '\u25b6 Start Mocking'}
            </button>
          ) : (
            <button disabled={sending} onClick={handleStop} style={{ background: 'linear-gradient(to bottom, #ff6666, #cc0000)', color: 'white', borderColor: '#cc0000' }}>
              {sending ? '\u23f3 Stopping...' : '\u23f9 Stop Mocking'}
            </button>
          )}
        </div>
        {message && <p className="permissions-message">{message}</p>}
      </div>
    </div>
  );
}

export default MockLocationMap;
