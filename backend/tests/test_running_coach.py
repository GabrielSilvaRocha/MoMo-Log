from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def test_current_running_goal_exists() -> None:
    response = client.get("/api/v1/running-goals/current?user_id=1")
    assert response.status_code == 200
    data = response.json()
    assert data["race_distance_km"] == "5.00"
    assert data["training_location"] == "treadmill"


def test_running_plan_week_exists() -> None:
    response = client.get("/api/v1/running-plan/week?user_id=1&reference_date=2026-06-29T00:00:00Z")
    assert response.status_code == 200
    data = response.json()
    assert isinstance(data, list)
    assert len(data) >= 1
    assert "steps" in data[0]


def test_running_execution_speed_adjustment_flow() -> None:
    sessions = client.get("/api/v1/running-plan/week?user_id=1&reference_date=2026-06-29T00:00:00Z").json()
    session = sessions[0]
    execution = client.post(f"/api/v1/running-plan/sessions/{session['id']}/start")
    assert execution.status_code == 201
    execution_id = execution.json()["id"]
    step_id = session["steps"][0]["id"]

    step_log = client.post(f"/api/v1/running-executions/{execution_id}/steps/{step_id}/start")
    assert step_log.status_code == 201
    step_log_id = step_log.json()["id"]

    adjustment = client.post(f"/api/v1/running-step-logs/{step_log_id}/speed-up")
    assert adjustment.status_code == 200
    assert adjustment.json()["adjustment_type"] == "increase"
