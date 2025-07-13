-- master-init.sql
-- Master DB 초기화 스크립트

-- 복제용 사용자 생성
CREATE USER IF NOT EXISTS 'replicator'@'%' IDENTIFIED BY 'replicatorpass';
GRANT REPLICATION SLAVE ON *.* TO 'replicator'@'%';

-- 일반 사용자 생성 (읽기/쓰기 권한)
CREATE USER IF NOT EXISTS 'testuser'@'%' IDENTIFIED BY 'testpass';
GRANT ALL PRIVILEGES ON testdb.* TO 'testuser'@'%';

-- Root 사용자 외부 접근 허용
CREATE USER IF NOT EXISTS 'root'@'%' IDENTIFIED BY 'rootpassword';
GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' WITH GRANT OPTION;

-- 테스트용 테이블 생성
USE testdb;

CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS products (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    stock INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 초기 데이터 삽입
INSERT INTO users (name, email) VALUES
    ('John Doe', 'john@example.com'),
    ('Jane Smith', 'jane@example.com'),
    ('Bob Johnson', 'bob@example.com');

INSERT INTO products (name, price, stock) VALUES
    ('Product A', 99.99, 100),
    ('Product B', 149.99, 50),
    ('Product C', 199.99, 25);

FLUSH PRIVILEGES;