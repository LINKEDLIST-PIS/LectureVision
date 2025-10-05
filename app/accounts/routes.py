from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from .db import SessionLocal
from . import services, schemas

router = APIRouter(prefix="/accounts", tags=["accounts"])

def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

@router.post("/signup", response_model=schemas.UserResponse)
async def signup(user_in: schemas.UserCreate, db: Session = Depends(get_db)):
    return await services.create_user(db, user_in)

@router.get("/verify")
def verify(token: str, db: Session = Depends(get_db)):
    return services.verify_user(db, token)

@router.post("/login")
def login(user_in: schemas.UserLogin, db: Session = Depends(get_db)):
    return services.login_user(db, user_in)
