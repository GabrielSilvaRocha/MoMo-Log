from fastapi.testclient import TestClient

from app.main import app


client = TestClient(app)


def test_ops_status_route_exists() -> None:
    response = client.get("/api/v1/ops/status")

    assert response.status_code == 200
    data = response.json()
    assert data["status"] in {"operational", "degraded"}
    assert "version" in data
    assert isinstance(data["version"], str)
    assert data["version"]
    assert isinstance(data["services"], list)


def test_deployment_checklist_route_exists() -> None:
    response = client.get("/api/v1/ops/deployment-checklist")

    assert response.status_code == 200
    data = response.json()
    assert "version" in data
    assert isinstance(data["version"], str)
    assert data["version"]
    assert any(item["key"] == "secrets" for item in data["items"])
    assert any(item["key"] == "portfolio" for item in data["items"])
    assert data["demo_script"]
    assert data["screenshot_targets"]
