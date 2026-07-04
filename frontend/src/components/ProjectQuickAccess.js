import React, { useState, useEffect, useMemo, useRef } from 'react';
import { projectApi } from '../api/projectApi';
import { getStaticIcon } from '../api/projectIconImports';

const PROJECTS = [
  { name: 'SafePath', icon: 'product.png', packages: ['com.smithmicro.safepath.family', 'com.smithmicro.safepath.family.child'], color: '#4CAF50' },
  { name: 'Secure Family', icon: 'att.png', packages: ['com.smithmicro.att.securefamily', 'com.wavemarket.waplauncher', 'com.att.securefamilycompanion'], color: '#2196F3' },
  { name: 'Safe&Found', icon: 'sprint.png', packages: ['com.smithmicro.sprint.safeandfound.test', 'com.sprint.safefound'], color: '#FF9800' },
  { name: 'Family Mode', icon: 'tmo.png', packages: ['com.smithmicro.tmobile.familymode.test', 'com.tmobile.familycontrols'], color: '#E91E63' },
  { name: 'CCI', icon: 'Senior.png', packages: ['com.smithmicro.cci.test', 'com.smithmicro.safepath.family.light', 'com.smithmicro.safepath.family.speakeasy'], color: '#9C27B0' },
  { name: 'Orange', icon: 'toyo.png', packages: ['com.smithmicro.orangespain.test', 'com.orange.es.TuYo'], color: '#FF5722' },
  { name: 'Dish', icon: 'dish.png', packages: ['com.smithmicro.safepath.dish.test', 'com.smithmicro.safepath.dish.kid.test'], color: '#607D8B' },
  { name: 'SPC', icon: 'spc.png', packages: ['com.smithmicro.safepath.connect'], color: '#00BCD4' },
];

// Load/save user-specific local paths from localStorage
function getLocalPaths() {
  const saved = localStorage.getItem('projectLocalPaths');
  return saved ? JSON.parse(saved) : {};
}
function saveLocalPaths(paths) {
  localStorage.setItem('projectLocalPaths', JSON.stringify(paths));
}

function ProjectQuickAccess({ devices, onProjectChange }) {
  const [selectedProject, setSelectedProject] = useState(null);
  const [backendProjects, setBackendProjects] = useState({});
  const [localPaths, setLocalPaths] = useState(getLocalPaths);
  const [savedMessage, setSavedMessage] = useState(false);

  // Fetch admin-defined project data from backend
  useEffect(() => {
    projectApi.list().then(res => {
      const map = {};
      (res.data || []).forEach(p => { map[p.name] = p; });
      setBackendProjects(map);
    }).catch(() => {});
  }, []);

  // Auto-select project only when a new device connects (serial changes)
  // Does NOT override manual user selection on subsequent polls
  const lastAutoSerial = useRef(null);
  useEffect(() => {
    if (!devices || devices.length === 0) {
      lastAutoSerial.current = null;
      return;
    }
    const devicesWithApp = devices.filter(d => d.deviceInfo?.safePathPackage && d.deviceInfo?.appInstalled);
    if (devicesWithApp.length === 1) {
      const device = devicesWithApp[0];
      const serial = device.deviceInfo.serialNumber;
      // Only auto-select when a NEW device connects (different serial)
      if (serial !== lastAutoSerial.current) {
        const pkg = device.deviceInfo.safePathPackage;
        const matched = PROJECTS.find(p => p.packages.includes(pkg));
        if (matched) {
          setSelectedProject(matched);
          lastAutoSerial.current = serial;
        }
      }
    }
  }, [devices]); // eslint-disable-line react-hooks/exhaustive-deps

  // Notify parent of selected project change
  useEffect(() => {
    if (onProjectChange) {
      onProjectChange(selectedProject?.name || null);
    }
  }, [selectedProject, onProjectChange]);

  function handleLocalPathChange(projectName, field, value) {
    const updated = { ...localPaths, [projectName]: { ...localPaths[projectName], [field]: value } };
    setLocalPaths(updated);
  }

  function handleSaveLocalPaths(projectName) {
    saveLocalPaths(localPaths);
    setSavedMessage(true);
    setTimeout(() => setSavedMessage(false), 2000);
  }

  const projectButtons = useMemo(() => (
    <div className="project-buttons-row">
      {PROJECTS.map((project) => (
        <button
          key={project.name}
          className={`project-quick-btn ${selectedProject?.name === project.name ? 'active' : ''}`}
          style={{ '--project-color': project.color }}
          onClick={() => setSelectedProject(selectedProject?.name === project.name ? null : project)}
          title={project.name}
        >
          <img src={getStaticIcon(project.icon)} alt={project.name} className="project-quick-icon" width="32" height="32" />
          <span className="project-quick-label">{project.name}</span>
        </button>
      ))}
    </div>
  ), [selectedProject]); // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <div className="project-quick-access-wrapper">
      {projectButtons}

      {selectedProject && (
        <div className="project-detail-panel">
          <div className="project-detail-header">
            <img src={getStaticIcon(selectedProject.icon)} alt={selectedProject.name} className="project-detail-icon" />
            <h3>{selectedProject.name}</h3>
            <button className="project-detail-close" onClick={() => setSelectedProject(null)}>✕</button>
          </div>
          <div className="project-detail-content project-detail-grid">
            {/* Column 1: Builds & Logs */}
            <div className="project-detail-column">
              <h4>📁 Builds & Logs</h4>
              <div className="project-detail-field">
                <label>Local APK Folder</label>
                <div className="project-local-row">
                  <input
                    type="text"
                    className="project-local-input"
                    placeholder="e.g. C:\Builds\SafePath"
                    value={localPaths[selectedProject.name]?.localApkFolder || ''}
                    onChange={e => handleLocalPathChange(selectedProject.name, 'localApkFolder', e.target.value)}
                    onClick={e => e.stopPropagation()}
                  />
                  <button className="project-save-btn" onClick={e => { e.stopPropagation(); handleSaveLocalPaths(selectedProject.name); }}>Save</button>
                </div>
              </div>
              <div className="project-detail-field">
                <label>Remote APK Location</label>
                <p>{backendProjects[selectedProject.name]?.remoteApkLocation || <em className="not-configured">Not configured by admin</em>}</p>
              </div>
              <div className="project-detail-field">
                <label>Local Log Folder</label>
                <div className="project-local-row">
                  <input
                    type="text"
                    className="project-local-input"
                    placeholder="e.g. C:\Logs\SafePath"
                    value={localPaths[selectedProject.name]?.localLogFolder || ''}
                    onChange={e => handleLocalPathChange(selectedProject.name, 'localLogFolder', e.target.value)}
                    onClick={e => e.stopPropagation()}
                  />
                  <button className="project-save-btn" onClick={e => { e.stopPropagation(); handleSaveLocalPaths(selectedProject.name); }}>Save</button>
                </div>
                {savedMessage && <span className="project-saved-msg">✓ Saved</span>}
              </div>
            </div>

            {/* Column 2: Design */}
            <div className="project-detail-column">
              <h4>🎨 Design</h4>
              <div className="project-detail-field">
                <label>Android Figma (latest)</label>
                {backendProjects[selectedProject.name]?.figmaLink
                  ? <a href={backendProjects[selectedProject.name].figmaLink} target="_blank" rel="noopener noreferrer">Open Android Figma ↗</a>
                  : <em className="not-configured">Not configured by admin</em>
                }
              </div>
              <div className="project-detail-field">
                <label>iOS Figma (latest)</label>
                {backendProjects[selectedProject.name]?.figmaLinkIos
                  ? <a href={backendProjects[selectedProject.name].figmaLinkIos} target="_blank" rel="noopener noreferrer">Open iOS Figma ↗</a>
                  : <em className="not-configured">Not configured by admin</em>
                }
              </div>
            </div>

            {/* Column 3: Project Info */}
            <div className="project-detail-column">
              <h4>📦 Project Info</h4>
              <div className="project-detail-field">
                <label>Associated Packages</label>
                <ul className="project-package-list">
                  {selectedProject.packages.map(pkg => (
                    <li key={pkg}><code>{pkg}</code></li>
                  ))}
                </ul>
              </div>
              <div className="project-detail-field">
                <label>📊 Stats</label>
                <p className="placeholder-text">Log collection stats will appear here</p>
              </div>
            </div>
          </div>

          {/* RC Info from Confluence - expandable */}
          <RcInfoSection projectName={selectedProject.name} />
        </div>
      )}
    </div>
  );
}

// Confluence search terms per project
const RC_SEARCH_MAP = {
  'SafePath': { pageId: null, title: '12.2.0 Components Artifacts' },
  'Secure Family': { pageId: '101875725', title: '12.2.0 Components Artifacts [AT&T] [Secure Family]' },
  'Safe&Found': { pageId: null, title: '12.2.0 Components Artifacts' },
  'Family Mode': { pageId: null, title: '12.2.0 Components Artifacts' },
  'CCI': { pageId: null, title: '12.2.0 Components Artifacts' },
  'Orange': { pageId: null, title: '12.2.0 Components Artifacts' },
  'Dish': { pageId: null, title: '12.2.0 Components Artifacts' },
  'SPC': { pageId: null, title: '12.2.0 Components Artifacts' },
};

function RcInfoSection({ projectName }) {
  const [expanded, setExpanded] = useState(false);
  const [artifacts, setArtifacts] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [downloading, setDownloading] = useState({});

  async function fetchArtifacts() {
    if (artifacts) { setExpanded(!expanded); return; }
    const config = RC_SEARCH_MAP[projectName];
    if (!config) { setError('No Confluence mapping for ' + projectName); return; }
    setLoading(true);
    setError('');
    try {
      let url;
      if (config.pageId) {
        url = `/api/confluence/artifacts?pageId=${config.pageId}`;
      } else {
        url = `/api/confluence/artifacts?search=${encodeURIComponent(config.title)}`;
      }
      const res = await projectApi.getConfluenceArtifacts(config.pageId || config.title);
      if (res.data.error) {
        setError(res.data.error);
      } else {
        setArtifacts(res.data);
        setExpanded(true);
      }
    } catch (err) {
      setError('Failed to fetch: ' + (err.response?.data?.message || err.message));
    } finally {
      setLoading(false);
    }
  }

  async function handleDownload(s3Path) {
    const savedPaths = localStorage.getItem('projectLocalPaths');
    let targetFolder = 'apks';
    if (savedPaths) {
      const paths = JSON.parse(savedPaths);
      if (paths[projectName]?.localApkFolder) {
        targetFolder = paths[projectName].localApkFolder;
      }
    }
    setDownloading(prev => ({ ...prev, [s3Path]: true }));
    try {
      const res = await fetch('http://localhost:8081/api/agent/devices/download-s3', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ s3Path, targetFolder })
      });
      const data = await res.json();
      if (data.success) {
        setDownloading(prev => ({ ...prev, [s3Path]: '✅' }));
      } else {
        setDownloading(prev => ({ ...prev, [s3Path]: '❌ ' + data.message }));
      }
    } catch (err) {
      setDownloading(prev => ({ ...prev, [s3Path]: '❌ ' + err.message }));
    }
  }

  return (
    <div className="rc-info-section">
      <button className="rc-info-toggle" onClick={fetchArtifacts}>
        {loading ? '⏳ Loading...' : expanded ? '📋 RC Info (Confluence) ▼' : '📋 RC Info (Confluence) ▶'}
      </button>
      {error && <p className="rc-info-error">{error}</p>}
      {expanded && artifacts && (
        <div className="rc-info-content">
          <p className="rc-info-title">{artifacts.title}</p>
          <table className="rc-info-table">
            <thead>
              <tr><th>Component</th><th>SP Ver</th><th>Version</th><th>Artifact(s)</th></tr>
            </thead>
            <tbody>
              {(artifacts.components || []).map((c, i) => (
                <tr key={i}>
                  <td><strong>{c.component}</strong></td>
                  <td>{c.spVersion}</td>
                  <td>{c.version}</td>
                  <td className="rc-artifact-cell">
                    {c.s3Paths ? c.s3Paths.split('|').map((p, j) => (
                      <div key={j} className="rc-artifact-row">
                        <span className="rc-artifact-path">{p.substring(p.lastIndexOf('/') + 1)}</span>
                        {p.endsWith('.apk') && (
                          <button className="rc-download-btn" onClick={() => handleDownload(p)}
                            disabled={downloading[p] === true}>
                            {downloading[p] === true ? '⏳' : downloading[p] === '✅' ? '✅' : '⬇'}
                          </button>
                        )}
                      </div>
                    )) : (c.artifactText || <em>—</em>)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

export default ProjectQuickAccess;
