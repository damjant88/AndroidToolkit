import axios from 'axios';

const api = axios.create();

api.interceptors.request.use(config => {
  const token = localStorage.getItem('accessToken');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

api.interceptors.response.use(r => r, error => Promise.reject(error));

export const crashApi = {
  // Crash history
  getCrashes: (projectId, params = {}) =>
    api.get(`/api/projects/${projectId}/crashes`, { params }),
  getCrashDetail: (projectId, crashId) =>
    api.get(`/api/projects/${projectId}/crashes/${crashId}`),
  downloadCrashLog: (projectId, crashId) =>
    api.get(`/api/projects/${projectId}/crashes/${crashId}/log`, { responseType: 'blob' }),
  acknowledgeCrash: (projectId, crashId) =>
    api.post(`/api/projects/${projectId}/crashes/${crashId}/acknowledge`),

  // Crash trends
  getCrashTrends: (projectId, params = {}) =>
    api.get(`/api/projects/${projectId}/crash-trends`, { params }),

  // Alert configuration
  getAlertConfig: (projectId) =>
    api.get(`/api/projects/${projectId}/alert-config`),
  updateAlertConfig: (projectId, data) =>
    api.put(`/api/projects/${projectId}/alert-config`, data),

  // Custom patterns
  getPatterns: (projectId) =>
    api.get(`/api/projects/${projectId}/alert-config/patterns`),
  addPattern: (projectId, data) =>
    api.post(`/api/projects/${projectId}/alert-config/patterns`, data),
  removePattern: (projectId, patternId) =>
    api.delete(`/api/projects/${projectId}/alert-config/patterns/${patternId}`),
  updatePattern: (projectId, patternId, data) =>
    api.put(`/api/projects/${projectId}/alert-config/patterns/${patternId}`, data),
  validateRegex: (projectId, regex) =>
    api.post(`/api/projects/${projectId}/alert-config/validate-regex`, regex, {
      headers: { 'Content-Type': 'text/plain' }
    }),
};
