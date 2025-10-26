---

# LectureVision API 서버

📌 **개요**  
LectureVision API는 강의실 인원 수 측정과 사용자 계정 관리, 모델 서버와의 안전한 티켓 기반 연동을 제공하는 API 서버입니다.  
FastAPI + MariaDB + Redis + Alembic 기반으로 구축되었으며, Docker Compose 환경에서 손쉽게 배포할 수 있습니다.  

---

## 🛠 기술 스택
- 언어/프레임워크: Python 3.12-slim, FastAPI
- DB: MariaDB
- 캐시/티켓 관리: Redis
- ORM: SQLAlchemy (Async)
- 마이그레이션: Alembic
- 배포: Docker, Docker Compose v2 (`docker compose`)
- 리버스 프록시: ADM Reverse Proxy
- 인증: JWT + 이메일 인증(@gnu.ac.kr 전용) + 일회성 티켓

---

## 📂 프로젝트 구조
```
app/
 ├── __init__.py
 ├── main.py
 ├── db.py
 ├── models.py
 ├── schemas.py
 ├── settings.py
 ├── security.py
 ├── hmac_utils.py
 ├── services/
 │    ├── __init__.py
 │    ├── storage.py
 │    └── uploads.py
 ├── accounts/                # ✅ 계정 모듈
 │    ├── db.py
 │    ├── models.py
 │    ├── schemas.py
 │    ├── services.py
 │    ├── routes.py
 │    └── email_utils.py
 ├── tickets/                 # ✅ 티켓 모듈
 │    ├── __init__.py
 │    ├── services.py
 │    └── routes.py
 ├── middleware/
 │    └── log_exceptions.py
alembic/
 ├── env.py
 ├── script.py.mako
 └── versions/
alembic.ini
docker-compose.yml
.env.example
```

---

## ⚙ 환경 변수
`.env` 파일 예시:
```env
DB_USER=yourid
DB_PASSWORD=yourpassword
DB_HOST=mariaDB
DB_PORT=8080
DB_NAME=DBtable

JWT_SECRET=your-secret-key
JWT_ALGORITHM=HS256
JWT_EXPIRE_MINUTES=10
JWT_ISSUER=lecturevision-api
JWT_AUDIENCE=lecturevision-client

HMAC_SECRET=your-hmac-secret
HMAC_TOLERANCE_SECONDS=60

REDIS_URL=redis://redis:6666
```

---

## 🚀 실행 방법

### 1. 로컬 개발 환경
```bash
pip install -r requirements.txt
uvicorn app.main:app --reload
```

### 2. Docker 환경
```bash
docker compose up --build
```

---

## 🗄 DB 마이그레이션
```bash
# 새 마이그레이션 생성
alembic revision --autogenerate -m "메시지"

# 마이그레이션 적용
alembic upgrade head

# 롤백
alembic downgrade -1
```

---

## 📌 주요 변경점

1. **계정 모듈 추가**
   - `/accounts/signup` : 회원가입 (gnu.ac.kr 이메일만 허용, 인증 메일 발송)
   - `/accounts/verify?token=...` : 이메일 인증
   - `/accounts/login` : 로그인 후 JWT 발급
   - `/accounts/me` : JWT 기반 사용자 정보 조회

2. **티켓 모듈 추가 (Redis 기반 일회성 티켓)**
   - `/tickets/issue` : 모바일 앱이 JWT로 요청 → API 서버가 일회성 티켓 발급 (Redis 저장, TTL 5분)
   - `/tickets/validate` : 모델 서버가 티켓 제출 → API 서버가 검증 후 즉시 소모

3. **모델 서버용 토큰 발급**
   - `/token` : 모델 서버가 자체 식별자로 요청 → API 서버가 JWT 발급

4. **인증/보안 요구사항**
   - 사용자 API: `Authorization: Bearer <토큰>` 필수
   - 업로드 API: `X-Timestamp`, `X-Signature` HMAC 서명 필수
   - `Idempotency-Key` 헤더로 중복 업로드 방지

5. **요청 제한**
   - 초당 5회 요청 제한(토큰/IP 기준)

6. **파일 검증**
   - 최대 10MB
   - `image/jpeg`, `image/png`만 허용

7. **응답/에러 로깅**
   - 업로드 시간, people_count, 처리 지연(ms) 기록
   - 예외 발생 시 요청 메타데이터와 함께 로깅

8. **비동기 처리**
   - 업로드 후 알림 발송·추가 분석은 BackgroundTasks로 실행

---

## 📡 주요 API 엔드포인트

### 🔑 계정
- `POST /accounts/signup`
- `GET /accounts/verify?token=...`
- `POST /accounts/login`
- `GET /accounts/me`

### 🎟 티켓
- `POST /tickets/issue` (모바일 앱 → API 서버)
- `POST /tickets/validate` (모델 서버 → API 서버)

### 🤖 모델 서버
- `POST /token`

### 📤 업로드
- `POST /upload`
- `GET /uploads`

### 🔐 인증 방식
- 대부분의 엔드포인트는 `Authorization: Bearer <token>` 헤더 필요

---

### 📂 Upload 파일 업로드

**POST `/upload`**  
업로드 요청 (파일 + 메타데이터)

#### 요청 예제 (multipart/form-data)
```http
POST /upload HTTP/1.1
Host: your-api.com
Authorization: Bearer <token>
X-Timestamp: 2025-10-25T16:50:00Z
X-Signature: abc123signature
Idempotency-Key: upload-001
Content-Type: multipart/form-data

file: [binary file]
people_count: 3
client_id: client-xyz
```

#### 응답 예제
```json
{
  "original_name": "photo.jpg",
  "stored_name": "abc123.jpg",
  "abs_path": "/uploads/abc123.jpg",
  "people_count": 3,
  "uploaded_at": "2025-10-25T16:51:00Z",
  "client_id": "client-xyz",
  "id": 42
}
```

---

### 📂 Upload 목록 조회

**GET `/uploads`**

#### 요청 예제
```http
GET /uploads?skip=0&limit=50 HTTP/1.1
Host: your-api.com
Authorization: Bearer <token>
```

#### 응답 예제
```json
[
  {
    "original_name": "photo.jpg",
    "stored_name": "abc123.jpg",
    "abs_path": "/uploads/abc123.jpg",
    "people_count": 3,
    "uploaded_at": "2025-10-25T16:51:00Z",
    "client_id": "client-xyz",
    "id": 42
  }
]
```

---

### 🔑 토큰 발급

**POST `/token`**

#### 요청 예제
```http
POST /token?model_server_id=model-001 HTTP/1.1
Host: your-api.com
```

#### 응답 예제
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "bearer"
}
```

---

### 👤 회원가입

**POST `/accounts/signup`**

#### 요청 예제
```http
POST /accounts/signup HTTP/1.1
Host: your-api.com
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "securepassword"
}
```

#### 응답 예제
```json
{
  "id": 1,
  "email": "user@example.com",
  "created_at": "2025-10-25T16:52:00Z",
  "is_verified": false
}
```

---

### ✅ 이메일 인증

**GET `/accounts/verify`**

#### 요청 예제
```http
GET /accounts/verify?token=abc123 HTTP/1.1
Host: your-api.com
```

---

### 🔐 로그인

**POST `/accounts/login`**

#### 요청 예제
```http
POST /accounts/login HTTP/1.1
Host: your-api.com
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "securepassword"
}
```

---

### 🎟 티켓 발급

**POST `/tickets/issue`**

#### 요청 예제
```http
POST /tickets/issue HTTP/1.1
Host: your-api.com
Authorization: Bearer <token>
```

---

### 🎟 티켓 검증

**POST `/tickets/validate`**

#### 요청 예제
```http
POST /tickets/validate?ticket=abc123 HTTP/1.1
Host: your-api.com
```

---

---

## 🔒 운영 환경 체크리스트
- HTTPS 적용 (리버스 프록시에서 SSL 인증서 설정)
- 방화벽/보안그룹에서 80, 443 외 포트 차단
- `.env` 파일 외부 노출 방지
- 로그 모니터링 및 에러 알림 설정

---

📜 **라이선스**  
이 프로젝트는 내부 운영 목적 또는 별도 합의된 범위 내에서 사용됩니다.

---
