import os
from fastapi import Depends, HTTPException
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from datetime import datetime, timedelta, timezone
from jose import jwt, JWTError
from sqlalchemy.orm import Session
from app.accounts.models import User
from app.accounts.db import get_db
import uuid

security = HTTPBearer()
SECRET_KEY = os.getenv("JWT_SECRET")
if not SECRET_KEY:
    raise RuntimeError("JWT_SECRET 환경 변수가 존재하지 않습니다.")

ALGORITHM = os.getenv("JWT_ALGORITHM", "HS256")
ACCESS_TOKEN_EXPIRE_MINUTES = int(os.getenv("JWT_EXPIRE_MINUTES", "60"))

ISSUER = os.getenv("JWT_ISSUER", "lecturevision-api")

AUDIENCE_CLIENT = os.getenv("JWT_AUDIENCE_CLIENT", "mobile-client")
AUDIENCE_SERVER = os.getenv("JWT_AUDIENCE_SERVER", "server-client")

MODEL_SERVER_ID = os.getenv("MODEL_SERVER_ID")

def create_access_token(data: dict, audience: str, expires_delta: int = None):
    to_encode = data.copy()
    expire = datetime.now(timezone.utc) + timedelta(
        minutes=expires_delta or ACCESS_TOKEN_EXPIRE_MINUTES
    )
    to_encode.update({
        "exp": expire,
        "iat": datetime.now(timezone.utc),
        "iss": ISSUER,
        "aud": audience,
        "sub": data.get("sub"),
        "jti": str(uuid.uuid4())
    })
    return jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)

def verify_bearer(
    credentials: HTTPAuthorizationCredentials = Depends(security),
    db: Session = Depends(get_db)
):
    token = credentials.credentials
    try:
        payload = jwt.decode(
            token,
            SECRET_KEY,
            algorithms=[ALGORITHM],
            issuer=ISSUER,
            options={"verify_aud": False}
        )
        aud = payload.get("aud")
        sub = payload.get("sub")

        if aud == AUDIENCE_CLIENT:
            user = db.query(User).filter(User.email == sub).first()
            if not user:
                raise HTTPException(status_code=401, detail="User not found")
            if not user.is_verified:
                raise HTTPException(status_code=403, detail="Email not verified")
            return payload

        elif aud == AUDIENCE_SERVER:
            if sub != MODEL_SERVER_ID:
                raise HTTPException(status_code=401, detail="Invalid model server identity")
            return payload

        else:
            raise HTTPException(status_code=401, detail="Unknown audience")

    except JWTError:
        raise HTTPException(status_code=401, detail="Invalid or expired token")