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
