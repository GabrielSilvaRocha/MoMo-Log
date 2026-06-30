from fastapi.testclient import TestClient

from app.main import app


client = TestClient(app)


def test_weekly_intelligence_route_exists() -> None:
    response = client.get('/api/v1/intelligence/weekly-insights?user_id=1&reference_date=2026-06-29')

    assert response.status_code == 200
    data = response.json()
    assert data['user_id'] == 1
    assert 'hybrid_score' in data
    assert isinstance(data['insights'], list)
    assert isinstance(data['recommendations'], list)


def test_5k_forecast_route_exists() -> None:
    response = client.get('/api/v1/intelligence/forecast-5k?user_id=1')

    assert response.status_code == 200
    data = response.json()
    assert data['user_id'] == 1
    assert 'predicted_5k_time_s' in data
    assert 'confidence' in data
