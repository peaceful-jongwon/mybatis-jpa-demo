#!/bin/bash
# setup-replication.sh
# MySQL Master-Slave 복제 설정 스크립트

echo "MySQL Master-Slave 복제 설정을 시작합니다..."

# 컨테이너가 완전히 시작될 때까지 대기
echo "컨테이너 시작 대기 중..."
sleep 30

# Master에서 바이너리 로그 상태 확인
echo "Master 바이너리 로그 상태 확인..."
MASTER_STATUS=$(docker exec mysql-master mysql -u root -prootpassword -e "SHOW MASTER STATUS\G")
echo "$MASTER_STATUS"

# GTID 기반 복제 설정 (GTID가 활성화된 경우)
echo "Slave에서 복제 설정..."
docker exec mysql-slave mysql -u root -prootpassword -e "
STOP SLAVE;
RESET SLAVE ALL;
CHANGE MASTER TO
    MASTER_HOST='mysql-master',
    MASTER_PORT=3306,
    MASTER_USER='replicator',
    MASTER_PASSWORD='replicatorpass',
    MASTER_AUTO_POSITION=1;
START SLAVE;
"

# 복제 상태 확인
echo "복제 상태 확인..."
sleep 5
docker exec mysql-slave mysql -u root -prootpassword -e "SHOW SLAVE STATUS\G" | grep -E "(Slave_IO_Running|Slave_SQL_Running|Last_Error)"

echo "복제 설정이 완료되었습니다!"
echo ""
echo "접속 정보:"
echo "Master (읽기/쓰기): localhost:3306"
echo "Slave (읽기 전용): localhost:3307"
echo ""
echo "사용자 계정:"
echo "Master - testuser/testpass (읽기/쓰기)"
echo "Slave - readonly_user/readonlypass (읽기 전용)"
echo "Slave - analyst/analystpass (읽기 전용)"