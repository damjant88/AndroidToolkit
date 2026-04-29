# AndroidToolkit

A multi-interface tool for managing Android devices through `adb`. Supports a desktop Swing app, a Spring Boot REST API, and a React web frontend — all sharing the same core business logic.

## Architecture

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   React     │     │   Swing     │     │   Postman   │
│  Frontend   │     │  Desktop    │     │   / curl    │
└──────┬──────┘     └──────┬──────┘     └──────┬──────┘
       │                   │                   │
       │ HTTP/WebSocket    │ Direct Java       │ HTTP
       ▼                   ▼                   ▼
┌──────────────────────────────────────────────────────┐
│                  Spring Boot Backend                  │
│         (REST API + WebSocket + Jobs)                 │
└──────────────────────┬───────────────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────────────┐
│                    Core Library                       │
│    (domain models, services, app managers)            │
└──────────────────────┬───────────────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────────────┐
│                   adb / devices                       │
└──────────────────────────────────────────────────────┘
```

## Project Structure

```
AndroidToolkit/
├── core/               Shared business logic (no UI dependencies)
│   ├── src/            domain, service, app layers
│   └── test/           Unit tests
├── desktop/            Swing desktop application
│   ├── src/            UI code
│   └── Assets/         Icons and images
├── backend/            Spring Boot REST API + WebSocket
│   ├── src/            Controllers, config, jobs, validation
│   └── resources/      application.properties
├── frontend/           React web application
│   ├── src/            Components, API layer, hooks
│   └── public/         Static assets (icons)
├── build.gradle        Root build config
├── settings.gradle     Module declarations
├── build.ps1           Build desktop jar
└── run.ps1             Run desktop app
```

## Quick Start

### Prerequisites
- Java 21 (JDK)
- Node.js 18+ (for React frontend)
- Android device connected via USB or WiFi
- `adb` on PATH
- `scrcpy` on PATH (optional, for screen mirror)

### Run the Desktop App
```powershell
.\run.ps1
```

### Run the Web App

Terminal 1 — Backend:
```powershell
.\gradlew.bat :backend:bootRun
```

Terminal 2 — Frontend:
```powershell
cd frontend
npm start
```

Open http://localhost:3000 in your browser.

### Build Everything
```powershell
.\gradlew.bat build -x test
```

### Run Tests
```powershell
.\gradlew.bat :core:test
```

## API Endpoints

### Devices
| Method | Path | Description |
|--------|------|-------------|
| GET | /api/devices | List connected devices |
| POST | /api/devices/{serial}/reboot | Reboot device |
| POST | /api/devices/{serial}/uninstall | Uninstall app |
| POST | /api/devices/{serial}/wifi-debug | Toggle WiFi debugging |
| POST | /api/devices/{serial}/firebase-debug | Enable Firebase debug |
| POST | /api/devices/{serial}/pull-logs | Pull SP logs from device |
| POST | /api/devices/{serial}/screenshot | Capture screenshot |

### Recording
| Method | Path | Description |
|--------|------|-------------|
| POST | /api/devices/{serial}/screen-mirror | Start scrcpy mirror |
| POST | /api/devices/{serial}/start-recording | Start screen recording |
| POST | /api/devices/{serial}/stop-recording | Stop recording + save |

### Permissions
| Method | Path | Description |
|--------|------|-------------|
| GET | /api/devices/{serial}/permissions | Get permission states |
| POST | /api/devices/{serial}/permissions/enable | Enable permissions |
| POST | /api/devices/{serial}/permissions/disable | Disable permissions |

### Builds
| Method | Path | Description |
|--------|------|-------------|
| POST | /api/builds/upload | Upload APK file |
| POST | /api/builds/install/{serial} | Install APK on device |

### Jobs (Background Operations)
| Method | Path | Description |
|--------|------|-------------|
| POST | /api/jobs/install | Start parallel install job |
| POST | /api/jobs/uninstall | Start parallel uninstall job |
| GET | /api/jobs/{id} | Get job status/progress |
| GET | /api/jobs | List recent jobs |

### Files
| Method | Path | Description |
|--------|------|-------------|
| GET | /api/files/screenshot/{name} | Get screenshot image |
| GET | /api/files/recording/download | Download recording file |
| POST | /api/files/open-folder | Open folder in Explorer |

### WebSocket
| Endpoint | Topic | Description |
|----------|-------|-------------|
| /ws | /topic/devices | Real-time device list updates |
| /ws | /topic/jobs/{id} | Real-time job progress |

## Key Design Decisions

See [docs/adr/](docs/adr/) for Architecture Decision Records.

## Local Storage

The app stores files at `C:/AdbToolkit/`:
- `Logs/` — Pulled device logs
- `Screenshots/` — Captured screenshots
- `Screen_Recordings/` — Screen recordings + logs
- `uploads/` — Uploaded APK files

## Technology Stack

| Layer | Technology |
|-------|-----------|
| Core | Java 21, plain classes |
| Desktop | Java Swing |
| Backend | Spring Boot 3.4, Spring Security, Spring WebSocket |
| Frontend | React 19, Axios, STOMP/SockJS |
| Database | H2 (dev), PostgreSQL (planned) |
| Build | Gradle 9.4 (multi-module) |
