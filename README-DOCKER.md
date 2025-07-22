# MyBatis-JPA Demo: Master-Slave 복제 환경 구축 가이드

이 문서는 MySQL Master-Slave 복제 환경을 Docker로 구축하고 Spring Boot 애플리케이션에서 사용하는 방법을 설명합니다.

## 📖 목차
1. [프로젝트 개요](#-프로젝트-개요)
2. [아키텍처 설명](#-아키텍처-설명)
3. [Docker 환경 구축](#-docker-환경-구축)
4. [복제 설정 확인](#-복제-설정-확인)
5. [애플리케이션 설정](#-애플리케이션-설정)
6. [테스트 방법](#-테스트-방법)
7. [트러블슈팅](#-트러블슈팅)

## 🎯 프로젝트 개요

이 프로젝트는 **MyBatis**와 **JPA**를 함께 사용하면서 **MySQL Master-Slave 복제 환경**에서 읽기/쓰기 분산을 구현하는 데모입니다.

### 주요 기능
- ✅ Master-Slave DB 자동 복제 설정
- ✅ 읽기 전용 메서드 자동 Slave 라우팅 (`@ReadOnly`)
- ✅ 쓰기 작업 Master 라우팅
- ✅ GTID 기반 안전한 복제
- ✅ 통합 테스트 환경

## 🏗 아키텍처 설명

```
┌─────────────────┐    ┌─────────────────┐
│  Spring Boot    │    │     Docker      │
│   Application   │◄──►│   Environment   │
│                 │    │                 │
│  ┌───────────┐  │    │  ┌───────────┐  │
│  │@ReadOnly  │  │    │  │   MySQL   │  │
│  │  Query    │──┼────┼──►   Slave   │  │
│  └───────────┘  │    │  │  (3307)   │  │
│                 │    │  │ READ-ONLY │  │
│  ┌───────────┐  │    │  └───────────┘  │
│  │  Write    │  │    │        ▲        │
│  │Operation  │──┼────┼────────┼────────┼┐
│  └───────────┘  │    │  ┌─────▼─────┐  ││
│                 │    │  │   MySQL   │  ││
│                 │    │  │  Master   │  ││
│                 │    │  │  (3306)   │◄─┘│
│                 │    │  │READ-WRITE │   │
└─────────────────┘    │  └───────────┘   │
                       │                  │
                       │  ┌───────────┐   │
                       │  │Replication│   │
                       │  │  Setup    │   │
                       │  │(자동실행) │   │
                       │  └───────────┘   │
                       └──────────────────┘
```

## 🐳 Docker 환경 구축

### 1. 사전 요구사항
- Docker Desktop 설치
- Docker Compose v3.8+ 지원

### 2. 프로젝트 클론 및 이동
```bash
git clone <repository-url>
cd mybatis-jpa-demo
```

### 3. Docker 컨테이너 실행
```bash
cd docker
docker-compose up -d
```

### 4. 컨테이너 상태 확인
```bash
docker ps
```

예상 결과:
```
NAMES          STATUS                    PORTS
mysql-master   Up 2 minutes (healthy)    0.0.0.0:3306->3306/tcp
mysql-slave    Up 2 minutes (healthy)    0.0.0.0:3307->3306/tcp
```

### 5. 복제 설정 로그 확인
```bash
docker-compose logs replication-setup
```

## ✅ 복제 설정 확인

### Master 상태 확인
```bash
docker exec mysql-master mysql -u root -prootpassword -e "SHOW MASTER STATUS\G"
```

### Slave 상태 확인
```bash
docker exec mysql-slave mysql -u root -prootpassword -e "SHOW SLAVE STATUS\G"
```

**정상 상태 확인 포인트:**
- `Slave_IO_Running: Yes`
- `Slave_SQL_Running: Yes` 
- `Seconds_Behind_Master: 0`
- `Last_Error: (비어있음)`

### 복제 테스트
```bash
# Master에 데이터 삽입
docker exec mysql-master mysql -u testuser -ptestpass testdb -e "
INSERT INTO users (name, email, phone_number) 
VALUES ('복제테스트', 'test@example.com', '010-1234-5678');"

# Slave에서 복제 확인 (2초 대기 후)
sleep 2
docker exec mysql-slave mysql -u testuser -ptestpass testdb -e "
SELECT * FROM users WHERE name='복제테스트';"
```

### Read-Only 확인
```bash
# Slave에서 쓰기 시도 (실패해야 함)
docker exec mysql-slave mysql -u testuser -ptestpass testdb -e "
INSERT INTO users (name, email) VALUES ('실패테스트', 'fail@test.com');"
```

예상 결과: `ERROR 1290 (HY000): The MySQL server is running with the --read-only option`

## ⚙️ 애플리케이션 설정

### 데이터베이스 연결 설정 (`application.yml`)
```yaml
spring:
  datasource:
    master:
      driver-class-name: com.mysql.cj.jdbc.Driver
      jdbc-url: jdbc:mysql://localhost:3306/testdb
      username: testuser
      password: testpass
      hikari:
        maximum-pool-size: 20
        minimum-idle: 5
        
    slave:
      driver-class-name: com.mysql.cj.jdbc.Driver
      jdbc-url: jdbc:mysql://localhost:3307/testdb
      username: testuser
      password: testpass
      hikari:
        maximum-pool-size: 15
        minimum-idle: 3
```

### 라우팅 설정
- **Master**: 쓰기 작업 (`@Transactional`)
- **Slave**: 읽기 작업 (`@ReadOnly + @Transactional(readOnly = true)`)

### 사용 예제
```java
@Service
@Transactional
public class UserService {
    
    @ReadOnly  // Slave DB로 라우팅
    @Transactional(readOnly = true)
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }
    
    @Transactional  // Master DB로 라우팅
    public User createUser(User user) {
        return userRepository.save(user);
    }
}
```

## 🧪 테스트 방법

### 1. 애플리케이션 실행
```bash
./gradlew bootRun
```

### 2. 통합 테스트 실행
```bash
# 특정 테스트 클래스 실행
./gradlew test --tests "*UserServiceMasterSlaveIntegrationTest"

# 모든 테스트 실행
./gradlew test
```

### 3. 테스트 내용
- **라우팅 테스트**: `@ReadOnly` → Slave, 쓰기 → Master
- **복제 테스트**: Master 변경사항이 Slave에 반영되는지 확인
- **권한 테스트**: Slave에서 쓰기 작업 차단 확인
- **연결 테스트**: Master/Slave 개별 연결 상태 확인

## 📊 주요 Docker 구성

### docker-compose.yml 구조
```yaml
services:
  mysql-master:
    image: mysql:8.0
    ports: ["3306:3306"]
    environment:
      MYSQL_ROOT_PASSWORD: rootpassword
      MYSQL_DATABASE: testdb
      MYSQL_USER: testuser
      MYSQL_PASSWORD: testpass
    command: >
      --server-id=1
      --log-bin=mysql-bin
      --gtid-mode=ON
      --enforce-gtid-consistency=ON

  mysql-slave:
    image: mysql:8.0
    ports: ["3307:3306"]
    command: >
      --server-id=2
      --read-only=1
      --super-read-only=1
      --gtid-mode=ON
      --enforce-gtid-consistency=ON
    depends_on:
      mysql-master:
        condition: service_healthy

  replication-setup:
    image: mysql:8.0
    depends_on:
      - mysql-master
      - mysql-slave
    restart: "no"
```

### 주요 MySQL 설정
- **GTID 모드**: 안전한 복제를 위한 Global Transaction ID
- **바이너리 로그**: 복제를 위한 변경 로그
- **Read-Only**: Slave DB 쓰기 차단
- **Super Read-Only**: Root 사용자 쓰기도 차단

## 🔧 트러블슈팅

### 1. 복제가 안될 때
```bash
# Slave 상태 확인
docker exec mysql-slave mysql -u root -prootpassword -e "SHOW SLAVE STATUS\G"

# 오류 로그 확인  
docker logs mysql-slave

# 복제 재설정
docker exec mysql-slave mysql -u root -prootpassword -e "
SET GLOBAL super_read_only=0;
STOP SLAVE;
RESET SLAVE ALL;
CHANGE MASTER TO
    MASTER_HOST='mysql-master',
    MASTER_USER='repl_user',
    MASTER_PASSWORD='repl_password',
    MASTER_AUTO_POSITION=1;
START SLAVE;
SET GLOBAL super_read_only=1;
"
```

### 2. 연결 문제
```bash
# 네트워크 확인
docker network ls
docker inspect docker_mysql-net

# 컨테이너 간 통신 테스트
docker exec mysql-slave ping mysql-master
```

### 3. 데이터베이스 초기화
```bash
# 모든 컨테이너와 볼륨 삭제
docker-compose down -v

# 다시 시작
docker-compose up -d
```

### 4. 포트 충돌
```bash
# 포트 사용 중인 프로세스 확인
lsof -i :3306
lsof -i :3307

# 필요시 프로세스 종료 후 재시작
```

## 📝 추가 정보

### 접속 정보
- **Master DB**: `localhost:3306` (읽기/쓰기)
- **Slave DB**: `localhost:3307` (읽기 전용)
- **사용자**: `testuser` / `testpass`
- **데이터베이스**: `testdb`

### 파일 구조
```
docker/
├── docker-compose.yml          # Docker 구성
├── master-init.sql            # Master 초기화 스크립트
└── setup-replication.sh      # 복제 설정 스크립트 (백업용)

src/main/java/.../config/
├── DatabaseConfig.java        # 데이터소스 설정
├── DataSourceType.java        # 라우팅 타입
├── RoutingDataSource.java     # 동적 라우팅
└── DataSourceContextHolder.java  # 스레드 로컬 컨텍스트
```

### 성능 최적화 팁
1. **Connection Pool 튜닝**: Master(20)/Slave(15) 연결 풀 크기 조정
2. **읽기 분산**: 복잡한 조회는 Slave로 라우팅
3. **트랜잭션 최소화**: 읽기 전용 트랜잭션 사용
4. **인덱스 최적화**: 자주 조회되는 컬럼에 인덱스 추가

---

## 🚀 시작하기

1. **Docker 환경 구축**
   ```bash
   cd docker && docker-compose up -d
   ```

2. **복제 상태 확인**
   ```bash
   docker exec mysql-slave mysql -u root -prootpassword -e "SHOW SLAVE STATUS\G"
   ```

3. **애플리케이션 실행**
   ```bash
   ./gradlew bootRun
   ```

4. **테스트 실행**
   ```bash
   ./gradlew test --tests "*MasterSlaveIntegrationTest"
   ```

문제가 발생하면 [트러블슈팅](#-트러블슈팅) 섹션을 참고하거나 이슈를 등록해주세요.