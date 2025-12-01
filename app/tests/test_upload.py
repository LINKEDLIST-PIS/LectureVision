import io
from app.main import app, verify_bearer

def test_upload_file_too_large(client):
    app.dependency_overrides[verify_bearer] = lambda: {"sub": "test-client", "aud": "server-client"}
    big_file = io.BytesIO(b"x" * (11 * 1024 * 1024))
    response = client.post(
        "/upload",
        files={"file": ("big.jpg", big_file, "image/jpeg")},
        data={"people_count": 1, "client_id": "test-client"},
        headers={
            "X-Timestamp": "1234567890",
            "X-Signature": "dummy",
            "Idempotency-Key": "abc123",
            "Authorization": "Bearer dummy-token"
        }
    )
    assert response.status_code == 413
    assert response.json()["detail"] == "File too large (max 10MB)"


def test_upload_file_invalid_type(client):
    app.dependency_overrides[verify_bearer] = lambda: {"sub": "test-client", "aud": "server-client"}
    small_file = io.BytesIO(b"x" * 1024)
    response = client.post(
        "/upload",
        files={"file": ("file.txt", small_file, "text/plain")},
        data={"people_count": 1, "client_id": "test-client"},
        headers={
            "X-Timestamp": "1234567890",
            "X-Signature": "dummy",
            "Idempotency-Key": "abc123",
            "Authorization": "Bearer dummy-token"
        }
    )
    assert response.status_code == 415
    assert response.json()["detail"] == "Unsupported file type"


def test_upload_invalid_audience(client, monkeypatch):
    monkeypatch.setattr("app.main.verify_bearer", lambda: {"sub": "test-client", "aud": "server-client"})
    small_file = io.BytesIO(b"x" * 1024)
    response = client.post(
        "/upload",
        files={"file": ("img.png", small_file, "image/png")},
        data={"people_count": 1, "client_id": "test-client"},
        headers={
            "X-Timestamp": "1234567890",
            "X-Signature": "dummy",
            "Idempotency-Key": "abc123",
            "Authorization": "Bearer invalid-token"
        }
    )
    assert response.status_code in (400, 401, 403)