SHELL := /bin/bash

.PHONY: up down rebuild migrate test backend-test frontend-build logs

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

logs:
	docker compose logs -f backend frontend
