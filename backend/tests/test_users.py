from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def test_users_route_exists():
    response = client.get("/api/v1/users")
    assert response.status_code in (200, 500)
