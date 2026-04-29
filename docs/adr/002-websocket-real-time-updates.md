# ADR-002: WebSocket for Real-Time Device Updates

## Status
Accepted

## Context
The React frontend needs to know when devices connect or disconnect. Options considered:
1. **HTTP polling every N seconds** — simple but wasteful and delayed
2. **WebSocket push** — instant updates, lower overhead
3. **Server-Sent Events (SSE)** — simpler than WebSocket but one-directional

## Decision
Use Spring WebSocket with STOMP protocol over SockJS:
- Server polls adb every 3 seconds (lightweight — just `adb devices`)
- Only broadcasts when the device list actually changes
- Clients subscribe to `/topic/devices` for instant updates
- Fallback HTTP polling every 30 seconds if WebSocket disconnects

## Consequences
- Device connect/disconnect is reflected in the UI within 3 seconds
- No unnecessary network traffic when nothing changes
- SockJS provides fallback for environments that block WebSocket
- Job progress is also broadcast via `/topic/jobs/{id}`
- Added dependency: `spring-boot-starter-websocket`, `sockjs-client`, `@stomp/stompjs`
