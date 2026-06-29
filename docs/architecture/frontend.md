# Frontend Architecture

The frontend is built with React, Vite, TypeScript and TailwindCSS.

## Goals

- Keep the first user interface simple and focused on the weekly training dashboard.
- Consume the existing API-first backend.
- Validate the Mo² LOG visual identity in a real screen.
- Prepare the structure for future pages: workout execution, exercise swap, running, goals and profile.

## Structure

```text
frontend/
├── src/
│   ├── api/
│   ├── components/
│   ├── layouts/
│   ├── pages/
│   ├── types/
│   └── utils/
├── Dockerfile
├── package.json
└── vite.config.ts
```

## API integration

The frontend uses `VITE_API_BASE_URL`, defaulting to:

```text
http://localhost:8000/api/v1
```

## First screen

The first screen is the Dashboard, showing:

- today's sessions;
- completed sessions;
- upcoming sessions;
- weekly strength volume;
- weekly running distance;
- consistency;
- active goals;
- insights.
