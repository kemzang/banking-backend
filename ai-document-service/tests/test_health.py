from fastapi.testclient import TestClient

from app.main import app


client = TestClient(app)


def test_health_check() -> None:
    response = client.get("/api/v1/health/")

    assert response.status_code == 200
    assert response.json() == {
        "status": "success",
        "message": "Microservice Python opérationnel",
        "data": {},
    }


def test_analysis() -> None:
    response = client.post(
        "/api/v1/analysis/",
        json={"values": [10, 20, 30]},
    )

    assert response.status_code == 200
    assert response.json() == {
        "status": "success",
        "message": "Analyse effectuée avec succès",
        "data": {
            "count": 3,
            "average": 20.0,
            "minimum": 10.0,
            "maximum": 30.0,
        },
    }


def test_analysis_validation_error_uses_standard_format() -> None:
    response = client.post(
        "/api/v1/analysis/",
        json={"values": ["invalid"]},
    )

    assert response.status_code == 422
    body = response.json()
    assert body["status"] == "error"
    assert body["message"] == "Données de requête invalides"
    assert body["errors"]
