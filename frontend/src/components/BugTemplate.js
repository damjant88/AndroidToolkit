import React, { useState, useEffect } from 'react';
import axios from 'axios';

const DEFAULT_TEMPLATE = `DESCRIPTION
{{DESCRIPTION}}

PRECONDITIONS
{{PRECONDITIONS}}

STEPS TO REPRODUCE
{{STEPS}}

ACTUAL RESULT
{{ACTUAL_RESULT}}

EXPECTED RESULT
{{EXPECTED_RESULT}}

USER IMPACT
{{USER_IMPACT}}

REPRODUCIBILITY
{{REPRODUCIBILITY}}

ENVIRONMENT:
{{ENVIRONMENT}}

DEVICE INFO:
{{DEVICE_INFO}}

ADDITIONAL INFO
{{ADDITIONAL_INFO}}`;

function BugTemplate({ devices, onClose }) {
  const [template, setTemplate] = useState(() => localStorage.getItem('bugTemplate') || DEFAULT_TEMPLATE);
  const manualDevices = JSON.parse(localStorage.getItem('manualDevices') || '[]');
  const [editingTemplate, setEditingTemplate] = useState(false);
  const [copied, setCopied] = useState(false);

  // Form fields
  const [description, setDescription] = useState('');
  const [preconditions, setPreconditions] = useState('1. Account is created with Admin and Child profiles');
  const [steps, setSteps] = useState('1. ');
  const [actualResult, setActualResult] = useState('');
  const [expectedResult, setExpectedResult] = useState('');
  const [userImpact, setUserImpact] = useState('');
  const [reproducibility, setReproducibility] = useState('100%');
  const [additionalInfo, setAdditionalInfo] = useState('Logs and screenshots are attached.');

  // Logcat data for all devices
  const [logcatDataMap, setLogcatDataMap] = useState({});

  useEffect(() => {
    if (devices && devices.length > 0) {
      devices.forEach(d => {
        const serial = d.deviceInfo.serialNumber;
        axios.get(`/api/devices/${encodeURIComponent(serial)}/logcat-data`)
          .then(res => {
            if (res.data) {
              setLogcatDataMap(prev => ({ ...prev, [serial]: res.data }));
            }
          })
          .catch(() => {});
      });
    }
  }, [devices]);

  function getEnvironment() {
    const lines = [];
    const envs = new Set();
    const productVersions = new Set();
    const projectVersions = new Set();

    Object.values(logcatDataMap).forEach(data => {
      if (data.environment) envs.add(data.environment);
      if (data.serverProductVersion) productVersions.add(data.serverProductVersion);
      if (data.serverProjectVersion) projectVersions.add(data.serverProjectVersion);
    });

    if (envs.size > 0) lines.push([...envs].join(', '));
    if (productVersions.size > 0) lines.push('Server Product Version: ' + [...productVersions].join(', '));
    if (projectVersions.size > 0) lines.push('Server Project Version: ' + [...projectVersions].join(', '));

    return lines.join('\n');
  }

  function getDeviceInfo() {
    const lines = [];
    // Auto-detected Android client version first
    const versions = new Set();
    Object.values(logcatDataMap).forEach(data => {
      if (data.clientVersion) versions.add(data.clientVersion);
    });
    if (versions.size > 0) lines.push('Android Client Version: ' + [...versions].join(', '));
    // Manual devices
    manualDevices.forEach(md => lines.push(md));
    return lines.join('\n');
  }

  function generateOutput() {
    return template
      .replace('{{DESCRIPTION}}', description || '(fill in)')
      .replace('{{PRECONDITIONS}}', preconditions || '(fill in)')
      .replace('{{STEPS}}', steps || '(fill in)')
      .replace('{{ACTUAL_RESULT}}', actualResult || '(fill in)')
      .replace('{{EXPECTED_RESULT}}', expectedResult || '(fill in)')
      .replace('{{USER_IMPACT}}', userImpact || '(fill in)')
      .replace('{{REPRODUCIBILITY}}', reproducibility)
      .replace('{{ENVIRONMENT}}', getEnvironment() || '(fill in)')
      .replace('{{DEVICE_INFO}}', getDeviceInfo() || '(fill in)')
      .replace('{{ADDITIONAL_INFO}}', additionalInfo);
  }

  function handleCopy() {
    navigator.clipboard.writeText(generateOutput());
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  }

  function handleSaveTemplate() {
    localStorage.setItem('bugTemplate', template);
    setEditingTemplate(false);
  }

  function handleResetTemplate() {
    setTemplate(DEFAULT_TEMPLATE);
    localStorage.setItem('bugTemplate', DEFAULT_TEMPLATE);
    setEditingTemplate(false);
  }

  return (
    <div className="bug-template-overlay">
      <div className="bug-template-dialog">
        <div className="bug-template-header">
          <h3>🐛 Bug Report Template</h3>
          <div className="bug-template-header-actions">
            <button className="toolbar-small-btn" onClick={() => setEditingTemplate(!editingTemplate)}>
              {editingTemplate ? 'Back to Form' : '✏️ Edit Template'}
            </button>
            <button className="close-btn" onClick={onClose}>✕</button>
          </div>
        </div>

        {editingTemplate ? (
          <div className="bug-template-editor">
            <p className="template-hint">Use placeholders: {'{{DESCRIPTION}}, {{PRECONDITIONS}}, {{STEPS}}, {{ACTUAL_RESULT}}, {{EXPECTED_RESULT}}, {{USER_IMPACT}}, {{REPRODUCIBILITY}}, {{ENVIRONMENT}}, {{DEVICE_INFO}}, {{ADDITIONAL_INFO}}'}</p>
            <textarea value={template} onChange={e => setTemplate(e.target.value)} rows={20} />
            <div className="template-editor-actions">
              <button onClick={handleSaveTemplate}>Save Template</button>
              <button onClick={handleResetTemplate} className="reset-btn">Reset to Default</button>
            </div>
          </div>
        ) : (
          <div className="bug-template-form">
            <div className="bug-template-columns">
              <div className="bug-template-left">
                <label>Description</label>
                <textarea value={description} onChange={e => setDescription(e.target.value)} rows={2} placeholder="Brief description of the bug..." />

                <label>Preconditions</label>
                <textarea value={preconditions} onChange={e => setPreconditions(e.target.value)} rows={2} />

                <label>Steps to Reproduce</label>
                <textarea value={steps} onChange={e => setSteps(e.target.value)} rows={4} placeholder="1. Start SafePath application&#10;2. ..." />

                <label>Actual Result</label>
                <textarea value={actualResult} onChange={e => setActualResult(e.target.value)} rows={2} placeholder="What actually happened..." />

                <label>Expected Result</label>
                <textarea value={expectedResult} onChange={e => setExpectedResult(e.target.value)} rows={2} placeholder="What should have happened..." />

                <label>User Impact</label>
                <input value={userImpact} onChange={e => setUserImpact(e.target.value)} placeholder="User will not receive..." />

                <label>Reproducibility</label>
                <input value={reproducibility} onChange={e => setReproducibility(e.target.value)} />
              </div>

              <div className="bug-template-right">
                <label>Environment (auto-detected)</label>
                <textarea value={getEnvironment()} readOnly rows={3} className="auto-field" />

                <label>Device Info (auto-detected + manual devices)</label>
                <textarea value={getDeviceInfo()} readOnly rows={5} className="auto-field" />

                <label>Additional Info</label>
                <input value={additionalInfo} onChange={e => setAdditionalInfo(e.target.value)} />
              </div>
            </div>

            <div className="bug-template-actions">
              <button onClick={handleCopy} className="copy-btn">
                {copied ? '✅ Copied!' : '📋 Copy to Clipboard'}
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

export default BugTemplate;
