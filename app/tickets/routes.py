from fastapi import APIRouter, Depends, HTTPException
from app.security import AUDIENCE_CLIENT, verify_bearer
from . import services

router = APIRouter(prefix="/tickets", tags=["tickets"])

@router.post("/issue")
async def issue_ticket(payload: dict = Depends(verify_bearer)):
    if payload.get("aud") != AUDIENCE_CLIENT:
        raise HTTPException(status_code=403, detail="Only client tokens can issue tickets")
    user_id = payload["sub"]
    return await services.issue_ticket(user_id)

@router.post("/validate")
async def validate_ticket(ticket: str):
    return await services.validate_ticket(ticket)
