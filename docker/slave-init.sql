-- slave-init.sql
-- Slave DB 초기화 스크립트

-- 읽기 전용 사용자 생성
CREATE USER IF NOT EXISTS 'readonly_user'@'%' IDENTIFIED BY 'readonlypass';
GRANT SELECT ON testdb.* TO 'readonly_user'@'%';

-- 분석용 사용자 생성 (읽기 전용)
CREATE USER IF NOT EXISTS 'analyst'@'%' IDENTIFIED BY 'analystpass';
GRANT SELECT ON testdb.* TO 'analyst'@'%';

-- Root 사용자 외부 접근 허용
CREATE USER IF NOT EXISTS 'root'@'%' IDENTIFIED BY 'rootpassword';
GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' WITH GRANT OPTION;

FLUSH PRIVILEGES;