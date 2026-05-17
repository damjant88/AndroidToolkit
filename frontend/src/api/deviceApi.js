import axios from 'axios';

const api = axios.create({
  baseURL: '/api',
});

// Agent API — direct connection for device operations (screenshots, scrcpy, etc.)
// In SaaS mode, the agent runs locally and handles all adb operations directly.
const AGENT_URL = localStorage.getItem('agentUrl') || 'http://localhost:8082';
const agentApi = axios.create({
  baseURL: AGENT_URL + '/api/agent',
});

// Attach JWT token to backend requests
api.interceptors.request.use(config => {
  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export async function getDevices() {
  const response = await api.get('/devices');
  return response.data;
}

// --- Device operations go directly to the agent ---

export async function rebootDevice(serial) {
  const response = await agentApi.post(`/devices/${serial}/reboot`);
  return response.data;
}

export async function uninstallApp(serial, packageName) {
  const response = await agentApi.post(`/devices/${serial}/uninstall`, { packageName });
  return response.data;
}

export async function enableFirebaseDebug(serial, packageName) {
  const response = await agentApi.post(`/devices/${serial}/firebase-debug`, { packageName });
  return response.data;
}

export async function toggleWifiDebug(serial, ipAddress, wifiDebugSession, hasWifiIp) {
  const response = await agentApi.post(`/devices/${serial}/wifi-debug`, {
    ipAddress,
    wifiDebugSession,
    hasWifiIp,
  });
  return response.data;
}

export async function pullLogs(serial) {
  const response = await agentApi.post(`/devices/${serial}/pull-logs`);
  return response.data;
}

export async function startScreenMirror(serial) {
  const response = await agentApi.post(`/devices/${serial}/screen-mirror`);
  return response.data;
}

export async function startRecording(serial) {
  const response = await agentApi.post(`/devices/${serial}/start-recording`);
  return response.data;
}

export async function stopRecording(serial, pid) {
  const response = await agentApi.post(`/devices/${serial}/stop-recording`, { pid });
  return response.data;
}

export async function takeScreenshot(serial, deviceName) {
  // Returns the image URL directly from the agent
  const response = await agentApi.post(`/devices/${serial}/screenshot`, null, {
    responseType: 'blob'
  });
  return { imageBlob: response.data, deviceName };
}

// --- Backend operations (data, files, jobs) ---

export async function openFolder(folderPath) {
  const response = await agentApi.post('/devices/open-folder', null, {
    params: { path: folderPath.replace(/\\/g, '/') }
  });
  return response.data;
}

export async function getPermissions(serial, packageName) {
  const response = await api.get(`/devices/${serial}/permissions?packageName=${encodeURIComponent(packageName)}`);
  return response.data;
}

export async function enablePermissions(serial, packageName, permissionIds) {
  const response = await api.post(`/devices/${serial}/permissions/enable`, { packageName, permissionIds });
  return response.data;
}

export async function disablePermissions(serial, packageName, permissionIds) {
  const response = await api.post(`/devices/${serial}/permissions/disable`, { packageName, permissionIds });
  return response.data;
}

export async function installBuild(serial, apkPath) {
  const response = await api.post(`/builds/install/${serial}?path=${encodeURIComponent(apkPath)}`);
  return response.data;
}

export async function uploadBuild(file) {
  const formData = new FormData();
  formData.append('file', file);
  const response = await api.post('/builds/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  return response.data;
}

export async function startInstallJob(apkPath, serials) {
  const response = await api.post('/jobs/install', { apkPath, serials });
  return response.data;
}

export async function startUninstallJob(serials) {
  const response = await api.post('/jobs/uninstall', { serials });
  return response.data;
}

export async function getJob(jobId) {
  const response = await api.get(`/jobs/${jobId}`);
  return response.data;
}

export async function getRecentJobs() {
  const response = await api.get('/jobs');
  return response.data;
}

export async function setMockLocation(serial, lat, lng, start = true) {
  const response = await agentApi.post(`/devices/${serial}/mock-location`, { lat, lng, start });
  return response.data;
}

export async function getDeviceLocation(serial) {
  const response = await agentApi.get(`/devices/${serial}/location`);
  return response.data;
}
