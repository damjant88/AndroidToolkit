const { app, BrowserWindow, Tray, Menu, nativeImage, dialog } = require('electron');
const path = require('path');
const { spawn } = require('child_process');
const http = require('http');
const fs = require('fs');

let mainWindow;
let tray;
let agentProcess;
let agentReady = false;

// Paths
const isDev = !app.isPackaged;
const resourcesPath = isDev
  ? path.join(__dirname, 'resources')
  : path.join(process.resourcesPath);

const agentJarPath = path.join(resourcesPath, 'agent', 'agent.jar');
const frontendPath = path.join(resourcesPath, 'frontend');

// Backend URL (remote shared server) — frontend API calls go here
const BACKEND_URL = process.env.BACKEND_URL || 'http://localhost:8080';
const AGENT_PORT = 8081;

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1440,
    height: 900,
    minWidth: 1024,
    minHeight: 600,
    title: 'AndroidToolkit',
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      nodeIntegration: false,
      contextIsolation: true
    },
    show: false
  });

  // Always load from the backend which serves both frontend static files and API
  // In dev mode with DEV_SERVER_URL, use the webpack dev server instead
  if (isDev && process.env.DEV_SERVER_URL) {
    mainWindow.loadURL(process.env.DEV_SERVER_URL);
    mainWindow.webContents.openDevTools();
  } else {
    // Load from backend (serves React build + API)
    mainWindow.loadURL(BACKEND_URL);
  }

  mainWindow.once('ready-to-show', () => {
    mainWindow.show();
  });

  mainWindow.on('close', (event) => {
    // Minimize to tray instead of closing
    if (!app.isQuitting) {
      event.preventDefault();
      mainWindow.hide();
    }
  });

  mainWindow.on('closed', () => {
    mainWindow = null;
  });
}

function createTray() {
  const iconPath = path.join(__dirname, 'resources', 'icons', 'tray-icon.png');
  let trayIcon;
  try {
    trayIcon = nativeImage.createFromPath(iconPath);
  } catch (e) {
    // Fallback: create a simple icon
    trayIcon = nativeImage.createEmpty();
  }

  tray = new Tray(trayIcon);
  tray.setToolTip('AndroidToolkit');

  const contextMenu = Menu.buildFromTemplate([
    {
      label: 'Show AndroidToolkit',
      click: () => {
        if (mainWindow) {
          mainWindow.show();
          mainWindow.focus();
        }
      }
    },
    {
      label: `Agent: ${agentReady ? '✅ Running' : '⏳ Starting...'}`,
      enabled: false
    },
    { type: 'separator' },
    {
      label: 'Restart Agent',
      click: () => restartAgent()
    },
    { type: 'separator' },
    {
      label: 'Quit',
      click: () => {
        app.isQuitting = true;
        app.quit();
      }
    }
  ]);

  tray.setContextMenu(contextMenu);
  tray.on('double-click', () => {
    if (mainWindow) {
      mainWindow.show();
      mainWindow.focus();
    }
  });
}

function updateTrayMenu() {
  if (!tray) return;
  const contextMenu = Menu.buildFromTemplate([
    {
      label: 'Show AndroidToolkit',
      click: () => {
        if (mainWindow) {
          mainWindow.show();
          mainWindow.focus();
        }
      }
    },
    {
      label: `Agent: ${agentReady ? '✅ Running' : '❌ Stopped'}`,
      enabled: false
    },
    { type: 'separator' },
    {
      label: 'Restart Agent',
      click: () => restartAgent()
    },
    { type: 'separator' },
    {
      label: 'Quit',
      click: () => {
        app.isQuitting = true;
        app.quit();
      }
    }
  ]);
  tray.setContextMenu(contextMenu);
}

function startAgent() {
  console.log('[Agent] Starting agent from:', agentJarPath);

  // Check if java is available
  const javaPath = process.env.JAVA_HOME
    ? path.join(process.env.JAVA_HOME, 'bin', 'java')
    : 'java';

  agentProcess = spawn(javaPath, ['-jar', agentJarPath], {
    cwd: path.dirname(agentJarPath),
    stdio: ['pipe', 'pipe', 'pipe'],
    env: {
      ...process.env,
      SERVER_PORT: String(AGENT_PORT)
    }
  });

  agentProcess.stdout.on('data', (data) => {
    const line = data.toString();
    process.stdout.write(`[Agent] ${line}`);
    if (line.includes('Started') || line.includes('Tomcat started')) {
      agentReady = true;
      updateTrayMenu();
      console.log('[Agent] Agent is ready on port', AGENT_PORT);
    }
  });

  agentProcess.stderr.on('data', (data) => {
    process.stderr.write(`[Agent:err] ${data.toString()}`);
  });

  agentProcess.on('exit', (code) => {
    console.log(`[Agent] Process exited with code ${code}`);
    agentReady = false;
    updateTrayMenu();
  });

  agentProcess.on('error', (err) => {
    console.error('[Agent] Failed to start:', err.message);
    agentReady = false;
    dialog.showErrorBox(
      'Agent Error',
      `Failed to start the agent process.\n\nMake sure Java 21+ is installed and available in PATH.\n\nError: ${err.message}`
    );
  });

  // Poll until agent is ready
  waitForAgent();
}

function waitForAgent(retries = 30) {
  const check = () => {
    http.get(`http://localhost:${AGENT_PORT}/api/agent/devices`, (res) => {
      if (res.statusCode === 200) {
        agentReady = true;
        updateTrayMenu();
        console.log('[Agent] Health check passed');
      }
    }).on('error', () => {
      if (retries > 0) {
        setTimeout(() => waitForAgent(retries - 1), 1000);
      }
    });
  };
  setTimeout(check, 2000);
}

function stopAgent() {
  if (agentProcess) {
    console.log('[Agent] Stopping agent...');
    agentProcess.kill('SIGTERM');
    // Force kill after 5 seconds
    setTimeout(() => {
      if (agentProcess && !agentProcess.killed) {
        agentProcess.kill('SIGKILL');
      }
    }, 5000);
    agentProcess = null;
    agentReady = false;
  }
}

function restartAgent() {
  stopAgent();
  setTimeout(startAgent, 1000);
}

// App lifecycle
app.whenReady().then(() => {
  createTray();
  startAgent();
  createWindow();
});

app.on('window-all-closed', () => {
  // Don't quit — keep running in tray
});

app.on('activate', () => {
  if (mainWindow === null) {
    createWindow();
  } else {
    mainWindow.show();
  }
});

app.on('before-quit', () => {
  app.isQuitting = true;
  stopAgent();
});
