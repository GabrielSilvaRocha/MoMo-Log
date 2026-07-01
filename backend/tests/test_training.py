from uuid import uuid4

from fastapi.testclient import TestClient

from app.main import app


client = TestClient(app)


def test_strength_load_progression_increases_when_rir_is_available() -> None:
    for set_number, load in [(1, 120), (2, 122.5)]:
        response = client.post(
            "/api/v1/strength/set-logs",
            json={
                "strength_workout_exercise_id": 1,
                "set_number": set_number,
                "reps": 8,
                "load": load,
                "rir": 3,
                "rpe": 7,
            },
        )
        assert response.status_code == 201

    response = client.get("/api/v1/strength/exercises/1/load-progression?user_id=1")

    assert response.status_code == 200
    data = response.json()
    assert data["exercise_id"] == 1
    assert data["sample_sets"] >= 2
    assert data["recommendation"] == "increase"
    assert data["suggested_load"] is not None


def test_strength_load_progression_handles_empty_history() -> None:
    register = client.post(
        "/api/v1/auth/register",
        json={
            "name": "Load Progression Empty",
            "email": f"load-progression-{uuid4()}@example.com",
            "password": "password123",
        },
    )
    assert register.status_code == 201
    user_id = register.json()["user"]["id"]

    response = client.get(f"/api/v1/strength/exercises/1/load-progression?user_id={user_id}")

    assert response.status_code == 200
    data = response.json()
    assert data["sample_sets"] == 0
    assert data["recommendation"] == "start"
