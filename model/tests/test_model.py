import pytest
from fastapi.testclient import TestClient
from model.main import model

def fake_capture_frame():
    return "dummy_frame"

def fake_detect_people(frame):
    return 3, [(0,0,10,10), (20,20,30,30), (40,40,50,50)]

def fake_apply_mosaic(frame, boxes):
    return "mosaicked_frame"

class DummyResponse:
    def json(self):
        return {"status": "ok", "uploaded": True}

def fake_upload_image(frame, people_count, client_id):
    return DummyResponse()

def fake_validate_ticket(ticket: str):
    if ticket == "valid-ticket":
        return {"user_id": "test-user"}
    return None

@pytest.fixture
def client(monkeypatch):
    monkeypatch.setattr("model.main.capture_frame", fake_capture_frame)
    monkeypatch.setattr("model.main.detect_people", fake_detect_people)
    monkeypatch.setattr("model.main.apply_mosaic", fake_apply_mosaic)
    monkeypatch.setattr("model.main.upload_image", fake_upload_image)
    monkeypatch.setattr("model.main.validate_ticket", fake_validate_ticket)
    return TestClient(model)

def test_measure_success(client):
    response = client.post("/measure", params={"ticket": "valid-ticket"})
    assert response.status_code == 200
    data = response.json()
    assert data["people_count"] == 3
    assert data["api_response"] == {"status": "ok", "uploaded": True}

def test_measure_invalid_ticket(client):
    response = client.post("/measure", params={"ticket": "invalid-ticket"})
    assert response.status_code == 403
    assert response.json()["detail"] == "Invalid ticket"