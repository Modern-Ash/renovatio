# Implementation Plan: renovatio-ui

**Issue:** #130 phase 3
**Swarm:** ui-layer
**Work:** renovatio-ui
**Spec:** docs/specs/renovatio-ui.md

## Overview

This plan breaks the renovatio-ui implementation into 10 tasks. Each task produces a testable increment.

## Tasks

### Task 0: Project Setup + Vite Config
**Depends on:** None
**Criteria:** no-regression

1. Create `renovatio-ui/` directory
2. Initialize `package.json` with dependencies
3. Create `vite.config.js` with React plugin and proxy to API
4. Create `tailwind.config.js` and `postcss.config.js`
5. Create `index.html` entry point
6. Create `src/main.jsx` and `src/App.jsx` with React Router
7. Add build script to output to `renovatio-api/src/main/resources/static/`
8. Verify: `npm install && npm run build` succeeds

### Task 1: API Client + Layout
**Depends on:** Task 0
**Criteria:** no-regression

1. Create `src/api/client.js` with fetch wrapper
2. Create `src/components/Layout.jsx` with sidebar navigation
3. Create `src/components/Sidebar.jsx` with navigation links
4. Create `src/components/Loading.jsx` spinner component
5. Create `src/styles/globals.css` with Tailwind imports
6. Verify: App renders with sidebar navigation

### Task 2: Dashboard Page
**Depends on:** Task 1
**Criteria:** dashboard-view

1. Create `src/pages/Dashboard.jsx` as main dashboard
2. Create `src/dashboard/MetricCards.jsx` with metric displays
3. Create `src/dashboard/GateStatus.jsx` with gate status per program
4. Create `src/dashboard/ActionItems.jsx` with action items by severity
5. Create `src/dashboard/JobTimeline.jsx` with job history
6. Wire dashboard to API endpoints
7. Verify: Dashboard shows metrics, gates, action items, timeline

### Task 3: Projects Page
**Depends on:** Task 1
**Criteria:** no-regression

1. Create `src/pages/Projects.jsx` with project list
2. Create project creation form
3. Create `src/pages/ProjectDetail.jsx` with project detail view
4. Wire to API endpoints
5. Verify: Can list and create projects

### Task 4: Wizard - Steps 1-3
**Depends on:** Task 1
**Criteria:** wizard-flow

1. Create `src/pages/Wizard.jsx` as wizard container
2. Create `src/wizard/StepFolder.jsx` for folder selection
3. Create `src/wizard/StepAnalyze.jsx` with SSE progress
4. Create `src/wizard/StepMetrics.jsx` with metrics display
5. Implement step navigation (next/prev)
6. Verify: Wizard navigates through steps 1-3

### Task 5: Wizard - Steps 4-5
**Depends on:** Task 4
**Criteria:** wizard-flow

1. Create `src/wizard/StepPlan.jsx` for plan configuration
2. Create `src/wizard/StepApply.jsx` for dry-run apply
3. Wire to API job endpoints
4. Verify: Plan configuration and dry-run work

### Task 6: Wizard - Steps 6-8
**Depends on:** Task 5
**Criteria:** wizard-flow

1. Create `src/wizard/StepDiff.jsx` with side-by-side diff viewer
2. Create `src/wizard/StepReview.jsx` with action item accept/reject
3. Create `src/wizard/StepExport.jsx` with export options
4. Verify: Diff view, action item review, and export work

### Task 7: COBOL Tooltips
**Depends on:** Task 2
**Criteria:** dashboard-view

1. Create `src/components/CobolTooltip.jsx` for COBOL term tooltips
2. Add tooltips to dashboard and wizard components
3. Verify: Tooltips appear on hover for COBOL terms

### Task 8: Export Functionality
**Depends on:** Task 2
**Criteria:** export-capabilities

1. Create `src/utils/export.js` for HTML/PDF export
2. Add export buttons to dashboard and wizard
3. Wire to API report endpoints
4. Verify: HTML/PDF export works

### Task 9: Tests
**Depends on:** All previous tasks
**Criteria:** tested

1. Create component tests for wizard steps
2. Create dashboard rendering tests
3. Create API integration tests
4. Verify: All tests pass

### Task 10: Build Integration + Documentation
**Depends on:** Task 9
**Criteria:** no-regression

1. Configure build output to `renovatio-api/src/main/resources/static/`
2. Add `npm run build` to Maven build lifecycle
3. Verify `java -jar renovatio-api.jar` serves SPA
4. Add README.md
5. Verify: Full build succeeds, SPA loads in browser

## Verification

After all tasks:
```bash
cd renovatio-ui
npm install
npm run build        # Builds to renovatio-api/src/main/resources/static/
cd ..
mvn clean install -pl renovatio-api -DskipTests
java -jar renovatio-api/target/renovatio-api.jar
# Open http://localhost:8080 - SPA should load
```

## Risk Mitigation

1. **Build integration:** Use `frontend-maven-plugin` or npm scripts in Maven
2. **CSP headers:** Configure Spring Boot to serve static assets with proper CSP
3. **API proxy:** Vite dev server proxies to API during development
4. **Diff viewer:** Use `react-diff-viewer-continued` for side-by-side diffs
