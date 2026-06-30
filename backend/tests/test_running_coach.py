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


def test_running_step_completion_advances_to_next_step() -> None:
    sessions = client.get("/api/v1/running-plan/week?user_id=1&reference_date=2026-06-29T00:00:00Z").json()
    session = next(item for item in sessions if len(item["steps"]) >= 2)
    execution = client.post(f"/api/v1/running-plan/sessions/{session['id']}/start")
    assert execution.status_code == 201
    execution_id = execution.json()["id"]

    first_step = session["steps"][0]
    step_log = client.post(f"/api/v1/running-executions/{execution_id}/steps/{first_step['id']}/start")
    assert step_log.status_code == 201

    advance = client.post(f"/api/v1/running-step-logs/{step_log.json()['id']}/complete")
    assert advance.status_code == 200
    data = advance.json()
    assert data["completed_step_log"]["completed"] is True
    assert data["next_step"]["id"] == session["steps"][1]["id"]
    assert data["session_completed"] is False
    assert data["execution"]["status"] == "in_progress"

    last_step = session["steps"][-1]
    last_step_log = client.post(f"/api/v1/running-executions/{execution_id}/steps/{last_step['id']}/start")
    assert last_step_log.status_code == 201
    finish = client.post(f"/api/v1/running-step-logs/{last_step_log.json()['id']}/complete")
    assert finish.status_code == 200
    assert finish.json()["session_completed"] is True
    assert finish.json()["execution"]["status"] == "completed"
