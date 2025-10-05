from fastapi import HTTPException, status
from passlib.context import CryptContext
from sqlalchemy.orm import Session
from jose import jwt
from datetime import datetime, timedelta
import uuid

from .models import User
from .schemas import UserCreate, UserLogin
from .email_utils import send_verification_email

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")

SECRET_KEY = "JWT_SECRET"
ALGORITHM = "HS256"
ACCESS_TOKEN_EXPIRE_MINUTES = 60

# 회원가입
async def create_user(db: Session, user_in: UserCreate):
    if not user_in.email.endswith("@gnu.ac.kr"):
        raise HTTPException(status_code=400, detail="gnu.ac.kr 이메일만 허용됩니다.")

    hashed_pw = pwd_context.hash(user_in.password)
    token = str(uuid.uuid4())

    user = User(
        email=user_in.email,
        hashed_password=hashed_pw,
        is_verified=False,
        verification_token=token
    )
    db.add(user)
    db.commit()
    db.refresh(user)

    await send_verification_email(user.email, token)
    return user

# 이메일 인증
def verify_user(db: Session, token: str):
    user = db.query(User).filter(User.verification_token == token).first()
    if not user:
        raise HTTPException(status_code=400, detail="잘못된 토큰입니다.")
    user.is_verified = True
    user.verification_token = None
    db.commit()
    db.refresh(user)
    return user

# 로그인
def verify_password(plain_password, hashed_password):
    return pwd_context.verify(plain_password, hashed_password)

def authenticate_user(db: Session, email: str, password: str):
    user = db.query(User).filter(User.email == email).first()
    if not user or not verify_password(password, user.hashed_password):
        return None
    if not user.is_verified:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="이메일 인증 필요")
    return user

def create_access_token(data: dict, expires_delta: timedelta | None = None):
    to_encode = data.copy()
    expire = datetime.utcnow() + (expires_delta or timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES))
    to_encode.update({"exp": expire})
    return jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)

def login_user(db: Session, user_in: UserLogin):
    user = authenticate_user(db, user_in.email, user_in.password)
    if not user:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="이메일 또는 비밀번호 오류")
    access_token = create_access_token(data={"sub": str(user.id)})
    return {"access_token": access_token, "token_type": "bearer"}
