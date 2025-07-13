#!/bin/bash
# test-replication.sh
# Master-Slave 복제 테스트 스크립트

echo "=== MySQL Master-Slave 복제 테스트 ==="
echo ""

# 1. Master에 데이터 삽입
echo "1. Master에 새 사용자 추가..."
docker exec mysql-master mysql -u testuser -ptestpass -e "
USE testdb;
INSERT INTO users (name, email) VALUES ('Test User', 'test@example.com');
SELECT * FROM users ORDER BY id DESC LIMIT 1;
"

echo ""
echo "잠시 대기 중... (복제 동기화)"
sleep 3

# 2. Slave에서 데이터 확인
echo "2. Slave에서 데이터 확인..."
docker exec mysql-slave mysql -u readonly_user -preadonlypass -e "
USE testdb;
SELECT * FROM users ORDER BY id DESC LIMIT 1;
"

echo ""

# 3. Slave에서 쓰기 시도 (실패해야 함)
echo "3. Slave에서 쓰기 시도 (실패해야 함)..."
docker exec mysql-slave mysql -u readonly_user -preadonlypass -e "
USE testdb;
INSERT INTO users (name, email) VALUES ('Should Fail', 'fail@example.com');
" 2>&1 || echo "예상대로 쓰기가 실패했습니다 (읽기 전용 모드)"

echo ""

# 4. 복제 상태 확인
echo "4. 복제 상태 확인..."
docker exec mysql-slave mysql -u root -prootpassword -e "
SHOW SLAVE STATUS\G
" | grep -E "(Slave_IO_Running|Slave_SQL_Running|Seconds_Behind_Master|Last_.*Error)"

echo ""

# 5. Master와 Slave 데이터 카운트 비교
echo "5. 데이터 동기화 상태 확인..."
MASTER_COUNT=$(docker exec mysql-master mysql -u root -prootpassword -e "USE testdb; SELECT COUNT(*) FROM users;" | tail -1)
SLAVE_COUNT=$(docker exec mysql-slave mysql -u root -prootpassword -e "USE testdb; SELECT COUNT(*) FROM users;" | tail -1)

echo "Master 사용자 수: $MASTER_COUNT"
echo "Slave 사용자 수: $SLAVE_COUNT"

if [ "$MASTER_COUNT" = "$SLAVE_COUNT" ]; then
    echo "✅ 데이터 동기화 성공!"
else
    echo "❌ 데이터 동기화 실패!"
fi

echo ""
echo "=== 테스트 완료 ==="