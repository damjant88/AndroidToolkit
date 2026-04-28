import axios from 'axios';

const api = axios.create({
  baseURL: '/api',
});

export async function getDevices() {
  const response = await api.get('/devices');
  return response.data;
}

export async function rebootDevice(serial) {
  const response = await api.post(`/devices/${serial}/reboot`);
  return response.data;
}

export async function uninstallApp(serial, packageName) {
  const response = await api.post(`/devices/${serial}/uninstall`, { packageName });
  return response.data;
}

export async function enableFirebaseDebug(serial, packageName) {
  const response = await api.post(`/devices/${serial}/firebase-debug`, { packageName });
  return response.data;
}

export async function toggleWifiDebug(serial, ipAddress, wifiDebugSession, hasWifiIp) {
  const response = await api.post(`/devices/${serial}/wifi-debug`, {
    ipAddress,
    wifiDebugSession,
    hasWifiIp,
  });
  return response.data;
}
