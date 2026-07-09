# SP Test Kit

A multi-tenant platform for managing Android devices, distributing builds, collecting logs, and detecting crashes across SafePath product lines. Supports standalone local use and cloud SaaS deployment.

## Architecture

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   React     │     │   Swing     │     │   Postman   │
│  Frontend   │     │  Desktop    │     │   / curl    │
└──────┬──────┘     └──────┬──────┘     └──────┬──────┘
       │                   │                   │
       │ HTTP/WS           │ Direct Java       │ HTTP
       ▼                   ▼                   ▼
┌──────────────────────────────────────────────────────┐
│               Spring Boot Backend                    │
│    (REST API + WebSocket + Jobs + Crash Detection)   │
│         Multi-tenant · JWT Auth · Swagger            │
└────────────┬─────────────────────────┬───────────────┘
             │                         │
             │ WebSocket               │ DB + Storage
             ▼                         ▼
┌────────────────────────┐   ┌─────────────────────────┐
│     Agent (local)      │   │  H2 / MySQL / PostgreSQL │
│  ADB · Logcat · S3    │   │  + S3 / Local Filesystem │
│  port 8081             │   └─────────────────────────┘
└────────────┬───────────┘
             │
             ▼
┌──────────────────────────────────────────────────────┐
│              Android Devices (USB / WiFi)             │
└──────────────────────────────────────────────────────┘
```

## Project Structure

```
SP-Test-Kit/
├── core/               Shared domain models and services
│   ├── src/            Tenant model, subscription tiers, roles
│   └── test/           Unit tests
├── desktop/            Swing desktop application (legacy)
│   ├── src/            UI code
│   └── Assets/         Icons and images
├── backend/            Spring Boot server (port 8080)
│   ├── src/            Controllers, entities, services, crash detection
│   ├── resources/      application.properties (standalone/dev/saas profiles)
│   └── test/           Unit + property-based tests (jqwik)
├── agent/              Spring Boot agent (port 8081)
│   ├── src/            ADB operations, logcat streaming, device discovery
│   └── resources/      Agent configuration
├── frontend/           React 19 web application
│   ├── src/            Components, API layer, WebSocket hooks
│   └── public/         Static assets (project icons)
├── build.gradle        Root build config
└── settings.gradle     Module declarations (core, desktop, backend, agent)
```

## Features

### Device Management
- Real-time device discovery and status monitoring
- Install / uninstall APKs (drag-and-drop or S3 download)
- Screenshot capture, screen mirroring (scrcpy), screen recording
- WiFi debugging, device reboot, mock GPS location
- Per-app permission management
- Firebase Analytics debug mode

### Project Quick Access
- Per-project configuration: APK locations, log folders, Figma links
- RC Info from Confluence (auto-discovery of latest release artifacts)
- One-click S3 build download with progress and cancel
- Project-specific device stats

### Log Collection & Analysis
- Pull app logs and logcat from devices
- Shared storage upload (network drives / S3)
- Real-time logcat streaming via WebSocket
- AI-powered log analysis reports (scheduled nightly)

### Crash Detection
- Pattern-based logcat analysis (FATAL, ANR, WARNING)
- Per-device deduplication (5s window)
- Stack trace capture and package extraction
- Alert configuration and notification service
- Crash history with trend analysis

### Confluence Integration
- Fetch RC component artifacts (Android, iOS, Server versions)
- Admin-configurable page IDs per project
- Auto-discovery of latest child page from parent

### Multi-Tenancy (SaaS Mode)
- Tenant isolation with Hibernate filters
- Subscription tiers: FREE, PRO, ENTERPRISE
- Role-based access: OWNER, ADMIN, USER
- User invitations via email
- SSO support (Enterprise)
- Audit logging

## Deployment Modes

| Mode | Database | Storage | Use Case |
|------|----------|---------|----------|
| **dev** | H2 (file-based) | Local filesystem | Local development |
| **standalone** | MySQL | Local filesystem | Single team / on-premise |
| **saas** | PostgreSQL | S3-compatible (MinIO) | Multi-tenant cloud |

## Quick Start

### Prerequisites
- Java 21 (JDK)
- Node.js 18+ (for React frontend)
- Android device connected via USB or WiFi
- `adb` on PATH
- `scrcpy` on PATH (optional, for screen mirror)

### Run Locally (Dev Mode)

Terminal 1 — Backend:
```bash
SPRING_PROFILES_ACTIVE=dev CONFLUENCE_API_TOKEN=<token> ./gradlew :backend:bootRun
```

Terminal 2 — Agent:
```bash
./gradlew :agent:bootRun
```

Terminal 3 — Frontend:
```bash
cd frontend
npm start
```

Open http://localhost:3000 in your browser.

### Run in Standalone Mode (MySQL)
```bash
SPRING_PROFILES_ACTIVE=standalone ./gradlew :backend:bootRun
```

### Run in SaaS Mode (PostgreSQL + S3)
```bash
SPRING_PROFILES_ACTIVE=saas \
  DATABASE_URL=jdbc:postgresql://host:5432/db \
  STORAGE_ENDPOINT=https://s3.example.com \
  ./gradlew :backend:bootRun
```

### Build Everything
```bash
./gradlew build -x test
```

### Run Tests
```bash
./gradlew :core:test :backend:test
```

## Supported Projects

| Project | Package(s) |
|---------|-----------|
| SafePath | `com.smithmicro.safepath.family`, `com.smithmicro.safepath.family.child` |
| Secure Family (AT&T) | `com.smithmicro.att.securefamily`, `com.att.securefamilycompanion` |
| Safe&Found (Sprint) | `com.smithmicro.sprint.safeandfound.test`, `com.sprint.safefound` |
| Family Mode (T-Mobile) | `com.smithmicro.tmobile.familymode.test`, `com.tmobile.familycontrols` |
| CCI / SpeakEasy | `com.smithmicro.cci.test`, `com.smithmicro.safepath.family.speakeasy` |
| Orange / TuYo | `com.smithmicro.orangespain.test`, `com.orange.es.TuYo` |
| Dish | `com.smithmicro.safepath.dish.test` |
| SPC (SafePath Connect) | `com.smithmicro.safepath.connect` |

## API Documentation

Swagger UI available at: http://localhost:8080/swagger-ui.html

### Key Endpoints

| Area | Method | Path | Description |
|------|--------|------|-------------|
| Devices | GET | /api/devices | List connected devices |
| Devices | POST | /api/devices/{serial}/reboot | Reboot device |
| Devices | POST | /api/devices/{serial}/uninstall | Uninstall app |
| Devices | POST | /api/devices/{serial}/pull-logs | Pull SP logs |
| Builds | POST | /api/builds/upload | Upload APK |
| Builds | POST | /api/builds/install/{serial} | Install APK on device |
| Projects | GET | /api/projects | List projects |
| Projects | PUT | /api/projects/{id} | Update project config |
| Confluence | GET | /api/confluence/artifacts | Fetch RC artifacts |
| Jobs | POST | /api/jobs/install | Parallel install job |
| Auth | POST | /api/auth/login | Login (JWT) |
| Auth | POST | /api/auth/register | Register user |

### WebSocket Topics

| Endpoint | Topic | Description |
|----------|-------|-------------|
| /ws | /topic/devices | Real-time device updates |
| /ws | /topic/logcat/{serial} | Live logcat data |
| /ws | /topic/jobs/{id} | Job progress |
| /ws/agent | — | Agent ↔ Server communication |

## Technology Stack

| Layer | Technology |
|-------|-----------|
| Core | Java 21, domain models |
| Backend | Spring Boot 3.4, Spring Security, WebSocket, JPA |
| Agent | Spring Boot 3.4, ADB process management |
| Frontend | React 19, Axios, STOMP/SockJS, Leaflet |
| Database | H2 (dev) / MySQL (standalone) / PostgreSQL (saas) |
| Storage | Local filesystem / S3-compatible (MinIO) |
| Auth | JWT + Refresh tokens |
| Testing | JUnit 5, jqwik (property-based), Mockito |
| Build | Gradle 9.4 (multi-module) |
| API Docs | SpringDoc OpenAPI / Swagger UI |
