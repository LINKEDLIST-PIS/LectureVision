`markdown

LectureVision API 서버

📌 개요
LectureVision은 강의실 인원 수 및 관련 데이터를 관리·처리하는 API 서버입니다.  
FastAPI + MySQL + Alembic 기반으로 구축되었으며, Docker 환경에서 손쉽게 배포할 수 있습니다.

---

🛠 기술 스택
- 언어/프레임워크: Python 3.12, FastAPI
- DB: MySQL 8.x
- ORM: SQLAlchemy (Async)
- 마이그레이션: Alembic
- 배포: Docker, Docker Compose
- 리버스 프록시: Nginx

---

📂 프로젝트 구조
`
app/
 ├── init.py
 ├── main.py
 ├── db.py
 ├── models.py
 ├── schemas.py
 ├── settings.py
 ├── services/
 │    ├── init.py
 │    ├── storage.py
 │    └── uploads.py
alembic/
 ├── env.py
 ├── script.py.mako
 └── versions/
alembic.ini
.docker-compose.yml
.env.example
`

---

⚙ 환경 변수
.env 파일 예시:
`env
DB_USER=appuser
DB_PASSWORD=apppass
DB_HOST=db
DB_PORT=3306
DB_NAME=lecturevision
`

---

🚀 실행 방법

1. 로컬 개발 환경
`bash
pip install -r requirements.txt
uvicorn app.main:app --reload
`

2. Docker 환경
`bash
docker-compose up --build
`

---

🗄 DB 마이그레이션

새 마이그레이션 생성
`bash
alembic revision --autogenerate -m "메시지"
`

마이그레이션 적용
`bash
alembic upgrade head
`

롤백
`bash
alembic downgrade -1
`

---

📡 API 명세
`
---
title: FastAPI v0.1.0
language_tabs:
  - shell: Shell
  - http: HTTP
  - javascript: JavaScript
  - ruby: Ruby
  - python: Python
  - php: PHP
  - java: Java
  - go: Go
toc_footers: []
includes: []
search: true
highlight_theme: darkula
headingLevel: 2

---

<!-- Generator: Widdershins v4.0.1 -->

<h1 id="fastapi">FastAPI v0.1.0</h1>

> Scroll down for code samples, example requests and responses. Select a language for code samples from the tabs above or the mobile navigation menu.

<h1 id="fastapi-default">Default</h1>

## upload_file_upload_post

<a id="opIdupload_file_upload_post"></a>

> Code samples

```shell
# You can also use wget
curl -X POST /upload \
  -H 'Content-Type: multipart/form-data' \
  -H 'Accept: application/json'

```

```http
POST /upload HTTP/1.1

Content-Type: multipart/form-data
Accept: application/json

```

```javascript
const inputBody = '{
  "file": "string",
  "people_count": 0
}';
const headers = {
  'Content-Type':'multipart/form-data',
  'Accept':'application/json'
};

fetch('/upload',
{
  method: 'POST',
  body: inputBody,
  headers: headers
})
.then(function(res) {
    return res.json();
}).then(function(body) {
    console.log(body);
});

```

```ruby
require 'rest-client'
require 'json'

headers = {
  'Content-Type' => 'multipart/form-data',
  'Accept' => 'application/json'
}

result = RestClient.post '/upload',
  params: {
  }, headers: headers

p JSON.parse(result)

```

```python
import requests
headers = {
  'Content-Type': 'multipart/form-data',
  'Accept': 'application/json'
}

r = requests.post('/upload', headers = headers)

print(r.json())

```

```php
<?php

require 'vendor/autoload.php';

$headers = array(
    'Content-Type' => 'multipart/form-data',
    'Accept' => 'application/json',
);

$client = new \GuzzleHttp\Client();

// Define array of request body.
$request_body = array();

try {
    $response = $client->request('POST','/upload', array(
        'headers' => $headers,
        'json' => $request_body,
       )
    );
    print_r($response->getBody()->getContents());
 }
 catch (\GuzzleHttp\Exception\BadResponseException $e) {
    // handle exception or api errors.
    print_r($e->getMessage());
 }

 // ...

```

```java
URL obj = new URL("/upload");
HttpURLConnection con = (HttpURLConnection) obj.openConnection();
con.setRequestMethod("POST");
int responseCode = con.getResponseCode();
BufferedReader in = new BufferedReader(
    new InputStreamReader(con.getInputStream()));
String inputLine;
StringBuffer response = new StringBuffer();
while ((inputLine = in.readLine()) != null) {
    response.append(inputLine);
}
in.close();
System.out.println(response.toString());

```

```go
package main

import (
       "bytes"
       "net/http"
)

func main() {

    headers := map[string][]string{
        "Content-Type": []string{"multipart/form-data"},
        "Accept": []string{"application/json"},
    }

    data := bytes.NewBuffer([]byte{jsonReq})
    req, err := http.NewRequest("POST", "/upload", data)
    req.Header = headers

    client := &http.Client{}
    resp, err := client.Do(req)
    // ...
}

```

`POST /upload`

*Upload File*

> Body parameter

```yaml
file: string
people_count: 0

```

<h3 id="upload_file_upload_post-parameters">Parameters</h3>

|Name|In|Type|Required|Description|
|---|---|---|---|---|
|body|body|[Body_upload_file_upload_post](#schemabody_upload_file_upload_post)|true|none|

> Example responses

> 200 Response

```json
{
  "original_name": "string",
  "stored_name": "string",
  "abs_path": "string",
  "people_count": 0,
  "uploaded_at": "2019-08-24T14:15:22Z",
  "id": 0
}
```

<h3 id="upload_file_upload_post-responses">Responses</h3>

|Status|Meaning|Description|Schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|Successful Response|[UploadResponse](#schemauploadresponse)|
|422|[Unprocessable Entity](https://tools.ietf.org/html/rfc2518#section-10.3)|Validation Error|[HTTPValidationError](#schemahttpvalidationerror)|

<aside class="success">
This operation does not require authentication
</aside>

## get_uploads_uploads_get

<a id="opIdget_uploads_uploads_get"></a>

> Code samples

```shell
# You can also use wget
curl -X GET /uploads \
  -H 'Accept: application/json'

```

```http
GET /uploads HTTP/1.1

Accept: application/json

```

```javascript

const headers = {
  'Accept':'application/json'
};

fetch('/uploads',
{
  method: 'GET',

  headers: headers
})
.then(function(res) {
    return res.json();
}).then(function(body) {
    console.log(body);
});

```

```ruby
require 'rest-client'
require 'json'

headers = {
  'Accept' => 'application/json'
}

result = RestClient.get '/uploads',
  params: {
  }, headers: headers

p JSON.parse(result)

```

```python
import requests
headers = {
  'Accept': 'application/json'
}

r = requests.get('/uploads', headers = headers)

print(r.json())

```

```php
<?php

require 'vendor/autoload.php';

$headers = array(
    'Accept' => 'application/json',
);

$client = new \GuzzleHttp\Client();

// Define array of request body.
$request_body = array();

try {
    $response = $client->request('GET','/uploads', array(
        'headers' => $headers,
        'json' => $request_body,
       )
    );
    print_r($response->getBody()->getContents());
 }
 catch (\GuzzleHttp\Exception\BadResponseException $e) {
    // handle exception or api errors.
    print_r($e->getMessage());
 }

 // ...

```

```java
URL obj = new URL("/uploads");
HttpURLConnection con = (HttpURLConnection) obj.openConnection();
con.setRequestMethod("GET");
int responseCode = con.getResponseCode();
BufferedReader in = new BufferedReader(
    new InputStreamReader(con.getInputStream()));
String inputLine;
StringBuffer response = new StringBuffer();
while ((inputLine = in.readLine()) != null) {
    response.append(inputLine);
}
in.close();
System.out.println(response.toString());

```

```go
package main

import (
       "bytes"
       "net/http"
)

func main() {

    headers := map[string][]string{
        "Accept": []string{"application/json"},
    }

    data := bytes.NewBuffer([]byte{jsonReq})
    req, err := http.NewRequest("GET", "/uploads", data)
    req.Header = headers

    client := &http.Client{}
    resp, err := client.Do(req)
    // ...
}

```

`GET /uploads`

*Get Uploads*

<h3 id="get_uploads_uploads_get-parameters">Parameters</h3>

|Name|In|Type|Required|Description|
|---|---|---|---|---|
|skip|query|integer|false|none|
|limit|query|integer|false|none|

> Example responses

> 200 Response

```json
[
  {
    "original_name": "string",
    "stored_name": "string",
    "abs_path": "string",
    "people_count": 0,
    "uploaded_at": "2019-08-24T14:15:22Z",
    "id": 0
  }
]
```

<h3 id="get_uploads_uploads_get-responses">Responses</h3>

|Status|Meaning|Description|Schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|Successful Response|Inline|
|422|[Unprocessable Entity](https://tools.ietf.org/html/rfc2518#section-10.3)|Validation Error|[HTTPValidationError](#schemahttpvalidationerror)|

<h3 id="get_uploads_uploads_get-responseschema">Response Schema</h3>

Status Code **200**

*Response Get Uploads Uploads Get*

|Name|Type|Required|Restrictions|Description|
|---|---|---|---|---|
|Response Get Uploads Uploads Get|[[UploadResponse](#schemauploadresponse)]|false|none|none|
|» UploadResponse|[UploadResponse](#schemauploadresponse)|false|none|none|
|»» original_name|string|true|none|none|
|»» stored_name|string|true|none|none|
|»» abs_path|string|true|none|none|
|»» people_count|integer|true|none|none|
|»» uploaded_at|string(date-time)|true|none|none|
|»» id|integer|true|none|none|

<aside class="success">
This operation does not require authentication
</aside>

# Schemas

<h2 id="tocS_Body_upload_file_upload_post">Body_upload_file_upload_post</h2>
<!-- backwards compatibility -->
<a id="schemabody_upload_file_upload_post"></a>
<a id="schema_Body_upload_file_upload_post"></a>
<a id="tocSbody_upload_file_upload_post"></a>
<a id="tocsbody_upload_file_upload_post"></a>

```json
{
  "file": "string",
  "people_count": 0
}

```

Body_upload_file_upload_post

### Properties

|Name|Type|Required|Restrictions|Description|
|---|---|---|---|---|
|file|string(binary)|true|none|none|
|people_count|integer|true|none|none|

<h2 id="tocS_HTTPValidationError">HTTPValidationError</h2>
<!-- backwards compatibility -->
<a id="schemahttpvalidationerror"></a>
<a id="schema_HTTPValidationError"></a>
<a id="tocShttpvalidationerror"></a>
<a id="tocshttpvalidationerror"></a>

```json
{
  "detail": [
    {
      "loc": [
        "string"
      ],
      "msg": "string",
      "type": "string"
    }
  ]
}

```

HTTPValidationError

### Properties

|Name|Type|Required|Restrictions|Description|
|---|---|---|---|---|
|detail|[[ValidationError](#schemavalidationerror)]|false|none|none|

<h2 id="tocS_UploadResponse">UploadResponse</h2>
<!-- backwards compatibility -->
<a id="schemauploadresponse"></a>
<a id="schema_UploadResponse"></a>
<a id="tocSuploadresponse"></a>
<a id="tocsuploadresponse"></a>

```json
{
  "original_name": "string",
  "stored_name": "string",
  "abs_path": "string",
  "people_count": 0,
  "uploaded_at": "2019-08-24T14:15:22Z",
  "id": 0
}

```

UploadResponse

### Properties

|Name|Type|Required|Restrictions|Description|
|---|---|---|---|---|
|original_name|string|true|none|none|
|stored_name|string|true|none|none|
|abs_path|string|true|none|none|
|people_count|integer|true|none|none|
|uploaded_at|string(date-time)|true|none|none|
|id|integer|true|none|none|

<h2 id="tocS_ValidationError">ValidationError</h2>
<!-- backwards compatibility -->
<a id="schemavalidationerror"></a>
<a id="schema_ValidationError"></a>
<a id="tocSvalidationerror"></a>
<a id="tocsvalidationerror"></a>

```json
{
  "loc": [
    "string"
  ],
  "msg": "string",
  "type": "string"
}

```

ValidationError

### Properties

|Name|Type|Required|Restrictions|Description|
|---|---|---|---|---|
|loc|[anyOf]|true|none|none|

anyOf

|Name|Type|Required|Restrictions|Description|
|---|---|---|---|---|
|» *anonymous*|string|false|none|none|

or

|Name|Type|Required|Restrictions|Description|
|---|---|---|---|---|
|» *anonymous*|integer|false|none|none|

continued

|Name|Type|Required|Restrictions|Description|
|---|---|---|---|---|
|msg|string|true|none|none|
|type|string|true|none|none|

`

---

🔒 운영 환경 체크리스트
- HTTPS 적용 (리버스 프록시에서 SSL 인증서 설정)
- 방화벽/보안그룹에서 80, 443 외 포트 차단
- .env 파일 외부 노출 방지
- 로그 모니터링 및 에러 알림 설정

---

📜 라이선스
이 프로젝트는 내부 운영 목적 또는 별도 합의된 범위 내에서 사용됩니다.
`

---