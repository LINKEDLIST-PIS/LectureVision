from pydantic import BaseModel
from datetime import datetime
from typing import Optional

class UploadBase(BaseModel):
    original_name: str
    stored_name: str
    abs_path: str
    people_count: int
    uploaded_at: datetime
    client_id: Optional[str] = None

    class Config:
        from_attributes = True

class UploadCreate(BaseModel):
    people_count: int
    client_id: Optional[str] = None

class UploadResponse(UploadBase):
    id: int
