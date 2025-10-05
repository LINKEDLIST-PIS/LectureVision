import uuid, os, json
import redis.asyncio as aioredis
from fastapi import HTTPException, status

REDIS_URL = os.getenv("REDIS_URL", "redis://localhost:6379")
TICKET_EXPIRE_SECONDS = 300  # 5분

async def get_redis():
    return await aioredis.from_url(REDIS_URL, decode_responses=True)

async def issue_ticket(user_id: str):
    redis = await get_redis()
    ticket_id = str(uuid.uuid4())
    measurement_id = str(uuid.uuid4())

    data = {"user_id": user_id, "measurement_id": measurement_id}
    await redis.setex(ticket_id, TICKET_EXPIRE_SECONDS, json.dumps(data))
    return {"ticket": ticket_id, "measurement_id": measurement_id}

async def validate_ticket(ticket_id: str):
    redis = await get_redis()
    data = await redis.get(ticket_id)
    if not data:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid or expired ticket")

    await redis.delete(ticket_id)
    return json.loads(data)
