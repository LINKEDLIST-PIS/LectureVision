from sqlalchemy import Column, Integer, String, DateTime, Boolean, func
from .db import Base

class User(Base):
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, index=True)
    email = Column(String, unique=True, index=True, nullable=False)
    hashed_password = Column(String, nullable=False)
    created_at = Column(DateTime(timezone=True), server_default=func.now())

    is_verified = Column(Boolean, default=False, nullable=False)
    verification_token = Column(String, unique=True, index=True, nullable=True)
