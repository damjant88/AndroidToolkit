# ADR-005: Local-Only Deployment (Server and Browser on Same Machine)

## Status
Accepted

## Context
The AndroidToolkit manages physical Android devices connected via USB or WiFi to a specific machine. The question was whether to design for remote access (server on one machine, browser on another) or local-only (both on the same machine).

## Decision
Design for local-only deployment:
- Server and browser always run on the same machine that has USB/WiFi access to devices
- File paths are shared (server can access the same filesystem the user sees)
- `open-folder` endpoint launches Windows Explorer on the local machine
- APK install uses local file paths directly (drag-drop uploads to server's local disk)
- Screen mirror (scrcpy) launches a native window on the same machine

## Consequences
- No need for file streaming/transfer between machines
- Explorer "open folder" works (though in background due to Windows focus rules)
- scrcpy works natively — no need for WebRTC video streaming
- Simpler architecture — no remote agent needed
- If remote access is needed later, an agent model can be added (the core library is already decoupled)
