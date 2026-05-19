import React, { useState, useEffect, useCallback } from 'react';
import { projectApi } from '../api/projectApi';

// Static project metadata (icons, packages, colors)
const PROJECT_META = {
  'SafePath': { icon: 'product.png', packages: ['com.smithmicro.safepath.family', 'com.smithmicro.safepath.family.child'], color: '#4CAF50' },
  'Secure Family': { icon: 'att.png', packages: ['com.smithmicro.att.securefamily', 'com.wavemarket.waplauncher', 'com.att.securefamilycompanion'], color: '#2196F3' },
  'Safe&Found': { icon: 'sprint.png', packages: ['com.smithmicro.sprint.safeandfound.test', 'com.sprint.safefound'], color: '#FF9800' },
  'Family Mode': { icon: 'tmo.png', packages: ['com.smithmicro.tmobile.familymode.test', 'com.tmobile.familycontrols'], color: '#E91E63' },
  'CCI': { icon: 'Senior.png', packages: ['com.smithmicro.cci.test', 'com.smithmicro.safepath.family.light', 'com.smithmicro.safepath.family.speakeasy'], color: '#9C27B0' },
  'Orange': { icon: 'toyo.png', packages: ['com.smithmicro.orangespain.test', 'com.orange.es.TuYo'], color: '#FF5722' },
  'Dish': { icon: 'dish.png', packages: ['com.smithmicro.safepath.dish.test', 'com.smithmicro.safepath.dish.kid.test'], color: '#607D8B' },
};

function getMeta(name) {
  return PROJECT_META[name] || { icon: 'Android.png', packages: [], color: '#999' };
}

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
  const [expandedProject, setExpandedProject] = useState(null);
  const [backendAvailable, setBackendAvailable] = useState(true);

  const fetchProjects = useCallback(async () => {
    try {
      const res = await projectApi.list();
      setProjects(res.data);
      setBackendAvailable(true);
    } catch {
      setBackendAvailable(false);
    }
  }, []);

  useEffect(() => { fetchProjects(); }, [fetchProjects]);

  async function handleCreate(e) {
    e.preventDefault();
    if (!name.trim() || !remoteApkLocation.trim() || !localApkFolder.trim() || !localLogFolder.trim()) return;
    setError('');
    try {
      await projectApi.create({ name: name.trim(), remoteApkLocation: remoteApkLocation.trim(), localApkFolder: localApkFolder.trim(), localLogFolder: localLogFolder.trim() });
      setName(''); setRemoteApkLocation(''); setLocalApkFolder(''); setLocalLogFolder('');
      fetchProjects();
    } catch (err) {
      setError('Error: ' + (err.response?.data?.message || err.message));
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

  async function handleUpdate(e) {
    e.preventDefault();
    if (!editName.trim() || !editRemoteApkLocation.trim() || !editLocalApkFolder.trim() || !editLocalLogFolder.trim()) return;
    setError('');
    try {
      await projectApi.update(editingId, { name: editName.trim(), remoteApkLocation: editRemoteApkLocation.trim(), localApkFolder: editLocalApkFolder.trim(), localLogFolder: editLocalLogFolder.trim() });
      setEditingId(null);
      fetchProjects();
    } catch (err) {
      setError('Error: ' + (err.response?.data?.message || err.message));
    }
  }

  async function handleDelete(project) {
    if (!window.confirm(`Delete project "${project.name}"?`)) return;
    try {
      await projectApi.delete(project.id);
      fetchProjects();
    } catch (err) {
      setError('Error: ' + (err.response?.data?.message || err.message));
    }
  }

  // Merge static metadata with dynamic projects from backend
  const allProjects = projects.length > 0
    ? projects.map(p => ({ ...p, ...getMeta(p.name) }))
    : Object.entries(PROJECT_META).map(([pName, meta]) => ({
        id: pName, name: pName, remoteApkLocation: '', localApkFolder: '', localLogFolder: '', ...meta
      }));

  return (
    <div className="admin-projects-panel">
      <h4>Projects</h4>

      {/* Project cards with icons */}
      <div className="project-cards-grid">
        {allProjects.map(p => (
          <div
            key={p.id}
            className={`project-card ${expandedProject === p.id ? 'expanded' : ''}`}
            style={{ borderLeftColor: p.color }}
            onClick={() => setExpandedProject(expandedProject === p.id ? null : p.id)}
          >
            <div className="project-card-header">
              <img src={`/icons/${p.icon}`} alt={p.name} className="project-card-icon" />
              <span className="project-card-name">{p.name}</span>
            </div>
            {expandedProject === p.id && (
              <div className="project-card-details">
                <div className="project-card-section">
                  <strong>📦 Packages:</strong>
                  <ul>{p.packages.map(pkg => <li key={pkg}><code>{pkg}</code></li>)}</ul>
                </div>
                <div className="project-card-section">
                  <strong>📡 Remote APK:</strong> <span>{p.remoteApkLocation || <em className="not-configured">Not configured</em>}</span>
                </div>
                <div className="project-card-section">
                  <strong>📁 Local APK:</strong> <span>{p.localApkFolder || <em className="not-configured">Not configured</em>}</span>
                </div>
                <div className="project-card-section">
                  <strong>📋 Local Logs:</strong> <span>{p.localLogFolder || <em className="not-configured">Not configured</em>}</span>
                </div>
                {backendAvailable && projects.length > 0 && (
                  <div className="project-card-actions">
                    <button onClick={(e) => { e.stopPropagation(); handleEditClick(p); }}>Edit</button>
                    <button onClick={(e) => { e.stopPropagation(); handleDelete(p); }}>Delete</button>
                  </div>
                )}
              </div>
            )}
          </div>
        ))}
      </div>

      {/* Edit form (inline) */}
      {editingId && (
        <div className="project-edit-form">
          <h5>Edit Project</h5>
          <form onSubmit={handleUpdate} className="project-form">
            <input type="text" placeholder="Name" value={editName} onChange={e => setEditName(e.target.value)} />
            <input type="text" placeholder="Remote APK location" value={editRemoteApkLocation} onChange={e => setEditRemoteApkLocation(e.target.value)} />
            <input type="text" placeholder="Local APK folder" value={editLocalApkFolder} onChange={e => setEditLocalApkFolder(e.target.value)} />
            <input type="text" placeholder="Local Log Folder" value={editLocalLogFolder} onChange={e => setEditLocalLogFolder(e.target.value)} />
            <button type="submit">Save</button>
            <button type="button" onClick={() => setEditingId(null)}>Cancel</button>
          </form>
        </div>
      )}

      {/* Create form */}
      <details className="project-create-section" open={projects.length === 0}>
        <summary>+ Add New Project</summary>
        <form onSubmit={handleCreate} className="project-form">
          <input type="text" placeholder="Project name" value={name} onChange={e => setName(e.target.value)} />
          <input type="text" placeholder="Remote APK location" value={remoteApkLocation} onChange={e => setRemoteApkLocation(e.target.value)} />
          <input type="text" placeholder="Local APK folder" value={localApkFolder} onChange={e => setLocalApkFolder(e.target.value)} />
          <input type="text" placeholder="Local Log Folder" value={localLogFolder} onChange={e => setLocalLogFolder(e.target.value)} />
          <button type="submit">Create</button>
        </form>
      </details>

      {error && <p className="project-error">{error}</p>}
    </div>
  );
}

export default AdminProjectsPanel;
