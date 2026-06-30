SHELL := /bin/bash

.PHONY: up down rebuild migrate test backend-test frontend-build logs status prod-up prod-down prod-migrate clean

up:
	docker compose up

rebuild:
	docker compose up --build

down:
	docker compose down

migrate:
	docker compose exec backend alembic upgrade head

test:
	docker compose exec backend python -m pytest -q -vv

backend-test:
	docker compose exec backend python -m pytest -q -vv

frontend-build:
	docker compose exec frontend npm run build

status:
	curl -s http://localhost:8000/api/v1/ops/status | python -m json.tool

logs:
	docker compose logs -f backend frontend

prod-up:
	docker compose -f docker-compose.prod.yml --env-file .env.production up --build -d

prod-down:
	docker compose -f docker-compose.prod.yml --env-file .env.production down

prod-migrate:
	docker compose -f docker-compose.prod.yml --env-file .env.production exec backend alembic upgrade head

clean:
	find . -type d -name __pycache__ -prune -exec rm -rf {} +
	find . -type f -name '*.pyc' -delete
	rm -rf frontend/dist frontend/tsconfig.tsbuildinfo backend/.pytest_cache
