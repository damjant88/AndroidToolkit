import axios from 'axios';

const api = axios.create({ baseURL: '/api/auth' });

function getDeviceFingerprint() {
  let fp = localStorage.getItem('deviceFingerprint');
  if (!fp) {
    fp = crypto.randomUUID();
    localStorage.setItem('deviceFingerprint', fp);
  }
  return fp;
}

export async function login(username, password, rememberMe) {
  const response = await api.post('/login', {
    username, password, rememberMe: String(rememberMe),
    deviceFingerprint: getDeviceFingerprint()
  });
  return response.data;
}

export async function register(username, email, password) {
  const response = await api.post('/register', { username, email, password });
  return response.data;
}

export async function refreshToken(token) {
  const response = await api.post('/refresh', {
    refreshToken: token,
    deviceFingerprint: getDeviceFingerprint()
  });
  return response.data;
}

export async function logout(token) {
  await api.post('/logout', { refreshToken: token });
}
