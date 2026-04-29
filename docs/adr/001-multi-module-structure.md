# ADR-001: Multi-Module Gradle Project Structure

## Status
Accepted

## Context
The original AndroidToolkit was a single-module Java Swing application with UI, business logic, and adb commands all mixed together. The goal is to support multiple frontends (Swing desktop, web React, REST API) sharing the same business logic.

## Decision
Split the project into three Gradle modules:
- **core** — domain models, services, app-layer managers (no UI dependencies)
- **desktop** — Swing UI (depends on core)
- **backend** — Spring Boot REST API + WebSocket (depends on core)

A fourth directory, **frontend**, holds the React app (not a Gradle module — uses npm).

## Consequences
- Core has zero Swing or Spring imports — it's pure Java
- Both desktop and backend can evolve independently
- Business logic changes are made once in core and both UIs benefit
- The desktop app still produces a standalone executable jar
- The `BuildOutputLog` interface was introduced to decouple core from Swing's ConsoleView
