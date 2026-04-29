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

export async function pullLogs(serial) {
  const response = await api.post(`/devices/${serial}/pull-logs`);
  return response.data;
}

export async function downloadLogs(deviceName) {
  const response = await api.get(`/files/logs/${deviceName}/download`, {
    responseType: 'blob',
  });
  return response.data;
}

export async function startScreenMirror(serial) {
  const response = await api.post(`/devices/${serial}/screen-mirror`);
  return response.data;
}

export async function startRecording(serial) {
  const response = await api.post(`/devices/${serial}/start-recording`);
  return response.data;
}

export async function stopRecording(serial, pid) {
  const response = await api.post(`/devices/${serial}/stop-recording`, { pid });
  return response.data;
}

export async function downloadRecordingFile(recordingPath, fileName) {
  const params = new URLSearchParams({ path: recordingPath, fileName });
  const response = await api.get(`/files/recording/download?${params.toString()}`, {
    responseType: 'blob',
  });
  return response.data;
}

export async function openFolder(folderPath) {
  const response = await api.post('/files/open-folder', null, {
    params: { path: folderPath.replace(/\\/g, '/') }
  });
  return response.data;
}

export async function takeScreenshot(serial, deviceName) {
  const response = await api.post(`/devices/${serial}/screenshot`, { deviceName });
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
