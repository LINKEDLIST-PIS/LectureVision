from app.main import app, verify_bearer
from app.db import get_db

def fake_get_db():
    class DummyResult:
        def fetchall(self):
            return []
    class DummySession:
        async def execute(self, *args, **kwargs):
            return DummyResult()
        def close(self):
            pass
    yield DummySession()

def test_get_uploads_requires_auth(client):
    app.dependency_overrides.clear()
    app.dependency_overrides[get_db] = fake_get_db
    response = client.get("/uploads")
    assert response.status_code in (401, 403)
    app.dependency_overrides.clear()

def test_get_uploads_with_auth(client, monkeypatch):
    app.dependency_overrides[get_db] = fake_get_db
    app.dependency_overrides[verify_bearer] = lambda: {"sub": "test-client"}
    response = client.get("/uploads")
    assert response.status_code == 200
    assert isinstance(response.json(), list)
    app.dependency_overrides.clear()