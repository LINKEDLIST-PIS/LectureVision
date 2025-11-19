from fastapi import APIRouter, Depends
from fastapi.responses import HTMLResponse
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

@router.get("/verify", response_class=HTMLResponse)
def verify(token: str, db: Session = Depends(get_db)):
    result = services.verify_user(db, token)
    if result:  # 성공
        return """
        <html>
          <head><title>계정 인증 완료</title></head>
          <body style="font-family: Arial; text-align:center; padding:50px;">
            <h1 style="color:#4CAF50;">계정 인증이 완료되었습니다!</h1>
            <p>이 창을 닫으셔도 좋습니다.</p>
          </body>
        </html>
        """
    else:  # 실패
        return """
        <html>
          <head><title>인증 실패</title></head>
          <body style="font-family: Arial; text-align:center; padding:50px;">
            <h1 style="color:red;">인증에 실패했습니다.</h1>
            <p>세션이 유효하지 않습니다.</p>
          </body>
        </html>
        """

@router.post("/login")
def login(user_in: schemas.UserLogin, db: Session = Depends(get_db)):
    return services.login_user(db, user_in)