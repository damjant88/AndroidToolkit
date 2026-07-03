import React, { useState, useEffect, useCallback } from 'react';
import { projectApi } from '../api/projectApi';

const FALLBACK_PROJECTS = [
  { id: 'sp', name: 'SafePath', remoteApkLocation: '', localApkFolder: '', localLogFolder: '' },
  { id: 'sf', name: 'Secure Family', remoteApkLocation: '', localApkFolder: '', localLogFolder: '' },
  { id: 'saf', name: 'Safe&Found', remoteApkLocation: '', localApkFolder: '', localLogFolder: '' },
  { id: 'fm', name: 'Family Mode', remoteApkLocation: '', localApkFolder: '', localLogFolder: '' },
  { id: 'cci', name: 'CCI', remoteApkLocation: '', localApkFolder: '', localLogFolder: '' },
  { id: 'or', name: 'Orange', remoteApkLocation: '', localApkFolder: '', localLogFolder: '' },
  { id: 'dish', name: 'Dish', remoteApkLocation: '', localApkFolder: '', localLogFolder: '' },
];

function UserProjectsPanel() {
  const [projects, setProjects] = useState([]);
  const [localPaths, setLocalPaths] = useState({});
  const [logPaths, setLogPaths] = useState({});
  const [message, setMessage] = useState('');

  const fetchProjects = useCallback(async () => {
    try {
      const res = await projectApi.list();
      const projectList = res.data;
      if (projectList.length === 0) {
        // No projects in DB — show fallback
        setProjects(FALLBACK_PROJECTS);
        return;
      }
      const resolved = await Promise.all(
        projectList.map(p => projectApi.getResolved(p.id).then(r => r.data))
      );
      setProjects(resolved);
      const paths = {};
      const logs = {};
      resolved.forEach(p => {
        paths[p.id] = p.localApkFolder;
        logs[p.id] = p.localLogFolder;
      });
      setLocalPaths(paths);
      setLogPaths(logs);
    } catch {
      // API failed — show fallback projects
      setProjects(FALLBACK_PROJECTS);
      setMessage('Showing default projects (backend unavailable)');
    }
  }, []);

  useEffect(() => { fetchProjects(); }, [fetchProjects]);

  function handlePathChange(projectId, value) {
    setLocalPaths(prev => ({ ...prev, [projectId]: value }));
  }

  function handleLogPathChange(projectId, value) {
    setLogPaths(prev => ({ ...prev, [projectId]: value }));
  }

  async function handleSave(projectId) {
    setMessage('');
    const value = localPaths[projectId];
    if (!value || !value.trim()) {
      setMessage('Local APK folder cannot be empty');
      return;
    }
    try {
      await projectApi.setMyOverride(projectId, { localApkFolder: value.trim() });
      setMessage('Override saved');
      fetchProjects();
    } catch (err) {
      const msg = err.response?.data?.message || err.response?.data?.error || err.message;
      setMessage('Error: ' + msg);
    }
  }

  async function handleReset(projectId) {
    setMessage('');
    try {
      await projectApi.deleteMyOverride(projectId);
      setMessage('Override removed');
      fetchProjects();
    } catch (err) {
      const msg = err.response?.data?.message || err.response?.data?.error || err.message;
      setMessage('Error: ' + msg);
    }
  }

  async function handleLogSave(projectId) {
    setMessage('');
    const value = logPaths[projectId];
    if (!value || !value.trim()) {
      setMessage('Local log folder cannot be empty');
      return;
    }
    try {
      await projectApi.setMyOverride(projectId, { localLogFolder: value.trim() });
      setMessage('Log folder override saved');
      fetchProjects();
    } catch (err) {
      const msg = err.response?.data?.message || err.response?.data?.error || err.message;
      setMessage('Error: ' + msg);
    }
  }

  async function handleLogReset(projectId) {
    setMessage('');
    try {
      await projectApi.deleteMyOverride(projectId);
      setMessage('Log folder override removed');
      fetchProjects();
    } catch (err) {
      const msg = err.response?.data?.message || err.response?.data?.error || err.message;
      setMessage('Error: ' + msg);
    }
  }

  return (
    <div className="user-projects-panel">
      <h4>Projects</h4>
      {message && <p className="project-message">{message}</p>}
      {projects.length === 0 ? (
        <p className="project-empty">No projects configured</p>
      ) : (
        <ul className="project-list">
          {projects.map(p => (
            <li key={p.id} className="project-item">
              <div className="project-info">
                <span className="project-name">{p.name}</span>
                <span className="project-remote">{p.remoteApkLocation}</span>
                {p.figmaLink && <a href={p.figmaLink} target="_blank" rel="noopener noreferrer" className="project-figma-link">🎨 Figma ↗</a>}
              </div>
              <div className="project-override">
                <input
                  type="text"
                  value={localPaths[p.id] || ''}
                  onChange={e => handlePathChange(p.id, e.target.value)}
                  placeholder="Local APK folder"
                />
                <button onClick={() => handleSave(p.id)}>Save</button>
                <button className="reset-btn" onClick={() => handleReset(p.id)}>Reset</button>
              </div>
              <div className="project-override project-override-log">
                <label className={`override-label${p.overriddenLogFolder ? ' override-active' : ''}`}>
                  {p.overriddenLogFolder ? 'Log Folder (override)' : 'Log Folder'}
                </label>
                <input
                  type="text"
                  value={logPaths[p.id] || ''}
                  onChange={e => handleLogPathChange(p.id, e.target.value)}
                  placeholder="Local Log folder"
                />
                <button onClick={() => handleLogSave(p.id)}>Save</button>
                <button className="reset-btn" onClick={() => handleLogReset(p.id)}>Reset</button>
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

export default UserProjectsPanel;
