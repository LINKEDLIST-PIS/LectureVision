from fastapi import APIRouter, Depends
from app.security import verify_bearer
from . import services

router = APIRouter(prefix="/tickets", tags=["tickets"])

@router.post("/issue")
async def issue_ticket(payload: dict = Depends(verify_bearer)):
    user_id = payload["sub"]
    return await services.issue_ticket(user_id)

@router.post("/validate")
async def validate_ticket(ticket: str):
    return await services.validate_ticket(ticket)
