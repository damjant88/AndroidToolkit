import React, { useState, useEffect, useCallback } from 'react';
import axios from 'axios';

const api = axios.create({ baseURL: '/api/access' });
api.interceptors.request.use(config => {
  const token = localStorage.getItem('accessToken');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// Add response interceptor to log actual errors
api.interceptors.response.use(r => r, error => {
  return Promise.reject(error);
});

function AllowedUsersPanel() {
  const [grants, setGrants] = useState([]);
  const [email, setEmail] = useState('');
  const [tier, setTier] = useState('BASIC');
  const [message, setMessage] = useState('');

  const fetchGrants = useCallback(async () => {
    try {
      const res = await api.get('/grants');
      setGrants(res.data);
    } catch {} // eslint-disable-line no-empty
  }, []);

  useEffect(() => { fetchGrants(); }, [fetchGrants]);

  async function handleGrant(e) {
    e.preventDefault();
    if (!email.trim()) return;
    setMessage('');
    try {
      const res = await api.post('/grant', { email: email.trim(), tier });
      setMessage(res.data.message);
      setEmail('');
      setTier('BASIC');
      fetchGrants();
    } catch (err) {
      const msg = err.response?.data?.message || err.response?.data?.error || err.message;
      setMessage('Error: ' + msg + (err.response?.status ? ` (${err.response.status})` : ''));
    }
  }

  async function handleRevoke(userEmail) {
    try {
      await api.post('/revoke', { email: userEmail });
      fetchGrants();
    } catch (err) {
      setMessage(err.response?.data?.message || err.message);
    }
  }

  return (
    <div className="allowed-users-panel">
      <h4>Allowed Users</h4>
      <form onSubmit={handleGrant} className="grant-form">
        <input
          type="email"
          placeholder="Email to grant access"
          value={email}
          onChange={e => setEmail(e.target.value)}
        />
        <select value={tier} onChange={e => setTier(e.target.value)}>
          <option value="BASIC">Basic</option>
          <option value="ADVANCED">Advanced</option>
        </select>
        <button type="submit">Grant</button>
      </form>
      {message && <p className="grant-message">{message}</p>}
      {grants.length === 0 ? (
        <p className="grant-empty">No users have access to your devices</p>
      ) : (
        <ul className="grant-list">
          {grants.map(g => (
            <li key={g.email}>
              <span>{g.status === 'pending' ? '⏳' : '✅'} {g.username !== '(pending)' ? g.username + ' ' : ''}{g.email} ({g.tier})</span>
              <button onClick={() => handleRevoke(g.email)}>Revoke</button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

export default AllowedUsersPanel;
