FROM python:3.12-slim

WORKDIR /app

# 필요한 시스템 패키지 + MySQL 클라이언트 설치
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    default-mysql-client \
    curl \
 && rm -rf /var/lib/apt/lists/*

RUN pip install --upgrade pip

# FastAPI와 Uvicorn 설치
RUN pip install --no-cache-dir fastapi uvicorn sqlalchemy psycopg[binary] aiomysql pydantic-settings alembic python-multipart

# 일반 사용자 생성
RUN useradd -m -u 1001 -g 100 appuser

# 앱 소스 복사
COPY . .

# 소스와 작업 디렉토리 소유권을 일반 사용자로 변경
RUN chown -R appuser:users /app

# 일반 사용자로 전환
USER appuser

# FastAPI 실행
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000", "--reload"]

