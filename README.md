# K8s 마이크로서비스 데모

이 프로젝트는 Kubernetes 환경에서 Redis와 MariaDB를 활용하는 간단한 마이크로서비스 데모입니다.

## 프로젝트 구조

- `frontend/`: Vue.js 기반 프론트엔드
- `backend/`: Flask 기반 백엔드
- `k8s/`: Kubernetes 설정 파일

## 기능

1. MariaDB
   - 메시지 저장 및 조회
   - 시간순 정렬 지원
   - 페이지네이션 지원

2. Redis
   - API 호출 로그 저장
   - 최근 100개의 로그 유지

## 데이터베이스 설정

### MariaDB 설정

1. **데이터베이스 및 테이블 생성**
```sql
CREATE DATABASE testdb;
USE testdb;

CREATE TABLE messages (
    id INT AUTO_INCREMENT PRIMARY KEY,
    message TEXT,
    created_at DATETIME
);
```

2. **사용자 생성 및 권한 설정**
```sql
CREATE USER 'testuser'@'%' IDENTIFIED BY 'testpassword';
GRANT ALL PRIVILEGES ON testdb.* TO 'testuser'@'%';
FLUSH PRIVILEGES;
```

3. **MariaDB 연결 정보**
- Host: my-mariadb
- Database: testdb
- User: testuser
- Password: testpassword

### Redis 설정

1. **Redis 연결 정보**
- Host: my-redis-master
- Port: 6379
- Password: {{비밀번호 입력}}

2. **Redis 데이터 구조**
- Key: api_logs
- Type: List
- Format: JSON
```json
{
    "timestamp": "ISO-8601 format",
    "action": "action_name",
    "details": "log details"
}
```

## 환경 변수 설정

### 백엔드 환경 변수
```yaml
- name: MYSQL_HOST
  value: "my-mariadb"
- name: MYSQL_USER
  value: "testuser"
- name: MYSQL_PASSWORD
  value: "testpassword"
- name: REDIS_HOST
  value: "my-redis-master"
- name: REDIS_PASSWORD
  value: "undIJzFiRi"
```

## 설치 및 실행

### 사전 요구사항

- Rancher Desktop
- Redis
- MariaDB

### 백엔드 실행

```bash
cd backend
pip install -r requirements.txt
python app.py
```

### 프론트엔드 실행

```bash
cd frontend
npm install
npm run serve
```

## API 엔드포인트

### MariaDB
- POST /db/message: 메시지 저장
- GET /db/messages: 모든 메시지 조회 (페이지네이션 지원)
  - Query Parameters:
    - offset: 시작 위치 (기본값: 0)
    - limit: 페이지 크기 (기본값: 20)

### Redis
- GET /logs/redis: API 호출 로그 조회 