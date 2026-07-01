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
    assert any(item["key"] == "cloud" and item["status"] == "ready" for item in data["items"])


def test_cloud_demo_readiness_route_exists() -> None:
    response = client.get("/api/v1/ops/cloud-demo-readiness")

    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "ready"
    assert data["recommended_stack"]["database"] == "PostgreSQL gerenciado"
    assert any(test["path"] == "/api/v1/health" for test in data["smoke_tests"])
    assert any(test["path"] == "/api/v1/mobile-sync/readiness" for test in data["smoke_tests"])
