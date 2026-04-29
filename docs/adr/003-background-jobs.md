# ADR-003: Background Jobs for Install/Uninstall Operations

## Status
Accepted

## Context
Installing an APK on multiple devices takes 10-60 seconds per device. Running these sequentially in an HTTP request would timeout or block the UI. Options:
1. **Fire-and-forget** — start the operation, don't track it
2. **Async with polling** — return a job ID, client polls for status
3. **Async with WebSocket** — return a job ID, push progress via WebSocket

## Decision
Implement option 2+3 (both):
- `POST /api/jobs/install` returns immediately with a job ID
- Server runs `adb install` on each device in parallel (thread pool of 10)
- Client polls `GET /api/jobs/{id}` every second for progress
- Server also broadcasts to `/topic/jobs/{id}` via WebSocket (for future real-time subscription)
- Job tracks per-device success/failure with messages

## Consequences
- UI stays responsive during long operations
- User sees live progress: `[50%] 2/4 devices done`
- Per-device results show success/failure individually
- Job history is queryable via `GET /api/jobs`
- Jobs are stored in-memory (lost on server restart — acceptable for now)
- Future: persist jobs to database for audit trail
