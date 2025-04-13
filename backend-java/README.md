# Java 백엔드 애플리케이션

이 프로젝트는 Flask 기반 백엔드를 Spring Boot를 사용한 Java 백엔드로 마이그레이션한 것입니다.

# 로컬환경 테스트 (클라우드 환경이 아닌 로컬환경에 구성됨)
./gradlew clean build bootRun --args='--spring.profiles.active=local'


## 기능

- MariaDB를 사용한 메시지 저장 및 조회
- Redis를 사용한 로깅 기능
- REST API 엔드포인트 제공

## 환경 변수

애플리케이션은 다음 환경 변수를 사용합니다:

- `MYSQL_HOST`: MariaDB 호스트 (기본값: my-mariadb)
- `MYSQL_USER`: MariaDB 사용자 (기본값: testuser)
- `MYSQL_PASSWORD`: MariaDB 비밀번호
- `REDIS_HOST`: Redis 호스트 (기본값: my-redis-master)
- `REDIS_PASSWORD`: Redis 비밀번호

## 빌드 및 실행

### Gradle로 로컬에서 실행

```bash
./gradlew bootRun
```

### Docker로 빌드 및 실행

```bash
docker build -t backend-java .
docker run -p 5000:5000 -e MYSQL_PASSWORD=YOUR_PASSWORD -e REDIS_PASSWORD=YOUR_PASSWORD backend-java
```

## API 엔드포인트

- `POST /db/message`: 메시지 저장
  - 요청 본문: `{"message": "your message"}`
  - 응답: `{"status": "success"}`

- `GET /db/messages`: 모든 메시지 조회
  - 응답: 메시지 목록

- `GET /logs/redis`: Redis 로그 조회
  - 응답: 로그 엔트리 목록 