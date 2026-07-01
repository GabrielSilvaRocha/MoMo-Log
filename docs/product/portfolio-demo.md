# Mo² LOG Portfolio Demo

## Positioning

Mo² LOG is a full-stack hybrid training product for strength, treadmill running, adaptive planning, progress analytics and deployment readiness.

## Demo Route

1. Open the Dashboard and show hybrid score, weekly focus and strength/running/recovery mix.
2. Open Planning and show editable strength, running, mobility and rest sessions.
3. Open Templates and schedule a reusable custom workout.
4. Open Workout and show guided set logging with load progression.
5. Open Running Coach and generate a treadmill plan with progression preferences.
6. Start a guided running session and adjust treadmill speed.
7. Open Intelligence and show race forecasts by target distance.
8. Finish on Deploy and show operational status, mobile sync readiness and screenshot targets.

## Screenshot Targets

- Dashboard: hybrid score, weekly focus and training mix.
- Running Coach: progression preferences and guided treadmill execution.
- Templates: custom workout template creation and scheduling.
- Intelligence: planned vs done and race forecasts.
- Deploy: services, checklist, demo script and mobile readiness.

## Deployment Checklist

- Backend API build passes with PostgreSQL and Alembic migrations.
- Frontend build passes with TypeScript and Vite.
- Docker Compose local environment starts backend, frontend and database.
- Production environment examples use placeholders only.
- Portfolio route exposes checklist, demo script and screenshot targets.

## Mobile Readiness

The mobile integration is designed around Android Health Connect and Samsung Health as an import-first flow. External running sessions should map into RunningActivity, with manual treadmill entries retained as fallback and conflicts routed to manual review.
