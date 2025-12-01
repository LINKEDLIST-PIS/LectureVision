from pydantic_settings import BaseSettings
from pydantic import ConfigDict   # 추가

class Settings(BaseSettings):
    DB_USER: str
    DB_PASSWORD: str
    DB_HOST: str
    DB_PORT: int = 3306
    DB_NAME: str
    UPLOAD_DIR: str = "/secure_data"

    model_config = ConfigDict(env_file=".env")

settings = Settings()
