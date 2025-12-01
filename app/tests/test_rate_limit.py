import pytest
from slowapi.errors import RateLimitExceeded
from app.main import app

@pytest.mark.asyncio
async def test_rate_limit_handler(client):
    class DummyLimit:
        error_message = "Too Many Requests"
    exc = RateLimitExceeded(DummyLimit())
    handler_response = await app.exception_handlers[RateLimitExceeded](None, exc)
    assert handler_response.status_code == 429