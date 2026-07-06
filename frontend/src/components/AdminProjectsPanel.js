import React, { useState, useEffect, useCallback } from 'react';
import { projectApi } from '../api/projectApi';
import { getStaticIcon } from '../api/projectIconImports';

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
  const [inlineFigmaIos, setInlineFigmaIos] = useState({});
  const [inlineConfluenceParent, setInlineConfluenceParent] = useState({});
  const [inlineConfluenceArtifacts, setInlineConfluenceArtifacts] = useState({});
  const [saveStatus, setSaveStatus] = useState({}); // { fieldKey: 'success' | 'error' }

  const fetchProjects = useCallback(async () => {
    try {
      const res = await projectApi.list();
      setProjects(res.data);
    } catch (err) {
      console.error('Failed to fetch projects:', err);
    }
  }, []);

  useEffect(() => { fetchProjects(); }, [fetchProjects]);

  function flashSaveStatus(key, status) {
    setSaveStatus(prev => ({ ...prev, [key]: status }));
    setTimeout(() => setSaveStatus(prev => ({ ...prev, [key]: null })), 2500);
  }

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

  function handleInlineFigmaIosChange(project, value) {
    setInlineFigmaIos(prev => ({ ...prev, [project.id]: value }));
  }

  async function handleInlineFigmaSave(project) {
    const newValue = inlineFigma[project.id];
    if (newValue === undefined || newValue === project.figmaLink) return;
    try {
      const res = await projectApi.list();
      const freshProjects = res.data;
      const existing = freshProjects.find(p => p.name === project.name);
      if (existing) {
        await projectApi.update(existing.id, {
          name: existing.name,
          remoteApkLocation: existing.remoteApkLocation || '',
          figmaLink: newValue.trim(),
          figmaLinkIos: existing.figmaLinkIos || '',
          localApkFolder: existing.localApkFolder || '',
          localLogFolder: existing.localLogFolder || '',
          confluenceParentPageId: existing.confluenceParentPageId || '',
          confluenceArtifactsPageId: existing.confluenceArtifactsPageId || ''
        });
        fetchProjects();
        setError('');
        flashSaveStatus(`figma_${project.id}`, 'success');
      } else {
        setError('Error: Project "' + project.name + '" not found in database.');
        flashSaveStatus(`figma_${project.id}`, 'error');
      }
    } catch (err) {
      setError('Error: ' + (err.response?.data?.message || err.message));
      flashSaveStatus(`figma_${project.id}`, 'error');
    }
  }

  async function handleInlineFigmaIosSave(project) {
    const newValue = inlineFigmaIos[project.id];
    if (newValue === undefined || newValue === project.figmaLinkIos) return;
    try {
      const res = await projectApi.list();
      const freshProjects = res.data;
      const existing = freshProjects.find(p => p.name === project.name);
      if (existing) {
        await projectApi.update(existing.id, {
          name: existing.name,
          remoteApkLocation: existing.remoteApkLocation || '',
          figmaLink: existing.figmaLink || '',
          figmaLinkIos: newValue.trim(),
          localApkFolder: existing.localApkFolder || '',
          localLogFolder: existing.localLogFolder || '',
          confluenceParentPageId: existing.confluenceParentPageId || '',
          confluenceArtifactsPageId: existing.confluenceArtifactsPageId || ''
        });
        fetchProjects();
        setError('');
        flashSaveStatus(`figmaIos_${project.id}`, 'success');
      } else {
        setError('Error: Project "' + project.name + '" not found in database.');
        flashSaveStatus(`figmaIos_${project.id}`, 'error');
      }
    } catch (err) {
      setError('Error: ' + (err.response?.data?.message || err.message));
      flashSaveStatus(`figmaIos_${project.id}`, 'error');
    }
  }

  async function handleConfluenceParentSave(project) {
    const newValue = inlineConfluenceParent[project.id];
    if (newValue === undefined) return;
    const current = project.confluenceParentPageId || '';
    if (newValue.trim() === current.trim()) return;
    try {
      const res = await projectApi.list();
      const freshProjects = res.data;
      const existing = freshProjects.find(p => p.name === project.name);
      if (existing) {
        await projectApi.update(existing.id, {
          name: existing.name,
          remoteApkLocation: existing.remoteApkLocation || '',
          figmaLink: existing.figmaLink || '',
          figmaLinkIos: existing.figmaLinkIos || '',
          localApkFolder: existing.localApkFolder || '',
          localLogFolder: existing.localLogFolder || '',
          confluenceParentPageId: newValue.trim(),
          confluenceArtifactsPageId: existing.confluenceArtifactsPageId || ''
        });
        fetchProjects();
        setError('');
        flashSaveStatus(`confParent_${project.id}`, 'success');
      } else {
        setError('Error: Project "' + project.name + '" not found in database.');
        flashSaveStatus(`confParent_${project.id}`, 'error');
      }
    } catch (err) {
      setError('Error: ' + (err.response?.data?.message || err.message));
      flashSaveStatus(`confParent_${project.id}`, 'error');
    }
  }

  async function handleConfluenceArtifactsSave(project) {
    const newValue = inlineConfluenceArtifacts[project.id];
    if (newValue === undefined) return;
    const current = project.confluenceArtifactsPageId || '';
    if (newValue.trim() === current.trim()) return;
    try {
      const res = await projectApi.list();
      const freshProjects = res.data;
      const existing = freshProjects.find(p => p.name === project.name);
      if (existing) {
        await projectApi.update(existing.id, {
          name: existing.name,
          remoteApkLocation: existing.remoteApkLocation || '',
          figmaLink: existing.figmaLink || '',
          figmaLinkIos: existing.figmaLinkIos || '',
          localApkFolder: existing.localApkFolder || '',
          localLogFolder: existing.localLogFolder || '',
          confluenceParentPageId: existing.confluenceParentPageId || '',
          confluenceArtifactsPageId: newValue.trim()
        });
        fetchProjects();
        setError('');
        flashSaveStatus(`confArtifacts_${project.id}`, 'success');
      } else {
        setError('Error: Project "' + project.name + '" not found in database.');
        flashSaveStatus(`confArtifacts_${project.id}`, 'error');
      }
    } catch (err) {
      setError('Error: ' + (err.response?.data?.message || err.message));
      flashSaveStatus(`confArtifacts_${project.id}`, 'error');
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
          figmaLinkIos: existing.figmaLinkIos || '',
          localApkFolder: existing.localApkFolder || '',
          localLogFolder: existing.localLogFolder || '',
          confluenceParentPageId: existing.confluenceParentPageId || '',
          confluenceArtifactsPageId: existing.confluenceArtifactsPageId || ''
        });
        fetchProjects();
        setError('');
        flashSaveStatus(`remote_${project.id}`, 'success');
      } else {
        setError('Error: Project "' + project.name + '" not found in database.');
        flashSaveStatus(`remote_${project.id}`, 'error');
      }
    } catch (err) {
      setError('Error: ' + (err.response?.data?.message || err.message));
      flashSaveStatus(`remote_${project.id}`, 'error');
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
              <img src={getStaticIcon(p.icon)} alt={p.name} className="project-card-icon" width="32" height="32" />
              <span className="project-card-name">{p.name}</span>
            </div>
            {expandedProject === p.id && (
              <div className="project-card-details">
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
                  {saveStatus[`remote_${p.id}`] && <span className={`save-indicator ${saveStatus[`remote_${p.id}`]}`}>{saveStatus[`remote_${p.id}`] === 'success' ? '✓ Saved' : '✗ Failed'}</span>}
                </div>
                <div className="project-card-section">
                  <strong>🎨 Latest {p.name} Android Figma:</strong>
                  <input
                    type="text"
                    className="project-local-input"
                    placeholder="e.g. https://www.figma.com/design/..."
                    value={inlineFigma[p.id] !== undefined ? inlineFigma[p.id] : (p.figmaLink || '')}
                    onClick={e => e.stopPropagation()}
                    onChange={e => handleInlineFigmaChange(p, e.target.value)}
                  />
                  <button className="project-save-btn" onClick={(e) => { e.stopPropagation(); handleInlineFigmaSave(p); }}>Save</button>
                  {saveStatus[`figma_${p.id}`] && <span className={`save-indicator ${saveStatus[`figma_${p.id}`]}`}>{saveStatus[`figma_${p.id}`] === 'success' ? '✓ Saved' : '✗ Failed'}</span>}
                  {p.figmaLink && <a href={p.figmaLink} target="_blank" rel="noopener noreferrer" className="project-figma-link" onClick={e => e.stopPropagation()}>Open ↗</a>}
                </div>
                <div className="project-card-section">
                  <strong>🎨 Latest {p.name} iOS Figma:</strong>
                  <input
                    type="text"
                    className="project-local-input"
                    placeholder="e.g. https://www.figma.com/design/..."
                    value={inlineFigmaIos[p.id] !== undefined ? inlineFigmaIos[p.id] : (p.figmaLinkIos || '')}
                    onClick={e => e.stopPropagation()}
                    onChange={e => handleInlineFigmaIosChange(p, e.target.value)}
                  />
                  <button className="project-save-btn" onClick={(e) => { e.stopPropagation(); handleInlineFigmaIosSave(p); }}>Save</button>
                  {saveStatus[`figmaIos_${p.id}`] && <span className={`save-indicator ${saveStatus[`figmaIos_${p.id}`]}`}>{saveStatus[`figmaIos_${p.id}`] === 'success' ? '✓ Saved' : '✗ Failed'}</span>}
                  {p.figmaLinkIos && <a href={p.figmaLinkIos} target="_blank" rel="noopener noreferrer" className="project-figma-link" onClick={e => e.stopPropagation()}>Open ↗</a>}
                </div>
                <div className="project-card-section">
                  <strong>📋 Confluence Parent Page ID:</strong>
                  <input
                    type="text"
                    className="project-local-input"
                    placeholder="e.g. 40793397 (for auto-discovery of latest RC)"
                    value={inlineConfluenceParent[p.id] !== undefined ? inlineConfluenceParent[p.id] : (p.confluenceParentPageId || '')}
                    onClick={e => e.stopPropagation()}
                    onChange={e => setInlineConfluenceParent(prev => ({ ...prev, [p.id]: e.target.value }))}
                  />
                  <button className="project-save-btn" onClick={(e) => { e.stopPropagation(); handleConfluenceParentSave(p); }}>Save</button>
                  {saveStatus[`confParent_${p.id}`] && <span className={`save-indicator ${saveStatus[`confParent_${p.id}`]}`}>{saveStatus[`confParent_${p.id}`] === 'success' ? '✓ Saved' : '✗ Failed'}</span>}
                </div>
                <div className="project-card-section">
                  <strong>📋 Confluence Artifacts Page ID (direct):</strong>
                  <input
                    type="text"
                    className="project-local-input"
                    placeholder="e.g. 101875725 (overrides parent if set)"
                    value={inlineConfluenceArtifacts[p.id] !== undefined ? inlineConfluenceArtifacts[p.id] : (p.confluenceArtifactsPageId || '')}
                    onClick={e => e.stopPropagation()}
                    onChange={e => setInlineConfluenceArtifacts(prev => ({ ...prev, [p.id]: e.target.value }))}
                  />
                  <button className="project-save-btn" onClick={(e) => { e.stopPropagation(); handleConfluenceArtifactsSave(p); }}>Save</button>
                  {saveStatus[`confArtifacts_${p.id}`] && <span className={`save-indicator ${saveStatus[`confArtifacts_${p.id}`]}`}>{saveStatus[`confArtifacts_${p.id}`] === 'success' ? '✓ Saved' : '✗ Failed'}</span>}
                </div>
                {projects.length > 0 && (
                  <div className="project-card-actions">
                    <button className="project-delete-btn" onClick={(e) => { e.stopPropagation(); handleDelete(p); }}>Delete Project</button>
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
