---
name: brainstorming into designs
description: Use when creating or developing, before writing code or implementation plans - refines rough ideas into fully-formed designs through collaborative questioning, alternative exploration, and incremental validation. Focuses on Java backend and React/Angular frontend integration.
---

# Brainstorming Ideas Into Designs

## Overview
Help turn ideas into fully formed designs and specs through natural collaborative dialogue. Focus on how a feature lives in the Java backend and manifests in the React/Angular frontend.

Start by understanding the current project context, then ask questions one at a time to refine the idea. Once you understand what you're building, present the design in small sections (200-300 words), checking after each section whether it looks right so far.

## The Process

**Understanding the idea:**
- Check out the current project state first (files, docs, recent commits) mainly those related to the idea.
- Ask questions one at a time to refine the data flow: "Will this require a new database table (Flyway) or just a frontend state change?".
- Ask about the UI framework choice: "Is this specific to the React implementation or the Angular alternative?".
- Prefer multiple choice questions when possible.
- Focus on understanding: purpose, constraints, and success criteria.

**Exploring approaches:**
- Propose 2-3 different approaches covering:
  - **Backend**: Entity design, DTO mapping (MapStruct), and Security.
  - **Frontend**: Component structure, State management, and Styling (Tailwind).
- Present options conversationally with your recommendation and reasoning.
- Recommend the most "YAGNI" (You Ain't Gonna Need It) approach to keep the boilerplate clean.

**Presenting the design:**
- Once you believe you understand what you're building, present the design in sections of 200-300 words.
- Ask after each section whether it looks right so far.
- Cover:
  - **Architecture & Data Flow**: REST endpoints and JSON structures.
  - **Components & Validation**: JSR-303 (Backend) and React Hook Form/Angular Reactive Forms (Frontend).
  - **Error Handling & Testing**: Be ready to clarify if something doesn't make sense.

## After the Design

**Documentation:**
- Write the validated design to `docs/plans/YYYY-MM-DD-<topic>/design.md`.
- Use elements-of-style:writing-clearly-and-concisely skill if available.

**Implementation (if continuing):**
- Ask: "Ready to set up for implementation?".
- Use writing-plans skill to create a detailed implementation plan.

## Key Principles
- **One question at a time** - Don't overwhelm with multiple questions.
- **Multiple choice preferred** - Easier to answer than open-ended when possible.
- **YAGNI ruthlessly** - Remove unnecessary features from all designs.
- **Explore alternatives** - Always propose 2-3 approaches before settling.
- **Incremental validation** - Present design in sections, validate each.
- **Be flexible** - Go back and clarify when something doesn't make sense.