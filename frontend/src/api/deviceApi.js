import axios from 'axios';

const api = axios.create({
  baseURL: '/api',
});

// Agent API — direct connection for local device operations (fast, no relay)
const agentApi = axios.create({
  baseURL: 'http://localhost:8081/api/agent',
});

// Attach JWT token to backend requests
api.interceptors.request.use(config => {
  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Logout on 401 (token expired)
api.interceptors.response.use(r => r, error => {
  if (error.response && error.response.status === 401) {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('username');
    window.location.href = '/';
  }
  return Promise.reject(error);
});

export async function getDevices() {
  const response = await api.get('/devices');
  return response.data;
}

// --- Device operations go directly to the local agent (fast path) ---

export async function rebootDevice(serial) {
  const response = await agentApi.post(`/devices/${encodeURIComponent(serial)}/reboot`);
  return response.data;
}

export async function uninstallApp(serial, packageName) {
  const response = await agentApi.post(`/devices/${encodeURIComponent(serial)}/uninstall`, { packageName });
  return response.data;
}

export async function enableFirebaseDebug(serial, packageName) {
  const response = await agentApi.post(`/devices/${encodeURIComponent(serial)}/firebase-debug`, { packageName });
  return response.data;
}

export async function toggleWifiDebug(serial, ipAddress, wifiDebugSession, hasWifiIp) {
  const response = await agentApi.post(`/devices/${encodeURIComponent(serial)}/wifi-debug`, {
    ipAddress,
    wifiDebugSession,
    hasWifiIp,
  });
  return response.data;
}

export async function pullLogs(serial) {
  const response = await agentApi.post(`/devices/${encodeURIComponent(serial)}/pull-logs`);
  return response.data;
}

export async function startScreenMirror(serial) {
  const response = await agentApi.post(`/devices/${encodeURIComponent(serial)}/screen-mirror`);
  return response.data;
}

export async function startRecording(serial) {
  const response = await agentApi.post(`/devices/${encodeURIComponent(serial)}/start-recording`);
  return response.data;
}

export async function stopRecording(serial, pid) {
  const response = await agentApi.post(`/devices/${encodeURIComponent(serial)}/stop-recording`, { pid });
  return response.data;
}

export async function takeScreenshot(serial, deviceName) {
  const response = await agentApi.post(`/devices/${encodeURIComponent(serial)}/screenshot`, null, {
    responseType: 'blob'
  });
  return { imageBlob: response.data, deviceName };
}

export async function openFolder(folderPath) {
  const response = await agentApi.post('/devices/open-folder', null, {
    params: { path: folderPath.replace(/\\/g, '/') }
  });
  return response.data;
}

export async function getPermissions(serial, packageName) {
  const response = await agentApi.get(`/devices/${encodeURIComponent(serial)}/permissions?packageName=${encodeURIComponent(packageName)}`);
  return response.data;
}

export async function enablePermissions(serial, packageName, permissionIds) {
  const response = await agentApi.post(`/devices/${encodeURIComponent(serial)}/permissions/enable`, { packageName, permissionIds });
  return response.data;
}

export async function disablePermissions(serial, packageName, permissionIds) {
  const response = await agentApi.post(`/devices/${encodeURIComponent(serial)}/permissions/disable`, { packageName, permissionIds });
  return response.data;
}

export async function installBuild(serial, apkPath) {
  const response = await api.post(`/builds/install/${encodeURIComponent(serial)}?path=${encodeURIComponent(apkPath)}`);
  return response.data;
}

export async function uploadBuild(file) {
  const formData = new FormData();
  formData.append('file', file);
  const response = await agentApi.post('/devices/upload-apk', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000
  });
  return response.data;
}

export async function startInstallJob(apkPath, serials) {
  const results = {};
  for (const serial of serials) {
    try {
      const res = await agentApi.post(`/devices/${encodeURIComponent(serial)}/install`, { apkPath });
      results[serial] = { serial, success: res.data.success, message: res.data.message };
    } catch (err) {
      results[serial] = { serial, success: false, message: err.message };
    }
  }
  return {
    jobId: 'direct-' + Date.now(),
    status: 'COMPLETED',
    totalCount: serials.length,
    completedCount: serials.length,
    deviceResults: results
  };
}

export async function startUninstallJob(serials, devices) {
  const results = {};
  for (const serial of serials) {
    try {
      const device = devices ? devices.find(d => d.deviceInfo.serialNumber === serial) : null;
      const packageName = device?.deviceInfo?.safePathPackage || '';
      if (!packageName) {
        results[serial] = { serial, success: false, message: 'No package to uninstall' };
        continue;
      }
      const res = await agentApi.post(`/devices/${encodeURIComponent(serial)}/uninstall`, { packageName });
      results[serial] = { serial, success: res.data.success, message: res.data.message };
    } catch (err) {
      results[serial] = { serial, success: false, message: err.message };
    }
  }
  return {
    jobId: 'direct-' + Date.now(),
    status: 'COMPLETED',
    totalCount: serials.length,
    completedCount: serials.length,
    deviceResults: results
  };
}

export async function getJob(jobId) {
  if (jobId.startsWith('direct-')) {
    return { status: 'COMPLETED' };
  }
  const response = await api.get(`/jobs/${jobId}`);
  return response.data;
}

export async function getRecentJobs() {
  const response = await api.get('/jobs');
  return response.data;
}

export async function setMockLocation(serial, lat, lng, start = true) {
  const response = await agentApi.post(`/devices/${encodeURIComponent(serial)}/mock-location`, { lat, lng, start });
  return response.data;
}

export async function getDeviceLocation(serial) {
  const response = await agentApi.get(`/devices/${encodeURIComponent(serial)}/location`);
  return response.data;
}
