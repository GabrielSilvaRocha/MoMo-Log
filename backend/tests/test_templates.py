from fastapi.testclient import TestClient

from app.main import app


client = TestClient(app)


def test_workout_templates_route_exists() -> None:
    response = client.get("/api/v1/workout-templates?user_id=1")

    assert response.status_code == 200
    data = response.json()
    assert isinstance(data, list)


def test_schedule_workout_template_route_exists() -> None:
    response = client.post(
        "/api/v1/workout-templates/1/schedule",
        json={"user_id": 1, "scheduled_date": "2026-07-06", "title": "Pernas via template"},
    )

    assert response.status_code in {201, 404, 400, 403}


def test_create_custom_workout_template() -> None:
    response = client.post(
        "/api/v1/workout-templates",
        json={
            "user_id": 1,
            "name": "Template personalizado de teste",
            "description": "Criado pela API para validar templates customizáveis.",
            "goal": "hipertrofia",
            "difficulty": "intermediate",
            "estimated_duration_minutes": 45,
            "exercises": [
                {"exercise_id": 1, "planned_sets": 4, "planned_reps": "6-8", "rest_seconds": 120, "notes": "Principal"},
                {"exercise_id": 6, "planned_sets": 3, "planned_reps": "8-10", "rest_seconds": 90, "notes": "Complementar"},
            ],
        },
    )

    assert response.status_code == 201
    data = response.json()
    assert data["name"] == "Template personalizado de teste"
    assert data["status"] == "active"
    assert len(data["exercises"]) == 2
    assert data["exercises"][0]["order_index"] == 1

    archived = client.delete(f"/api/v1/workout-templates/{data['id']}?user_id=1")
    assert archived.status_code == 204


def test_archive_custom_workout_template_removes_from_active_list() -> None:
    created = client.post(
        "/api/v1/workout-templates",
        json={
            "user_id": 1,
            "name": "Template para arquivar",
            "difficulty": "beginner",
            "exercises": [{"exercise_id": 1, "planned_sets": 2, "planned_reps": "10", "rest_seconds": 60}],
        },
    )
    assert created.status_code == 201
    template_id = created.json()["id"]

    archived = client.delete(f"/api/v1/workout-templates/{template_id}?user_id=1")
    assert archived.status_code == 204

    active_templates = client.get("/api/v1/workout-templates?user_id=1").json()
    assert all(template["id"] != template_id for template in active_templates)
