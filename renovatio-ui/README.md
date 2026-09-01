# Renovatio UI

React SPA for the Renovatio REST API, providing a visual interface for COBOL modernization workflows.

## Features

- **Dashboard**: Real-time metrics, gate status, action items, job timeline
- **Projects**: Create and manage COBOL modernization projects
- **8-Step Wizard**: Guided workflow from folder selection to export
- **COBOL Tooltips**: Hover over COBOL terms for explanations

## Development

```bash
npm install
npm run dev    # Vite dev server on :3000 (proxies /api to :8080)
npm run build  # Outputs to renovatio-api/src/main/resources/static/
npm test       # Runs vitest
```

## Production

The SPA is built into `renovatio-api/src/main/resources/static/` and served by the Spring Boot API:

```bash
cd ../renovatio-api
mvn package    # Runs npm build automatically
java -jar target/renovatio-api.jar
```

## Tech Stack

- React 18
- Vite
- Tailwind CSS
- React Router
- Vitest
