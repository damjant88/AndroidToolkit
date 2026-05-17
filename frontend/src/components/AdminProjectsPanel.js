import React, { useState, useEffect, useCallback } from 'react';
import { projectApi } from '../api/projectApi';

function AdminProjectsPanel() {
  const [projects, setProjects] = useState([]);
  const [name, setName] = useState('');
  const [remoteApkLocation, setRemoteApkLocation] = useState('');
  const [localApkFolder, setLocalApkFolder] = useState('');
  const [localLogFolder, setLocalLogFolder] = useState('');
  const [error, setError] = useState('');
  const [editingId, setEditingId] = useState(null);
  const [editName, setEditName] = useState('');
  const [editRemoteApkLocation, setEditRemoteApkLocation] = useState('');
  const [editLocalApkFolder, setEditLocalApkFolder] = useState('');
  const [editLocalLogFolder, setEditLocalLogFolder] = useState('');

  const fetchProjects = useCallback(async () => {
    try {
      const res = await projectApi.list();
      setProjects(res.data);
    } catch {} // eslint-disable-line no-empty
  }, []);

  useEffect(() => { fetchProjects(); }, [fetchProjects]);

  async function handleCreate(e) {
    e.preventDefault();
    if (!name.trim() || !remoteApkLocation.trim() || !localApkFolder.trim() || !localLogFolder.trim()) return;
    setError('');
    try {
      await projectApi.create({ name: name.trim(), remoteApkLocation: remoteApkLocation.trim(), localApkFolder: localApkFolder.trim(), localLogFolder: localLogFolder.trim() });
      setName('');
      setRemoteApkLocation('');
      setLocalApkFolder('');
      setLocalLogFolder('');
      fetchProjects();
    } catch (err) {
      const msg = err.response?.data?.message || err.response?.data?.error || err.message;
      setError('Error: ' + msg);
    }
  }

  function handleEditClick(project) {
    setEditingId(project.id);
    setEditName(project.name);
    setEditRemoteApkLocation(project.remoteApkLocation);
    setEditLocalApkFolder(project.localApkFolder);
    setEditLocalLogFolder(project.localLogFolder);
    setError('');
  }

  function handleCancelEdit() {
    setEditingId(null);
    setError('');
  }

  async function handleUpdate(e) {
    e.preventDefault();
    if (!editName.trim() || !editRemoteApkLocation.trim() || !editLocalApkFolder.trim() || !editLocalLogFolder.trim()) return;
    setError('');
    try {
      await projectApi.update(editingId, { name: editName.trim(), remoteApkLocation: editRemoteApkLocation.trim(), localApkFolder: editLocalApkFolder.trim(), localLogFolder: editLocalLogFolder.trim() });
      setEditingId(null);
      fetchProjects();
    } catch (err) {
      const msg = err.response?.data?.message || err.response?.data?.error || err.message;
      setError('Error: ' + msg);
    }
  }

  async function handleDelete(project) {
    if (!window.confirm(`Delete project "${project.name}"?`)) return;
    setError('');
    try {
      await projectApi.delete(project.id);
      fetchProjects();
    } catch (err) {
      const msg = err.response?.data?.message || err.response?.data?.error || err.message;
      setError('Error: ' + msg);
    }
  }

  return (
    <div className="admin-projects-panel">
      <h4>Manage Projects</h4>
      <form onSubmit={handleCreate} className="project-form">
        <input
          type="text"
          placeholder="Project name"
          value={name}
          onChange={e => setName(e.target.value)}
        />
        <input
          type="text"
          placeholder="Remote APK location"
          value={remoteApkLocation}
          onChange={e => setRemoteApkLocation(e.target.value)}
        />
        <input
          type="text"
          placeholder="Local APK folder"
          value={localApkFolder}
          onChange={e => setLocalApkFolder(e.target.value)}
        />
        <input
          type="text"
          placeholder="Local Log Folder"
          value={localLogFolder}
          onChange={e => setLocalLogFolder(e.target.value)}
        />
        <button type="submit">Create</button>
      </form>
      {error && <p className="project-error">{error}</p>}
      {projects.length === 0 ? (
        <p className="project-empty">No projects configured</p>
      ) : (
        <table className="project-table">
          <thead>
            <tr>
              <th>Name</th>
              <th>Remote APK Location</th>
              <th>Local APK Folder</th>
              <th>Local Log Folder</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {projects.map(p => (
              editingId === p.id ? (
                <tr key={p.id}>
                  <td>
                    <input type="text" value={editName} onChange={e => setEditName(e.target.value)} />
                  </td>
                  <td>
                    <input type="text" value={editRemoteApkLocation} onChange={e => setEditRemoteApkLocation(e.target.value)} />
                  </td>
                  <td>
                    <input type="text" value={editLocalApkFolder} onChange={e => setEditLocalApkFolder(e.target.value)} />
                  </td>
                  <td>
                    <input type="text" value={editLocalLogFolder} onChange={e => setEditLocalLogFolder(e.target.value)} />
                  </td>
                  <td>
                    <button onClick={handleUpdate}>Save</button>
                    <button onClick={handleCancelEdit}>Cancel</button>
                  </td>
                </tr>
              ) : (
                <tr key={p.id}>
                  <td>{p.name}</td>
                  <td>{p.remoteApkLocation}</td>
                  <td>{p.localApkFolder}</td>
                  <td>{p.localLogFolder}</td>
                  <td>
                    <button onClick={() => handleEditClick(p)}>Edit</button>
                    <button onClick={() => handleDelete(p)}>Delete</button>
                  </td>
                </tr>
              )
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}

export default AdminProjectsPanel;
