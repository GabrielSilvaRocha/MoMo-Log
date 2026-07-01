from fastapi.testclient import TestClient

from app.main import app


client = TestClient(app)


def test_mobile_sync_readiness_route_exists() -> None:
    response = client.get("/api/v1/mobile-sync/readiness")

    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "android_ready"
    assert "Android Health Connect" in data["target_platforms"]
    assert data["android_app"]["language"] == "Kotlin"
    assert any(item["mo2log"] == "RunningActivity" for item in data["data_mapping"])
    assert any(group["group"] == "exercise" for group in data["permission_groups"])


def test_android_sync_plan_route_exists() -> None:
    response = client.get("/api/v1/mobile-sync/android-plan")

    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "ready_for_scaffold"
    assert data["api_payload"]["source"] == "health_connect"
    assert any("Health Connect" in step for step in data["worker_flow"])
