from fastapi.testclient import TestClient

from app.main import app


client = TestClient(app)


def test_product_mvp_status() -> None:
    response = client.get("/api/v1/product/mvp-status")

    assert response.status_code == 200

    data = response.json()
    assert data["status"] == "operational"
    assert "milestone" in data
    assert "modules" in data
    assert len(data["modules"]) >= 8
    assert isinstance(data["user_flows"], list)
    assert any(flow["key"] == "manual_treadmill_run" for flow in data["user_flows"])


def test_release_notes() -> None:
    response = client.get("/api/v1/product/release-notes")

    assert response.status_code == 200
    assert response.json()["version"] == "3.0.0"
