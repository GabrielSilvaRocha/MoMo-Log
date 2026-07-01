from fastapi.testclient import TestClient

from app.main import app


client = TestClient(app)


def test_mobile_sync_readiness_route_exists() -> None:
    response = client.get("/api/v1/mobile-sync/readiness")

    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "designed"
    assert "Android Health Connect" in data["target_platforms"]
    assert any(item["mo2log"] == "RunningActivity" for item in data["data_mapping"])
