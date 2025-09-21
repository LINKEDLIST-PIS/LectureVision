import os
import shutil
from pathlib import Path
from uuid import uuid4
from typing import Tuple

from app.settings import settings

UPLOAD_DIR = Path(settings.UPLOAD_DIR)

def save_file(file_obj, original_name: str) -> Tuple[str, str]:
    suffix = Path(original_name).suffix
    stored_name = f"{uuid4().hex}{suffix}"
    dest_path = UPLOAD_DIR / stored_name

    try:
        with open(dest_path, "wb") as buffer:
            shutil.copyfileobj(file_obj, buffer)
        os.chmod(dest_path, 0o200) # 쓰기만 가능
        return stored_name, str(dest_path)
    except Exception:
        if dest_path.exists():
            dest_path.unlink()
        raise