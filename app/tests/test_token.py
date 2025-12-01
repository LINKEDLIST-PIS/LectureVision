def test_issue_token_success(client):
    response = client.post("/token", params={"model_server_id": "test-model"})
    assert response.status_code == 200
    data = response.json()
    assert "access_token" in data
    assert data["token_type"] == "bearer"