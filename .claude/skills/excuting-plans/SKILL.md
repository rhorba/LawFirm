---
name: executing-plans
description: Use when a complete implementation plan is provided to execute in controlled batches with review checkpoints - ensures synchronization between Java 21 backend (MapStruct/Flyway) and React/Angular frontend before moving between batches.
---

# Executing Plans (Full-Stack Edition)

## Overview
Load the plan, review it critically for full-stack consistency, execute tasks in batches of 3, and report for review between batches.

**Core Principle:** Batch execution with mandatory checkpoints to ensure the Java backend and React/Angular frontend remain in sync.

**Announce at start:** "I'm using the executing-plans skill to implement this plan."

## The Process

### Step 1: Load and Review Plan
1. **Read plan file:** Locate and load the specified plan.
2. **Technical Audit:** Confirm both Backend (Java 21) and Frontend (React/Angular) paths are addressed.
3. **Dependency Check:** Verify that MapStruct mappers and Flyway migrations are included and not skipped.
4. **Identify Concerns:** If there are critical gaps or logical errors, raise them with your partner before starting. Otherwise, create a Todo list and proceed.

### Step 2: Execute Batch
**Default: Execute the first 3 tasks.**
For each task:
1. Mark as `in_progress`.
2. **Backend Standards:** Ensure Lombok and MapStruct annotations are correctly applied; verify Spring Boot 3.4 patterns.
3. **Frontend Standards:** Follow framework-specific patterns (React Hooks or Angular Observables) and ensure Tailwind classes are used for styling.
4. **Verification:** Follow every bite-sized step exactly and run verifications as specified in the plan.
5. Mark as `completed`.

### Step 3: Report & Verify
When the batch is complete:
- **Status Report:** Show exactly what was implemented.
- **Verification Output:** Show Java compilation status and Frontend build/lint status.
- **Synchronization Check:** Confirm: "Backend and Frontend are in sync. Ready for feedback or the next batch?"

### Step 4: Continue or Commit
- **If user wants to commit:** Run linting and type checking, commit the changes, and ask to proceed to the next batch.
- **If changes are needed:** Apply feedback before moving forward.
- **Finalization:** Once all tasks are complete, run a final full-stack verification and offer to create a pull request.

## Critical Blockers (STOP Immediately)
**STOP executing and ask for clarification if:**
- **Mismatch:** Backend API signatures and Frontend Types/Services do not match.
- **Tooling Failure:** MapStruct fails to generate implementation or Flyway migrations fail on startup.
- **Verification Failure:** Any specified test or linting step fails repeatedly.
- **Ambiguity:** You hit a missing dependency or an instruction you do not understand. **Do not guess.**

## Remember
- **Follow exactly:** Do not skip steps or verifications.
- **Batch focus:** Only do 3 tasks at a time unless otherwise directed.
- **Sync first:** Never leave the frontend and backend in a broken state between batches.