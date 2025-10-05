FROM python:3.12-slim

WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    default-mysql-client \
    curl \
 && rm -rf /var/lib/apt/lists/*

RUN pip install --upgrade pip

RUN pip install --no-cache-dir fastapi uvicorn sqlalchemy psycopg[binary] aiomysql pydantic-settings alembic python-multipart python-jose[cryptography] slowapi passlib[bcrypt] email-validator pyjwt fastapi-mail redis

RUN useradd -m -u 1001 -g 100 appuser

COPY . .

RUN chown -R appuser:users /app

USER appuser

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000", "--reload"]

