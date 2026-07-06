# Design: RC Info from Confluence (Admin Config + LLM Monitoring)

## Overview

Two enhancements to the RC Info feature:
1. Admin-configurable Confluence page ID per project (replaces hardcoded frontend IDs)
2. Amazon Bedrock LLM job that periodically checks for Confluence page changes and extracts artifact info

---

## Part 1: Admin Confluence Page ID Configuration

### Database Schema

Add two new columns to the `projects` table:

```sql
ALTER TABLE projects ADD COLUMN confluence_parent_page_id VARCHAR(50);
ALTER TABLE projects ADD COLUMN confluence_artifacts_page_id VARCHAR(50);
```

- `confluence_parent_page_id` — The parent "Releases" page ID (for auto-discovery of latest child)
- `confluence_artifacts_page_id` — Direct page ID override (takes priority if set)

### Backend Changes

**Entity:** Add fields to `Project.java`:
```java
@Column(length = 50)
private String confluenceParentPageId;

@Column(length = 50)
private String confluenceArtifactsPageId;
```

**DTOs:** Add to `ProjectRequest`, `ProjectResponse`, `ResolvedProjectResponse`

**Service:** Update `ProjectService.create()` and `update()` to handle new fields

**Controller:** `/api/confluence/artifacts` endpoint now reads page IDs from the DB instead of receiving them from frontend

### Frontend Changes

**AdminProjectsPanel:** Add two input fields per project:
- "Confluence Parent Page ID" — for auto-discovery mode
- "Confluence Artifacts Page ID" — for direct page access (overrides parent)

**ProjectQuickAccess:** Remove hardcoded `RC_PARENT_PAGES` map. Instead, read page IDs from `backendProjects` (fetched from API).

### API Flow

```
Frontend clicks "RC Info" 
  → GET /api/confluence/artifacts?projectId={id}
  → Backend reads project from DB
  → If confluenceArtifactsPageId is set → fetch that page directly
  → Else if confluenceParentPageId is set → fetch children, find latest
  → Parse and return artifacts
```

---

## Part 2: Amazon Bedrock LLM Periodic Job

### Purpose

Periodically (every 6 hours) check Confluence pages for changes and:
1. Detect if the artifacts page has been updated (new RC)
2. Use LLM to extract/summarize key artifact info
3. Cache the parsed result in the database for fast frontend access

### Architecture

```
┌──────────────────────────────────────────────────────┐
│ Scheduled Job (Spring @Scheduled, every 6h)          │
│                                                      │
│ 1. For each project with a Confluence page ID:       │
│    - Fetch page version from Confluence REST API     │
│    - Compare with stored version                     │
│    - If changed:                                     │
│      a. Fetch full page content                      │
│      b. Send to Amazon Bedrock (Claude/Titan)        │
│      c. LLM extracts structured artifacts JSON       │
│      d. Store result in DB (cached_artifacts table)  │
│                                                      │
│ 2. Frontend reads from cache (fast, no Confluence    │
│    call needed on every click)                       │
└──────────────────────────────────────────────────────┘
```

### Database Schema

```sql
CREATE TABLE cached_artifacts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects(id),
    confluence_page_id VARCHAR(50),
    confluence_page_version INT,
    page_title VARCHAR(500),
    artifacts_json TEXT,  -- JSON array of parsed components
    last_checked_at TIMESTAMP,
    last_updated_at TIMESTAMP,
    llm_model VARCHAR(100),
    UNIQUE(project_id)
);
```

### Backend Components

**New Service:** `ConfluenceMonitorService.java`
- `@Scheduled(fixedDelay = 6 * 60 * 60 * 1000)` — runs every 6 hours
- Iterates projects with Confluence config
- Checks page version via Confluence API (`/rest/api/content/{id}?expand=version`)
- If version changed → triggers LLM extraction

**New Service:** `BedrockArtifactExtractor.java`
- Calls Amazon Bedrock (Claude 3 Haiku or Sonnet)
- Prompt: "Extract component artifacts from this Confluence page HTML. Return JSON array..."
- Parses LLM response into structured `List<ComponentArtifact>`

**New Endpoint:** `GET /api/confluence/artifacts/cached?projectId={id}`
- Returns cached artifacts from DB (instant, no Confluence call)
- Falls back to live fetch if cache is empty

### AWS Configuration

```properties
# Amazon Bedrock
bedrock.region=${AWS_REGION:us-east-1}
bedrock.model-id=${BEDROCK_MODEL_ID:anthropic.claude-3-haiku-20240307-v1:0}
bedrock.enabled=${BEDROCK_ENABLED:false}
```

### LLM Prompt Template

```
Extract the component artifacts from this Confluence page content.
Return a JSON array with objects containing:
- component: string (e.g., "Auth", "Server Core", "Android", "iOS")
- spVersion: string (SafePath version)
- version: string (project version)  
- s3Paths: array of S3 paths found
- artifactText: full text content of the artifacts cell

Page content:
{pageContent}
```

### Frontend Changes

- RC Info button first checks cached endpoint (`/api/confluence/artifacts/cached`)
- If cache exists and is fresh (< 6h old) → show cached data
- If cache is stale or missing → fall back to live Confluence fetch
- Show "Last updated: X hours ago" indicator

---

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Page ID Persistence Round-Trip

*For any* valid project with Confluence page IDs (parent and/or artifacts), creating or updating the project and then reading it back via the API SHALL return the same page ID values that were saved.

**Validates: Requirements 1.3, 1.4, 2.4**

### Property 2: Artifacts Page ID Priority Override

*For any* project that has both `confluenceArtifactsPageId` and `confluenceParentPageId` configured, the artifacts endpoint SHALL always use the `confluenceArtifactsPageId` and never attempt parent page child discovery.

**Validates: Requirements 4.2, 4.4**

### Property 3: Parent Page Latest Child Selection

*For any* parent page with one or more child pages, when a project only has `confluenceParentPageId` set, the system SHALL select the most recently created/modified child page as the artifacts source.

**Validates: Requirements 4.3**

### Property 4: Cache Uniqueness Per Project

*For any* two cache insert operations targeting the same project ID, the database SHALL reject the second insert due to the unique constraint, ensuring exactly one cached entry per project.

**Validates: Requirements 6.2**

### Property 5: Monitor Processes Only Configured Projects

*For any* set of projects in the database, the Confluence Monitor Service SHALL process only those projects that have at least one Confluence page ID configured, and skip all others.

**Validates: Requirements 7.2**

### Property 6: Version Change Triggers Extraction

*For any* project being checked by the monitor, LLM extraction SHALL be triggered if and only if the fetched Confluence page version differs from the stored version.

**Validates: Requirements 7.4, 7.5**

### Property 7: Monitor Resilience on Failure

*For any* set of projects being processed by the monitor, if one project's Confluence API call fails, all remaining projects SHALL still be processed.

**Validates: Requirements 7.6**

### Property 8: LLM Response Parsing Safety

*For any* response from Amazon Bedrock, the Bedrock Extractor SHALL either successfully parse it into a valid list of artifacts (if well-formed JSON) or gracefully reject it and retain previously cached data (if malformed).

**Validates: Requirements 8.3, 8.4**

### Property 9: Cache Freshness Determination

*For any* cached artifacts entry, the system SHALL serve the cached data directly if `last_updated_at` is less than 6 hours ago, and SHALL fall back to live Confluence fetch otherwise.

**Validates: Requirements 9.2, 9.3**

### Property 10: Frontend Cached-First Fallback

*For any* RC Info request from the frontend, the system SHALL first attempt the cached endpoint, and if the result is stale or empty, SHALL fall back to the live Confluence fetch endpoint.

**Validates: Requirements 10.3**

### Property 11: Bedrock Disabled Skips Extraction

*For any* project and any version change detected, WHILE `bedrock.enabled` is false, the Confluence Monitor Service SHALL never trigger LLM extraction regardless of version differences.

**Validates: Requirements 11.4**

---

## Implementation Order

1. **Part 1 first** (admin config) — simpler, immediate value
2. **Part 2 second** (LLM job) — requires AWS Bedrock setup, more complex

### Part 1 Tasks
- [ ] Add DB columns to Project entity
- [ ] Update DTOs (Request, Response, Resolved)
- [ ] Update ProjectService create/update
- [ ] Add admin UI fields for Confluence page IDs
- [ ] Update /api/confluence/artifacts to read from DB
- [ ] Remove hardcoded RC_PARENT_PAGES from frontend
- [ ] Test with Secure Family project

### Part 2 Tasks
- [ ] Add cached_artifacts table/entity
- [ ] Create ConfluenceMonitorService with @Scheduled
- [ ] Create BedrockArtifactExtractor service
- [ ] Add /api/confluence/artifacts/cached endpoint
- [ ] Update frontend to use cached endpoint first
- [ ] Add AWS Bedrock dependency to build.gradle
- [ ] Add configuration properties
- [ ] Test with mock LLM response
