#!/bin/bash
# setup-replication.sh
# MySQL Master-Slave 복제 설정 스크립트

set -e  # 에러 발생 시 스크립트 중단

echo "MySQL Master-Slave 복제 설정을 시작합니다..."

# 컨테이너 상태 확인 함수
wait_for_container() {
    local container_name=$1
    local max_attempts=60
    local attempt=1
    
    echo "컨테이너 $container_name 준비 상태 확인 중..."
    
    while [ $attempt -le $max_attempts ]; do
        if docker exec $container_name mysqladmin ping -h localhost -u root -prootpassword --silent; then
            echo "컨테이너 $container_name 준비 완료!"
            return 0
        fi
        echo "시도 $attempt/$max_attempts: 컨테이너 $container_name 대기 중..."
        sleep 2
        ((attempt++))
    done
    
    echo "에러: 컨테이너 $container_name이 준비되지 않았습니다."
    return 1
}

# Master와 Slave 컨테이너 상태 확인
wait_for_container "mysql-master"
wait_for_container "mysql-slave"

# Master에서 복제 사용자 존재 확인
echo "복제 사용자 확인 중..."
if ! docker exec mysql-master mysql -u root -prootpassword -e "SELECT User FROM mysql.user WHERE User='repl_user';" | grep -q "repl_user"; then
    echo "에러: 복제 사용자 'repl_user'가 Master에 존재하지 않습니다."
    echo "master-init.sql 파일을 확인하고 컨테이너를 다시 시작하세요."
    exit 1
fi

# Master에서 GTID 상태 확인
echo "Master GTID 상태 확인..."
GTID_MODE=$(docker exec mysql-master mysql -u root -prootpassword -e "SELECT @@gtid_mode;" -s)
if [ "$GTID_MODE" != "ON" ]; then
    echo "에러: Master에서 GTID가 활성화되지 않았습니다. GTID Mode: $GTID_MODE"
    exit 1
fi

# Master에서 바이너리 로그 상태 확인
echo "Master 바이너리 로그 상태 확인..."
MASTER_STATUS=$(docker exec mysql-master mysql -u root -prootpassword -e "SHOW MASTER STATUS\G")
echo "$MASTER_STATUS"

# Slave에서 GTID 상태 확인
echo "Slave GTID 상태 확인..."
SLAVE_GTID_MODE=$(docker exec mysql-slave mysql -u root -prootpassword -e "SELECT @@gtid_mode;" -s)
if [ "$SLAVE_GTID_MODE" != "ON" ]; then
    echo "에러: Slave에서 GTID가 활성화되지 않았습니다. GTID Mode: $SLAVE_GTID_MODE"
    exit 1
fi

# Slave에서 복제 설정
echo "Slave에서 복제 설정..."
if docker exec mysql-slave mysql -u root -prootpassword -e "
STOP SLAVE;
RESET SLAVE ALL;
CHANGE MASTER TO
    MASTER_HOST='mysql-master',
    MASTER_PORT=3306,
    MASTER_USER='repl_user',
    MASTER_PASSWORD='repl_password',
    MASTER_AUTO_POSITION=1;
START SLAVE;
"; then
    echo "복제 설정 명령 실행 완료"
else
    echo "에러: 복제 설정 명령 실행 실패"
    exit 1
fi

# 복제 상태 확인 (상세)
echo "복제 상태 확인..."
sleep 5

SLAVE_STATUS=$(docker exec mysql-slave mysql -u root -prootpassword -e "SHOW SLAVE STATUS\G")
IO_RUNNING=$(echo "$SLAVE_STATUS" | grep "Slave_IO_Running:" | awk '{print $2}')
SQL_RUNNING=$(echo "$SLAVE_STATUS" | grep "Slave_SQL_Running:" | awk '{print $2}')
LAST_ERROR=$(echo "$SLAVE_STATUS" | grep "Last_Error:" | cut -d':' -f2- | sed 's/^ *//')

echo "IO Thread 상태: $IO_RUNNING"
echo "SQL Thread 상태: $SQL_RUNNING"

if [ "$LAST_ERROR" ]; then
    echo "마지막 에러: $LAST_ERROR"
fi

if [ "$IO_RUNNING" = "Yes" ] && [ "$SQL_RUNNING" = "Yes" ]; then
    echo "✅ 복제 설정이 성공적으로 완료되었습니다!"
else
    echo "❌ 복제 설정에 문제가 있습니다."
    echo "전체 Slave 상태:"
    echo "$SLAVE_STATUS"
    exit 1
fi

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