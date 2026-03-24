# AndroidToolkit Refactor

This repository is the refactor track for the original `AndroidToolkit` desktop application.

The current application is a Java Swing tool that manages Android devices through `adb`, stores local files under `C:/AdbToolkit`, and bundles UI, device operations, process execution, and file handling in the same classes.

The goal of this repo is to evolve that codebase into a structure that can support:

- a scalable web application
- server-side APIs
- database-backed persistence
- authentication and role-based access
- background jobs for long-running device operations
- optional device-host agents for `adb` access

## Current State

- Desktop Java Swing application
- Java 11 project structure
- No database
- No authentication
- Device and UI logic are tightly coupled
- Local filesystem paths are hardcoded

## Refactor Goals

1. Separate UI code from device and business logic.
2. Move command execution and file access behind interfaces.
3. Introduce application services for installs, logs, screenshots, recordings, and device discovery.
4. Replace hardcoded paths with configuration.
5. Prepare a backend-friendly architecture that can later be hosted as a web app.
6. Add support for users, roles, audit logging, and persistent storage.

## Target Architecture

Planned layers:

- `ui`
  Swing for now, web client later
- `application`
  use cases and orchestration
- `domain`
  plain models and business rules
- `infrastructure`
  adb, filesystem, persistence, external integrations

Expected future runtime shape:

- Spring Boot backend
- PostgreSQL database
- Web frontend
- background job processing
- optional host agent for machines that have direct `adb` access to devices

## Initial Refactor Steps

1. Extract `Util` responsibilities into focused services.
2. Introduce configuration for storage locations and command execution.
3. Move device actions out of Swing listeners into service classes.
4. Add persistence models for users, devices, builds, and jobs.
5. Keep Swing working during the transition so behavior can be verified incrementally.

## Working Branches

- `main`
  backup and stable refactor baseline
- upcoming feature branches
  small, reviewable refactor steps

## Notes

This repo is intended to preserve a working migration path rather than attempt a one-shot rewrite.

## Local Build And Run

- Build from PowerShell with `.\build.ps1`
- Run the desktop app with `.\run.ps1`
- Package a Windows launcher with `.\package.ps1`
- The run script creates `C:\AdbToolkit` if it does not already exist because the app stores local state there
