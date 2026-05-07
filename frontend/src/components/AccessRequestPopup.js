import React from 'react';
import axios from 'axios';

const api = axios.create({ baseURL: '/api/access' });
api.interceptors.request.use(config => {
  const token = localStorage.getItem('accessToken');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

function AccessRequestPopup({ requestId, fromUser, onDismiss }) {
  async function handleRespond(approved) {
    try {
      await api.post('/respond', { requestId, approved });
    } catch {} // eslint-disable-line no-empty
    onDismiss();
  }

  return (
    <div className="access-request-overlay">
      <div className="access-request-dialog">
        <h3>Access Request</h3>
        <p>Allow user <strong>{fromUser}</strong> to control your devices?</p>
        <div className="access-request-actions">
          <button className="access-btn-yes" onClick={() => handleRespond(true)}>Yes</button>
          <button className="access-btn-no" onClick={() => handleRespond(false)}>No</button>
        </div>
      </div>
    </div>
  );
}

export default AccessRequestPopup;
