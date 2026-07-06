import axios from 'axios';

const api = axios.create();

api.interceptors.request.use(config => {
  const token = localStorage.getItem('accessToken');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

api.interceptors.response.use(r => r, error => {
  if (error.response && error.response.status === 401) {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('username');
    window.location.href = '/';
  }
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
  getConfluenceArtifacts: (idOrParam) => {
    if (typeof idOrParam === 'number' || (typeof idOrParam === 'string' && idOrParam.startsWith('projectId:'))) {
      const id = typeof idOrParam === 'number' ? idOrParam : idOrParam.substring(10);
      return api.get(`/api/confluence/artifacts?projectId=${id}`);
    }
    if (idOrParam.startsWith('page:')) {
      return api.get(`/api/confluence/artifacts?pageId=${idOrParam.substring(5)}`);
    }
    return api.get(`/api/confluence/artifacts?parentId=${idOrParam}`);
  },
};
