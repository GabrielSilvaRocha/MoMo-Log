from fastapi.testclient import TestClient

from app.main import app


client = TestClient(app)


def test_openapi_contains_core_routes() -> None:
    response = client.get("/openapi.json")
    assert response.status_code == 200
    paths = response.json()["paths"]
    assert "/api/v1/health" in paths
    assert "/api/v1/running-activities" in paths
    assert "/api/v1/product/mvp-status" in paths
    assert "/api/v1/intelligence/weekly-insights" in paths
    assert "/api/v1/ops/status" in paths
    assert "/api/v1/workout-templates" in paths
    assert "/api/v1/running-goals/current" in paths
    assert "/api/v1/running-plan/week" in paths
