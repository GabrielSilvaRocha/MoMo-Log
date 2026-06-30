from fastapi.testclient import TestClient

from app.main import app


client = TestClient(app)


def test_demo_login_returns_token() -> None:
    response = client.post("/api/v1/auth/demo-login")

    assert response.status_code == 200

    data = response.json()
    assert data["token_type"] == "bearer"
    assert data["access_token"]
    assert data["user"]["email"] == "gabriel.demo@mo2log.com.br"


def test_me_route_with_token() -> None:
    login_response = client.post("/api/v1/auth/demo-login")
    token = login_response.json()["access_token"]

    response = client.get("/api/v1/auth/me", headers={"Authorization": f"Bearer {token}"})

    assert response.status_code == 200
    assert response.json()["user"]["id"] == 1
    assert response.json()["preferences"]["default_running_source"] == "manual_treadmill"
