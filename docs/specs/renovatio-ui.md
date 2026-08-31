# Specification: renovatio-ui

**Issue:** #130 phase 3
**Swarm:** ui-layer
**Work:** renovatio-ui
**Status:** Draft

## 1. Overview

`renovatio-ui` is a Vite + React SPA that provides a wizard for analysts and a dashboard for leads. The SPA is built as static assets served by `renovatio-api` (CSP strict, self-contained). No MCP/OpenRewrite terminology in UI; tooltips for COBOL terms.

## 2. Architecture

```
renovatio-ui/
├── package.json
├── vite.config.js
├── index.html
├── src/
│   ├── main.jsx
│   ├── App.jsx
│   ├── api/
│   │   └── client.js          # API client (fetch wrapper)
│   ├── components/
│   │   ├── Layout.jsx         # App shell with sidebar
│   │   ├── Sidebar.jsx        # Navigation sidebar
│   │   └── Loading.jsx        # Loading spinner
│   ├── pages/
│   │   ├── Dashboard.jsx      # Main dashboard
│   │   ├── Wizard.jsx         # Migration wizard
│   │   ├── Projects.jsx       # Project list
│   │   └── ProjectDetail.jsx  # Project detail view
│   ├── wizard/
│   │   ├── StepFolder.jsx     # Step 1: Select folder
│   │   ├── StepAnalyze.jsx    # Step 2: Analyze (SSE progress)
│   │   ├── StepMetrics.jsx    # Step 3: Metrics + complexity
│   │   ├── StepPlan.jsx       # Step 4: Configure plan
│   │   ├── StepApply.jsx      # Step 5: Dry-run apply
│   │   ├── StepDiff.jsx       # Step 6: Side-by-side diffs
│   │   ├── StepReview.jsx     # Step 7: Review action items
│   │   └── StepExport.jsx     # Step 8: Real apply / export
│   ├── dashboard/
│   │   ├── MetricCards.jsx    # LOC, complexity, copybooks, procedures
│   │   ├── GateStatus.jsx     # Gate status per program
│   │   ├── ActionItems.jsx    # Action items by severity
│   │   └── JobTimeline.jsx    # Job timeline
│   └── styles/
│       └── globals.css        # Tailwind CSS
└── public/
    └── favicon.ico
```

## 3. Dependencies

### 3.1 npm Dependencies

```json
{
  "dependencies": {
    "react": "^18.2.0",
    "react-dom": "^18.2.0",
    "react-router-dom": "^6.20.0",
    "react-diff-viewer-continued": "^3.0.0"
  },
  "devDependencies": {
    "@vitejs/plugin-react": "^4.2.0",
    "vite": "^5.0.0",
    "tailwindcss": "^3.3.0",
    "autoprefixer": "^10.4.0",
    "postcss": "^8.4.0"
  }
}
```

### 3.2 Integration with renovatio-api

The SPA is built to `renovatio-api/src/main/resources/static/` and served by Spring Boot's static resource handling.

## 4. Pages

### 4.1 Dashboard (`/`)

**Purpose:** Overview for tech leads

**Components:**
- **Metric Cards:** LOC, cyclomatic complexity, copybooks, complex procedures (with trend indicators)
- **Gate Status:** Per-program gate status (parse, analyze, generate, test)
- **Action Items:** Grouped by severity (CRITICAL, HIGH, MEDIUM, LOW) with accept/reject buttons
- **Job Timeline:** Recent jobs with status indicators

### 4.2 Wizard (`/wizard`)

**Purpose:** Step-by-step migration for analysts

**Steps:**
1. **Folder Selection:** Browse/select COBOL workspace folder
2. **Analyze:** SSE progress stream, shows analysis results
3. **Metrics:** Displays complexity metrics, LOC, dependencies
4. **Plan Configuration:** Review/edit migration plan steps
5. **Dry-Run Apply:** Execute plan in dry-run mode
6. **Diff View:** Side-by-side code comparison
7. **Action Items:** Review and accept/reject manual actions
8. **Export:** Apply for real or export HTML/PDF report

### 4.3 Projects (`/projects`)

**Purpose:** List and manage projects

**Features:**
- Create new project
- List existing projects
- Navigate to project detail

### 4.4 Project Detail (`/projects/:id`)

**Purpose:** Project-specific view

**Features:**
- Project metrics summary
- Job history
- Action items for project
- Quick-start wizard for project

## 5. API Integration

### 5.1 API Client

```javascript
const API_BASE = '/api';

async function apiCall(path, options = {}) {
  const response = await fetch(`${API_BASE}${path}`, {
    headers: {
      'X-Role': localStorage.getItem('userRole') || 'ADMIN',
      'Content-Type': 'application/json',
      ...options.headers
    },
    ...options
  });
  if (!response.ok) throw new Error(`API error: ${response.status}`);
  return response.json();
}
```

### 5.2 SSE Integration

```javascript
function subscribeToJob(jobId, onEvent) {
  const eventSource = new EventSource(`/api/jobs/${jobId}/events`);
  eventSource.addEventListener('progress', (e) => onEvent(JSON.parse(e.data)));
  eventSource.addEventListener('status', (e) => onEvent(JSON.parse(e.data)));
  eventSource.addEventListener('error', (e) => onEvent({ error: 'Connection lost' }));
  return () => eventSource.close();
}
```

## 6. Styling

- **Framework:** Tailwind CSS
- **Theme:** Professional, clean, accessible
- **Colors:** Blue primary (trust), Green success, Red critical, Yellow warning
- **Typography:** System fonts (no external dependencies)
- **Responsive:** Mobile-friendly with breakpoints

## 7. COBOL Tooltips

| COBOL Term | Tooltip |
|------------|---------|
| COPYBOOK | Reusable COBOL data division fragment, similar to #include in C |
| PARAGRAPH | A named block of COBOL statements within a section |
| SECTION | A group of related paragraphs |
| DIVISION | Major structural unit (IDENTIFICATION, ENVIRONMENT, DATA, PROCEDURE) |
| FD | File Description entry, defines file structure |
| WORKING-STORAGE | Variable declaration area in COBOL |
| PIC | Picture clause, defines data type and size |
| REDEFINES | Allows same memory to be interpreted as different data types |
| OCCURS | Defines arrays or repeated items |
| THRU | Keyword used in PERFORM...THRU to execute a range of paragraphs |

## 8. Acceptance Criteria

| ID | Criterion | Description |
|----|-----------|-------------|
| wizard-flow | Wizard Flow | Wizard guides analyst through: folder selection -> analyze (SSE progress) -> metrics+complexity display -> plan configuration -> dry-run apply -> side-by-side diffs -> action item review (accept/reject) -> real apply or export |
| dashboard-view | Dashboard View | Dashboard shows: metric cards (LOC, cyclomatic complexity, copybooks, complex procedures), gate status per program, action items by severity, job timeline |
| export-capabilities | Export Capabilities | Export HTML/PDF reports from dashboard; export action items with review status |
| no-regression | No Regression | Build succeeds; renovatio-api serves static assets; existing API endpoints unchanged |
| tested | Tested | Component tests for wizard steps; dashboard rendering tests; API integration tests |

## 9. Out of Scope

- Authentication (uses X-Role header like API)
- Real-time collaboration
- Mobile app
- Internationalization (i18n)
- Dark mode (can be added later)
