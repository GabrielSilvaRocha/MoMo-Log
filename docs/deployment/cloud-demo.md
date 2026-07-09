# Cloud Demo Readiness

This guide prepares Mo² LOG for a public demo deployment with separate frontend, backend and managed PostgreSQL.

## Recommended Stack

- Frontend: Vercel or Netlify.
- Backend: Render, Railway or Fly.io.
- Database: managed PostgreSQL.
- Domain: HTTPS frontend and backend subdomains.

## Required Environment

- APP_VERSION=7.1.0
- DATABASE_URL=postgresql://user:password@host:5432/mo2log
- FRONTEND_ORIGIN=https://your-frontend-domain.example
- BACKEND_PUBLIC_URL=https://your-backend-domain.example
- SECRET_KEY=generate-a-long-random-value
- ACCESS_TOKEN_EXPIRE_MINUTES=1440

## Publish Flow

1. Create managed PostgreSQL and copy DATABASE_URL.
2. Deploy backend using backend/Dockerfile.prod or a Python build.
3. Run alembic upgrade head in the backend environment.
4. Deploy frontend using frontend/Dockerfile.prod or Vite static output.
5. Configure FRONTEND_ORIGIN on the backend.
6. Run smoke tests from /api/v1/ops/cloud-demo-readiness.

## Smoke Tests

- GET /api/v1/health should return status=ok.
- GET /api/v1/ops/status should return status=operational.
- GET /api/v1/product/release-notes should return version=9.4.0.
- GET /api/v1/mobile-sync/readiness should return status=designed.

## Rollback

- Keep the previous backend image or release available.
- Use a dedicated demo database where possible.
- Revert FRONTEND_ORIGIN when CORS fails.
- Roll back DNS only after confirming the previous deployment is healthy.
