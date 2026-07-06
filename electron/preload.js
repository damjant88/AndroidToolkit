const { contextBridge, ipcRenderer } = require('electron');

// Expose safe APIs to the renderer process
contextBridge.exposeInMainWorld('electronAPI', {
  getAgentUrl: () => 'http://localhost:8081',
  getBackendUrl: () => process.env.BACKEND_URL || 'http://localhost:8080',
  getPlatform: () => process.platform,
  getVersion: () => require('./package.json').version
});
