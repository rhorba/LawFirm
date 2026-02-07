---
name: writing-plans
description: Use when design is complete and you need detailed implementation tasks for engineers with zero codebase context - creates comprehensive full-stack implementation plans with exact file paths, complete code examples (Spring Boot/React/Angular), and verification steps.
---

# Writing Plans (Full-Stack Edition)

## Overview

Write comprehensive implementation plans assuming the engineer has zero context for our codebase and questionable taste. Document everything they need to know: which files to touch for each task, code examples, and docs they might need to check. Give them the whole plan as bite-sized tasks. 

**Core Principles:** DRY, YAGNI, frequent commits, and strict enterprise patterns for Spring Boot 3.4 (Java 21) and React/Angular.

Assume the engineer is a skilled developer but knows almost nothing about our toolset or problem domain.

**Announce at start:** "I'm using the writing-plans skill to create the implementation plan."

**Save plans to:** The same folder as the design document (e.g., `docs/plans/YYYY-MM-DD-<feature-name>/plan.md`).

## Bite-Sized Task Granularity

**Each step is one action (2-5 minutes):**
- "Implement Spring Boot Entity/Controller/Service" - step
- "Generate/Update MapStruct Mapper or DTO" - step
- "Create React/Angular Component, Service, or State Hook" - step
- "Add Flyway migration script" - step
- "Verify functionality (Run type check, linting, and backend/frontend tests)" - step

## Mandatory Full-Stack Requirements

1.  **Data Consistency**: Every backend API change must include a task to update the frontend API client (e.g., TanStack Query hooks or Angular Services).
2.  **Database Integrity**: Database changes MUST include a Flyway migration task (`src/main/resources/db/migration/`).
3.  **Type Safety**: Update TypeScript interfaces (React) or Models/DTOs (Angular) to match backend DTOs.
4.  **Verification**: Always include specific commands for both environments (e.g., Maven and npm/pnpm/bun).

## Plan Document Header

**Every plan MUST start with this header:**

# [Feature Name] Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: use executing-plans skill to implement this plan task-by-task.

**Goal:** [One sentence describing what this builds]

**Architecture:** [2-3 sentences about the full-stack approach]

**Tech Stack:** Spring Boot 3.4 (Java 21), React/Angular, Flyway, MapStruct, Tailwind.



## Task Structure Example

### Task N: [Component/Feature Name]

**Files:**

* Create: `backend/src/main/java/com/example/features/ExampleEntity.java`
* Create: `backend/src/main/resources/db/migration/V1__init_feature.sql`
* Modify: `frontend/src/api/featureService.ts`

**Step 1: Implement Backend Logic & Persistence**
[Insert MapStruct, Entity, or Controller code here]

**Step 2: Implement Frontend Integration**
[Insert React Hook or Angular Service code here]

**Step 3: Verify Full-Stack Flow**
Run:

```bash
# Backend
mvn clean compile
# Frontend
pnpm lint && pnpm test

```

Expected: No compilation errors, all tests pass, and Flyway migration succeeds.

## Execution Handoff

After saving the plan, offer execution choice:

**"Plan complete and saved to `docs/plans/<filename>.md`. Ready for execution:"**

Ask the user if they want to execute the plan now or later. If now, use the `executing-plans` skill to proceed task-by-task. If later, instruct them to call the skill when ready.
