import React, { useState, useEffect, useRef } from 'react';
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

  const projectButtons = (
    <div className="project-buttons-row">
      {PROJECTS.map((project) => (
        <button
          key={project.name}
          className={`project-quick-btn ${selectedProject?.name === project.name ? 'active' : ''}`}
          style={{ '--project-color': project.color }}
          onClick={() => setSelectedProject(selectedProject?.name === project.name ? null : project)}
          title={project.name}
        >
          <img src={getStaticIcon(project.icon)} alt={project.name} className="project-quick-icon" width="32" height="32" loading="eager" />
          <span className="project-quick-label">{project.name}</span>
        </button>
      ))}
    </div>
  );

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
          <div className="project-detail-layout">
            {/* Left: RC Info */}
            <div className="project-detail-left">
              <RcInfoSection projectName={selectedProject.name} backendProject={backendProjects[selectedProject.name]} />
            </div>

            {/* Middle: Local paths */}
            <div className="project-detail-middle">
              <div className="project-detail-field">
                <label>📁 Local APK Folder</label>
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
                <label>📡 Remote APK</label>
                <p>{backendProjects[selectedProject.name]?.remoteApkLocation || <em className="not-configured">Not configured</em>}</p>
              </div>
              <div className="project-detail-field">
                <label>📂 Local Log Folder</label>
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

            {/* Right: Design + Packages */}
            <div className="project-detail-right">
              <div className="project-detail-field">
                <label>🎨 Android Figma</label>
                {backendProjects[selectedProject.name]?.figmaLink
                  ? <a href={backendProjects[selectedProject.name].figmaLink} target="_blank" rel="noopener noreferrer">Open Android Figma ↗</a>
                  : <em className="not-configured">Not configured</em>
                }
              </div>
              <div className="project-detail-field">
                <label>🎨 iOS Figma</label>
                {backendProjects[selectedProject.name]?.figmaLinkIos
                  ? <a href={backendProjects[selectedProject.name].figmaLinkIos} target="_blank" rel="noopener noreferrer">Open iOS Figma ↗</a>
                  : <em className="not-configured">Not configured</em>
                }
              </div>
              <div className="project-detail-field">
                <label>📦 Packages</label>
                <span className="project-packages-inline">{selectedProject.packages.join(', ')}</span>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

// Parent page IDs are now read from the backend (admin-configurable per project)
// Fallback to empty if not configured in the admin panel

function RcInfoSection({ projectName, backendProject }) {
  const [artifacts, setArtifacts] = useState(null);
  const [error, setError] = useState('');
  const [downloadPopup, setDownloadPopup] = useState(null);
  const [selectedComponent, setSelectedComponent] = useState(null); // 'Server Core', 'Android', 'iOS'

  // Persist download state across project switches using refs keyed by project
  const allDownloads = useRef({});    // { projectName: { downloadKey: true/false } }
  const allResults = useRef({});      // { projectName: { downloadKey: { success, message } } }
  const allActiveIds = useRef({});    // { projectName: { downloadKey: downloadId } }
  const [, forceUpdate] = useState(0); // trigger re-render when download state changes

  const downloading = allDownloads.current[projectName] || {};
  const downloadResult = allResults.current[projectName] || {};
  const activeDownloadId = allActiveIds.current[projectName] || {};

  function setDownloading(updater) {
    const prev = allDownloads.current[projectName] || {};
    allDownloads.current[projectName] = typeof updater === 'function' ? updater(prev) : updater;
    forceUpdate(n => n + 1);
  }
  function setDownloadResult(updater) {
    const prev = allResults.current[projectName] || {};
    allResults.current[projectName] = typeof updater === 'function' ? updater(prev) : updater;
    forceUpdate(n => n + 1);
  }
  function setActiveDownloadId(updater) {
    const prev = allActiveIds.current[projectName] || {};
    allActiveIds.current[projectName] = typeof updater === 'function' ? updater(prev) : updater;
  }

  // Reset artifacts/error when project changes — always fetch data but keep table collapsed
  useEffect(() => {
    setArtifacts(null);
    setError('');
    setSelectedComponent(null);
    fetchArtifactsForProject(projectName, backendProject);
  }, [projectName]); // eslint-disable-line react-hooks/exhaustive-deps

  async function fetchArtifactsForProject(pName, bp) {
    const proj = bp || backendProject;
    if (!proj || !proj.id) {
      setError('No Confluence page configured for ' + pName + ' (project not found in backend)');
      return;
    }
    // Check if at least one Confluence ID is configured
    if ((!proj.confluenceParentPageId || proj.confluenceParentPageId.trim() === '') &&
        (!proj.confluenceArtifactsPageId || proj.confluenceArtifactsPageId.trim() === '')) {
      setError('No Confluence page ID configured for ' + pName + '. Set it in Admin → Projects.');
      return;
    }
    setError('');
    try {
      const res = await projectApi.getConfluenceArtifacts(proj.id);
      if (res.data.error) {
        setError(res.data.error);
      } else {
        setArtifacts(res.data);
      }
    } catch (err) {
      setError('Failed to fetch: ' + (err.response?.data?.message || err.message));
    }
  }

  async function handleDownload(s3Path, downloadKey) {
    const savedPaths = localStorage.getItem('projectLocalPaths');
    let targetFolder = 'apks';
    if (savedPaths) {
      const paths = JSON.parse(savedPaths);
      if (paths[projectName]?.localApkFolder) {
        targetFolder = paths[projectName].localApkFolder;
      }
    }
    const downloadId = 'dl_' + Date.now();
    setActiveDownloadId(prev => ({ ...prev, [downloadKey]: downloadId }));
    setDownloading(prev => ({ ...prev, [downloadKey]: true }));
    setDownloadResult(prev => ({ ...prev, [downloadKey]: null }));
    try {
      const res = await fetch('http://localhost:8081/api/agent/devices/download-s3', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ s3Path, targetFolder, downloadId })
      });
      const data = await res.json();
      setDownloading(prev => ({ ...prev, [downloadKey]: false }));
      if (data.success) {
        const output = data.output || '';
        const lastCompleted = output.split(/[\r\n]/).filter(l => l.includes('Completed')).pop() || '';
        setDownloadResult(prev => ({ ...prev, [downloadKey]: { success: true, message: '✅ ' + (lastCompleted.trim() || 'Downloaded') } }));
        // Show popup
        setDownloadPopup({ fileName: data.fileName, localPath: data.localPath, targetFolder: data.targetFolder });
      } else {
        const output = data.output || data.message || 'Unknown error';
        const errorLine = output.split(/[\r\n]/).find(l => l.includes('fatal') || l.includes('error') || l.includes('denied')) || output.substring(0, 150);
        setDownloadResult(prev => ({ ...prev, [downloadKey]: { success: false, message: '❌ ' + errorLine.trim() } }));
      }
    } catch (err) {
      setDownloading(prev => ({ ...prev, [downloadKey]: false }));
      setDownloadResult(prev => ({ ...prev, [downloadKey]: { success: false, message: '❌ ' + err.message } }));
    }
  }

  async function handleCancelDownload(downloadKey) {
    const downloadId = activeDownloadId[downloadKey];
    if (!downloadId) return;
    try {
      await fetch('http://localhost:8081/api/agent/devices/cancel-download', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ downloadId })
      });
    } catch (e) { /* ignore */ }
    setDownloading(prev => ({ ...prev, [downloadKey]: false }));
    setDownloadResult(prev => ({ ...prev, [downloadKey]: { success: false, message: '⚠️ Cancelled' } }));
  }

  async function handleOpenFolder(folderPath) {
    try {
      await fetch('http://localhost:8081/api/agent/devices/open-folder', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: 'path=' + encodeURIComponent(folderPath)
      });
    } catch (e) { /* ignore */ }
    setDownloadPopup(null);
  }

  return (
    <div className="rc-info-section">
      <div className="rc-info-title-row">
        <p className="rc-info-title">RC Info (Confluence)</p>
        {artifacts && <a href={artifacts.url} target="_blank" rel="noopener noreferrer" className="rc-confluence-link">📋 {artifacts.title} ↗</a>}
      </div>
      {artifacts && (() => {
        const components = artifacts.components || [];
        const server = components.find(c => c.component === 'Server Core');
        const android = components.find(c => c.component === 'Android');
        const ios = components.find(c => c.component === 'iOS');
        return (server || android || ios) ? (
          <div className="rc-info-versions">
            {android && <span className={`rc-version-badge ${selectedComponent === 'Android' ? 'active' : ''}`} onClick={() => setSelectedComponent(selectedComponent === 'Android' ? null : 'Android')}>🤖 Android: <strong>{android.version}</strong> {android.spVersion && `(SP ${android.spVersion})`}</span>}
            {ios && <span className={`rc-version-badge ${selectedComponent === 'iOS' ? 'active' : ''}`} onClick={() => setSelectedComponent(selectedComponent === 'iOS' ? null : 'iOS')}>🍎 iOS: <strong>{ios.version}</strong> {ios.spVersion && `(SP ${ios.spVersion})`}</span>}
            {server && <span className={`rc-version-badge ${selectedComponent === 'Server Core' ? 'active' : ''}`} onClick={() => setSelectedComponent(selectedComponent === 'Server Core' ? null : 'Server Core')}>🖥 Server: <strong>{server.version}</strong> {server.spVersion && `(SP ${server.spVersion})`}</span>}
          </div>
        ) : null;
      })()}
      {error && <p className="rc-info-error">{error}</p>}
      {selectedComponent && artifacts && (() => {
        const comp = (artifacts.components || []).find(c => c.component === selectedComponent);
        if (!comp) return null;
        return (
          <div className="rc-component-detail">
            <div className="rc-artifact-cell">
              {comp.artifactText && comp.artifactText.split('\n')
                .filter(line => !line.trim().startsWith('*To stream') && !line.trim().startsWith('To stream')
                  && !line.trim().startsWith('Quick Debug Download') && !line.trim().startsWith('aws s3 cp'))
                .map((line, j) => {
                const s3InLine = line.match(/s3:\/\/safepath-builds\/[^\s"&<]+/);
                const isLabeledDownload = line.trim().startsWith('**Debug') || line.trim().startsWith('**Release');
                const isAndroid = comp.component === 'Android';
                let s3Match = null;
                if (isAndroid && (isLabeledDownload || s3InLine)) {
                  s3Match = s3InLine ? s3InLine[0] : null;
                }
                const downloadKey = `detail_${j}`;
                let displayLine = line.replace(/s3:\/\/safepath-builds\/[^/]+\/android\/[^/]+\//g, '');
                const parts = displayLine.split(/\*\*/);
                return line.trim() ? (
                  <div key={j} className="rc-artifact-row">
                    <span className={`rc-artifact-path ${s3Match ? 'rc-artifact-downloadable' : ''}`}
                      onClick={s3Match ? (e) => { e.stopPropagation(); handleDownload(s3Match, downloadKey); } : undefined}>
                      {parts.map((part, k) => k % 2 === 1 ? <strong key={k}>{part.replace(/\s*-\s*$/, '')} </strong> : <span key={k}>{part}</span>)}
                    </span>
                    {s3Match && (
                      <span className="rc-download-group">
                        <button className="rc-download-btn" onClick={(e) => { e.stopPropagation(); handleDownload(s3Match, downloadKey); }}
                          disabled={downloading[downloadKey]}>
                          {downloading[downloadKey] ? '⏳' : '⬇'}
                        </button>
                        {downloading[downloadKey] && <>
                          <span className="rc-download-progress"><span className="rc-progress-bar"></span> Downloading...</span>
                          <button className="rc-cancel-btn" onClick={(e) => { e.stopPropagation(); handleCancelDownload(downloadKey); }}>✕</button>
                        </>}
                        {downloadResult[downloadKey] && !downloading[downloadKey] && (
                          <span className={`rc-download-result ${downloadResult[downloadKey].success ? 'success' : 'error'}`}>
                            {downloadResult[downloadKey].message}
                          </span>
                        )}
                      </span>
                    )}
                  </div>
                ) : <div key={j} className="rc-artifact-spacer" />;
              })}
              {!comp.artifactText && <em>No artifact info available</em>}
            </div>
          </div>
        );
      })()}
      {downloadPopup && (
        <div className="rc-download-popup-overlay">
          <div className="rc-download-popup">
            <h4>✅ Download Complete</h4>
            <p><strong>{downloadPopup.fileName}</strong></p>
            <p className="rc-popup-path">{downloadPopup.localPath}</p>
            <div className="rc-popup-buttons">
              <button className="rc-popup-open-btn" onClick={() => handleOpenFolder(downloadPopup.targetFolder)}>📂 Open Folder</button>
              <button className="rc-popup-close-btn" onClick={() => setDownloadPopup(null)}>Close</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default ProjectQuickAccess;
