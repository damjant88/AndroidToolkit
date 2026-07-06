# AndroidToolkit Desktop (Electron)

Desktop application that bundles the React frontend and the Spring Boot agent into a single installable app.

## Architecture

```
┌─────────────────────────────────────────────┐
│  Electron Desktop App                       │
│                                             │
│  ┌────────────┐    ┌────────────────────┐   │
│  │ App Window │    │ Agent Process      │   │
│  │ (React UI) │    │ (Spring Boot JAR)  │   │
│  │            │───▶│ localhost:8081      │   │
│  └────────────┘    │ ADB, S3, file ops  │   │
│       │            └────────────────────┘   │
└───────┼─────────────────────────────────────┘
        │ REST/WebSocket
        ▼
┌─────────────────────┐
│ Remote Backend       │
│ (shared server)      │
│ Projects, Users, Auth│
└─────────────────────┘
```

## Prerequisites

- **Node.js** 18+ (for Electron build)
- **Java 21+** (for running the agent JAR)
- The backend running on a remote server (or locally for dev)

## Development

```bash
# Install Electron dependencies
cd electron
npm install

# Run in dev mode (uses frontend dev server + local backend)
DEV_SERVER_URL=http://localhost:3000 npm start

# Or just point to local backend
npm start
```

## Building for Distribution

```bash
# 1. Build the React frontend
cd ../frontend
npm run build

# 2. Build the agent JAR
cd ..
./gradlew :agent:bootJar

# 3. Copy artifacts into electron/resources
cp -r frontend/build/* electron/resources/frontend/
cp agent/build/libs/agent.jar electron/resources/agent/

# 4. Build the installer
cd electron
npm run dist
```

The installer will be in `electron/dist/`.

## Configuration

Environment variables:

| Variable | Default | Description |
|----------|---------|-------------|
| `BACKEND_URL` | `http://localhost:8080` | Remote backend URL |
| `JAVA_HOME` | (system) | Path to Java 21+ installation |
| `DEV_SERVER_URL` | — | React dev server URL (dev mode only) |

## Features

- **System tray** — app minimizes to tray, agent keeps running
- **Auto-start agent** — Spring Boot agent starts with the app
- **Agent health monitoring** — tray icon shows agent status
- **Restart agent** — right-click tray → Restart Agent
- **Cross-platform** — builds for Windows (.exe), macOS (.dmg), Linux (.AppImage)
