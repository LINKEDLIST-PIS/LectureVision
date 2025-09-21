from pydantic import BaseModel
from datetime import datetime

class UploadBase(BaseModel):
    original_name: str
    stored_name: str
    abs_path: str
    people_count: int
    uploaded_at: datetime

    class Config:
        from_attributes = True

class UploadCreate(BaseModel):
    people_count: int

class UploadResponse(UploadBase):
    id: int