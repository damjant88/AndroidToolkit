import React, { useState, useEffect } from 'react';

const PROJECTS = [
  {
    name: 'SafePath',
    icon: 'product.png',
    packages: ['com.smithmicro.safepath.family', 'com.smithmicro.safepath.family.child'],
    color: '#4CAF50'
  },
  {
    name: 'Secure Family',
    icon: 'att.png',
    packages: ['com.smithmicro.att.securefamily', 'com.wavemarket.waplauncher', 'com.att.securefamilycompanion'],
    color: '#2196F3'
  },
  {
    name: 'Safe&Found',
    icon: 'sprint.png',
    packages: ['com.smithmicro.sprint.safeandfound.test', 'com.sprint.safefound'],
    color: '#FF9800'
  },
  {
    name: 'Family Mode',
    icon: 'tmo.png',
    packages: ['com.smithmicro.tmobile.familymode.test', 'com.tmobile.familycontrols'],
    color: '#E91E63'
  },
  {
    name: 'CCI',
    icon: 'Senior.png',
    packages: ['com.smithmicro.cci.test', 'com.smithmicro.safepath.family.light', 'com.smithmicro.safepath.family.speakeasy'],
    color: '#9C27B0'
  },
  {
    name: 'Orange',
    icon: 'toyo.png',
    packages: ['com.smithmicro.orangespain.test', 'com.orange.es.TuYo'],
    color: '#FF5722'
  },
  {
    name: 'Dish',
    icon: 'dish.png',
    packages: ['com.smithmicro.safepath.dish.test', 'com.smithmicro.safepath.dish.kid.test'],
    color: '#607D8B'
  }
];

function ProjectQuickAccess({ devices }) {
  const [selectedProject, setSelectedProject] = useState(PROJECTS[0]); // SafePath default
  const [autoSelected, setAutoSelected] = useState(false);

  // Auto-select project based on first device's installed package (overrides default)
  useEffect(() => {
    if (autoSelected || !devices || devices.length === 0) return;

    for (const device of devices) {
      const pkg = device.deviceInfo?.safePathPackage;
      if (pkg && device.deviceInfo?.appInstalled) {
        const matchedProject = PROJECTS.find(p => p.packages.includes(pkg));
        if (matchedProject) {
          setSelectedProject(matchedProject);
          setAutoSelected(true);
          return;
        }
      }
    }
  }, [devices, autoSelected]);

  return (
    <div className="project-quick-access-wrapper">
      <div className="project-buttons-row">
        {PROJECTS.map((project) => (
          <button
            key={project.name}
            className={`project-quick-btn ${selectedProject?.name === project.name ? 'active' : ''}`}
            style={{ '--project-color': project.color }}
            onClick={() => setSelectedProject(selectedProject?.name === project.name ? null : project)}
            title={project.name}
          >
            <img src={`/icons/${project.icon}`} alt={project.name} className="project-quick-icon" />
            <span className="project-quick-label">{project.name}</span>
          </button>
        ))}
      </div>

      {selectedProject && (
        <div className="project-detail-panel">
          <div className="project-detail-header">
            <img src={`/icons/${selectedProject.icon}`} alt={selectedProject.name} className="project-detail-icon" />
            <h3>{selectedProject.name}</h3>
            <button className="project-detail-close" onClick={() => setSelectedProject(null)}>✕</button>
          </div>
          <div className="project-detail-content">
            <div className="project-detail-section">
              <h4>📦 Associated Packages</h4>
              <ul className="project-package-list">
                {selectedProject.packages.map(pkg => (
                  <li key={pkg}><code>{pkg}</code></li>
                ))}
              </ul>
            </div>
            <div className="project-detail-section">
              <h4>📁 APK Paths</h4>
              <p className="placeholder-text">Configure remote APK location and local folder in Manage Projects</p>
            </div>
            <div className="project-detail-section">
              <h4>📊 Stats</h4>
              <p className="placeholder-text">Log collection stats and analysis reports will appear here</p>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default ProjectQuickAccess;
