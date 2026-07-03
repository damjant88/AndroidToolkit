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
  'SPC': { icon: 'spc.png', packages: ['com.smithmicro.safepath.connect'], color: '#00BCD4' },
};

function getMeta(name) {
  return PROJECT_META[name] || { icon: 'Android.png', packages: [], color: '#999' };
}

function AdminProjectsPanel() {
  const [projects, setProjects] = useState([]);
  const [name, setName] = useState('');
  const [remoteApkLocation, setRemoteApkLocation] = useState('');
  const [error, setError] = useState('');
  const [expandedProject, setExpandedProject] = useState(null);
  const [inlineRemote, setInlineRemote] = useState({});
  const [inlineFigma, setInlineFigma] = useState({});

  const fetchProjects = useCallback(async () => {
    try {
      const res = await projectApi.list();
      setProjects(res.data);
    } catch (err) {
      console.error('Failed to fetch projects:', err);
    }
  }, []);

  useEffect(() => { fetchProjects(); }, [fetchProjects]);

  async function handleCreate(e) {
    e.preventDefault();
    if (!name.trim() || !remoteApkLocation.trim()) return;
    setError('');
    try {
      await projectApi.create({ name: name.trim(), remoteApkLocation: remoteApkLocation.trim(), localApkFolder: 'default', localLogFolder: 'default' });
      setName(''); setRemoteApkLocation('');
      fetchProjects();
    } catch (err) {
      setError('Error: ' + (err.response?.data?.message || err.message));
    }
  }

  function handleInlineRemoteChange(project, value) {
    setInlineRemote(prev => ({ ...prev, [project.id]: value }));
  }

  function handleInlineFigmaChange(project, value) {
    setInlineFigma(prev => ({ ...prev, [project.id]: value }));
  }

  async function handleInlineFigmaSave(project) {
    const newValue = inlineFigma[project.id];
    if (newValue === undefined || newValue === project.figmaLink) return;
    try {
      // Fetch fresh project list to ensure we have DB IDs
      const res = await projectApi.list();
      const freshProjects = res.data;
      const existing = freshProjects.find(p => p.name === project.name);
      if (existing) {
        await projectApi.update(existing.id, {
          name: existing.name,
          remoteApkLocation: existing.remoteApkLocation || '',
          figmaLink: newValue.trim(),
          localApkFolder: existing.localApkFolder || '',
          localLogFolder: existing.localLogFolder || ''
        });
        fetchProjects();
        setError('');
      } else {
        setError('Error: Project "' + project.name + '" not found in database.');
      }
    } catch (err) {
      setError('Error: ' + (err.response?.data?.message || err.message));
    }
  }

  async function handleInlineRemoteSave(project) {
    const newValue = inlineRemote[project.id];
    if (newValue === undefined || newValue === project.remoteApkLocation) return;
    if (!newValue.trim()) return;
    try {
      // Fetch fresh project list to ensure we have DB IDs
      const res = await projectApi.list();
      const freshProjects = res.data;
      const existing = freshProjects.find(p => p.name === project.name);
      if (existing) {
        // Update existing project
        await projectApi.update(existing.id, {
          name: existing.name,
          remoteApkLocation: newValue.trim(),
          figmaLink: existing.figmaLink || '',
          localApkFolder: existing.localApkFolder || '',
          localLogFolder: existing.localLogFolder || ''
        });
        fetchProjects();
        setError('');
      } else {
        setError('Error: Project "' + project.name + '" not found in database.');
      }
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
        id: pName, name: pName, remoteApkLocation: '', ...meta
      }));

  return (
    <div className="admin-projects-panel">
      <h4>Projects (Admin)</h4>

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
              <img src={`/icons/${p.icon}`} alt={p.name} className="project-card-icon" width="32" height="32" />
              <span className="project-card-name">{p.name}</span>
            </div>
            {expandedProject === p.id && (
              <div className="project-card-details">
                <div className="project-card-section">
                  <strong>📦 Packages:</strong>
                  <ul>{p.packages.map(pkg => <li key={pkg}><code>{pkg}</code></li>)}</ul>
                </div>
                <div className="project-card-section">
                  <strong>📡 Remote APK Location:</strong>
                  <input
                    type="text"
                    className="project-local-input"
                    placeholder="e.g. \\\\server\\builds\\SafePath"
                    value={inlineRemote[p.id] !== undefined ? inlineRemote[p.id] : (p.remoteApkLocation || '')}
                    onClick={e => e.stopPropagation()}
                    onChange={e => handleInlineRemoteChange(p, e.target.value)}
                  />
                  <button className="project-save-btn" onClick={(e) => { e.stopPropagation(); handleInlineRemoteSave(p); }}>Save</button>
                </div>
                <div className="project-card-section">
                  <strong>🎨 Latest Figma Link:</strong>
                  <input
                    type="text"
                    className="project-local-input"
                    placeholder="e.g. https://www.figma.com/design/..."
                    value={inlineFigma[p.id] !== undefined ? inlineFigma[p.id] : (p.figmaLink || '')}
                    onClick={e => e.stopPropagation()}
                    onChange={e => handleInlineFigmaChange(p, e.target.value)}
                  />
                  <button className="project-save-btn" onClick={(e) => { e.stopPropagation(); handleInlineFigmaSave(p); }}>Save</button>
                  {p.figmaLink && <a href={p.figmaLink} target="_blank" rel="noopener noreferrer" className="project-figma-link" onClick={e => e.stopPropagation()}>Open ↗</a>}
                </div>
                {projects.length > 0 && (
                  <div className="project-card-actions">
                    <button onClick={(e) => { e.stopPropagation(); handleDelete(p); }}>Delete</button>
                  </div>
                )}
              </div>
            )}
          </div>
        ))}
      </div>

      {/* Create form */}
      <details className="project-create-section" open={projects.length === 0}>
        <summary>+ Add New Project</summary>
        <form onSubmit={handleCreate} className="project-form">
          <input type="text" placeholder="Project name" value={name} onChange={e => setName(e.target.value)} />
          <input type="text" placeholder="Remote APK Location" value={remoteApkLocation} onChange={e => setRemoteApkLocation(e.target.value)} />
          <button type="submit">Create</button>
        </form>
      </details>

      {error && <p className="project-error">{error}</p>}
    </div>
  );
}

export default AdminProjectsPanel;
