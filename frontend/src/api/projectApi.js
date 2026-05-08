import axios from 'axios';

const api = axios.create();

api.interceptors.request.use(config => {
  const token = localStorage.getItem('accessToken');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

api.interceptors.response.use(r => r, error => {
  return Promise.reject(error);
});

export const projectApi = {
  list: () => api.get('/api/projects'),
  getById: (id) => api.get(`/api/projects/${id}`),
  create: (data) => api.post('/api/projects', data),
  update: (id, data) => api.put(`/api/projects/${id}`, data),
  delete: (id) => api.delete(`/api/projects/${id}`),
  getMyOverride: (projectId) => api.get(`/api/projects/${projectId}/overrides/me`),
  setMyOverride: (projectId, data) => api.put(`/api/projects/${projectId}/overrides/me`, data),
  deleteMyOverride: (projectId) => api.delete(`/api/projects/${projectId}/overrides/me`),
  getResolved: (projectId) => api.get(`/api/projects/${projectId}/resolved`),
};
