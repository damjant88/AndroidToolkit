# ADR-006: Technology Stack Choices

## Status
Accepted

## Context
Choosing technologies for the web application layer of AndroidToolkit.

## Decisions

### Backend: Spring Boot 3.4
- Industry standard for Java REST APIs
- Excellent ecosystem (Security, WebSocket, JPA, OpenAPI)
- Same language as the existing core library — no serialization boundary
- Large community and documentation

### Frontend: React 19
- Component-based, easy to reason about
- Huge ecosystem and community
- Hot-reload for fast development
- Axios for HTTP, STOMP/SockJS for WebSocket

### Database: H2 (development) → PostgreSQL (production)
- H2 in-memory for zero-config development
- PostgreSQL for production persistence
- Spring Data JPA abstracts the difference

### Build: Gradle 9.4 Multi-Module
- Already used by the original project
- Multi-module support for core/desktop/backend
- Plugin ecosystem (Spring Boot, dependency management)

### API Documentation: SpringDoc OpenAPI (Swagger)
- Auto-generates API docs from controller annotations
- Swagger UI available at /swagger-ui/index.html
- No manual spec maintenance needed

## Consequences
- Java 21 required (Spring Boot 3.4 minimum)
- Node.js required for frontend development
- Gradle wrapper included — no global Gradle install needed
- All technologies are free and open source
