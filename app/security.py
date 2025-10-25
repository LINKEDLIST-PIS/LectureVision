import os
from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from datetime import datetime, timedelta
from jose import jwt, JWTError
import uuid

security = HTTPBearer()
SECRET_KEY = os.getenv("JWT_SECRET")
if not SECRET_KEY:
    raise RuntimeError("JWT_SECRET 환경 변수가 존재하지 않습니다.")

ALGORITHM = os.getenv("JWT_ALGORITHM", "HS256")

ACCESS_TOKEN_EXPIRE_MINUTES = int(os.getenv("JWT_EXPIRE_MINUTES", "10"))

ISSUER = os.getenv("JWT_ISSUER", "lecturevision-api")

AUDIENCE = os.getenv("JWT_AUDIENCE", "lecturevision-client")

def create_access_token(data: dict, expires_delta: int = None):
    to_encode = data.copy()
    expire = datetime.utcnow() + timedelta(
        minutes=expires_delta or ACCESS_TOKEN_EXPIRE_MINUTES
    )
    to_encode.update({
        "exp": expire,
        "iat": datetime.utcnow(),
        "iss": ISSUER,
        "aud": AUDIENCE,
        "sub": data.get("sub"),
        "jti": str(uuid.uuid4())
    })
    return jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)

def verify_bearer(credentials: HTTPAuthorizationCredentials = Depends(security)):
    token = credentials.credentials
    try:
        payload = jwt.decode(
            token,
            SECRET_KEY,
            algorithms=[ALGORITHM],
            audience=AUDIENCE,
            issuer=ISSUER
        )
        model_server_id = payload.get("sub")
        if model_server_id != os.getenv("MODEL_SERVER_ID"):
            raise HTTPException(status_code=401, detail="Invalid model server identity")
        return payload
    except JWTError:
        raise HTTPException(status_code=401, detail="Invalid or expired token")
