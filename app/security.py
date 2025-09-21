import os
from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

security = HTTPBearer()

def verify_bearer(credentials: HTTPAuthorizationCredentials = Depends(security)):
    token = credentials.credentials
    expected = os.getenv("APP_BEARER_TOKEN")
    if not expected or not token or token != expected:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid auth token")
    return token  # 필요시 토큰 식별자 반환