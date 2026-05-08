import React, { useState, useEffect, useCallback } from 'react';
import { projectApi } from '../api/projectApi';

function UserProjectsPanel() {
  const [projects, setProjects] = useState([]);
  const [localPaths, setLocalPaths] = useState({});
  const [message, setMessage] = useState('');

  const fetchProjects = useCallback(async () => {
    try {
      const res = await projectApi.list();
      const projectList = res.data;
      const resolved = await Promise.all(
        projectList.map(p => projectApi.getResolved(p.id).then(r => r.data))
      );
      setProjects(resolved);
      const paths = {};
      resolved.forEach(p => { paths[p.id] = p.localApkFolder; });
      setLocalPaths(paths);
    } catch {
      setMessage('Failed to load projects');
    }
  }, []);

  useEffect(() => { fetchProjects(); }, [fetchProjects]);

  function handlePathChange(projectId, value) {
    setLocalPaths(prev => ({ ...prev, [projectId]: value }));
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
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

export default UserProjectsPanel;
