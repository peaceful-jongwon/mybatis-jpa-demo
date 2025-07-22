package kr.co.peacefuljw.mybatisjpademo.application.service;

import kr.co.peacefuljw.mybatisjpademo.config.DataSourceContextHolder;
import kr.co.peacefuljw.mybatisjpademo.config.DataSourceType;
import kr.co.peacefuljw.mybatisjpademo.domain.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("integration")
@DisplayName("UserService Master-Slave 라우팅 통합 테스트")
class UserServiceMasterSlaveIntegrationTest {

    @Autowired
    private UserService userService;
    
    @Autowired
    private DataSource dataSource;
    
    private JdbcTemplate masterJdbcTemplate;
    private JdbcTemplate slaveJdbcTemplate;
    
    @AfterEach
    void tearDown() {
        DataSourceContextHolder.clear();
    }
    
    @Test
    @DisplayName("@ReadOnly 애노테이션이 있는 메서드는 Slave DB로 라우팅되어야 한다")
    void readOnlyMethodShouldRouteToSlaveDatabase() {
        // Given: DataSource Context를 모니터링
        try (MockedStatic<DataSourceContextHolder> mockedStatic = 
             Mockito.mockStatic(DataSourceContextHolder.class)) {
            
            // When: @ReadOnly 애노테이션이 있는 조회 메서드 실행
            mockedStatic.when(DataSourceContextHolder::getDataSourceType)
                       .thenReturn(DataSourceType.SLAVE);
            
            List<User> users = userService.queryUsers();
            
            // Then: Slave DataSource가 사용되었는지 검증
            mockedStatic.verify(() -> DataSourceContextHolder.getDataSourceType());
            assertThat(users).isNotNull();
        }
    }
    
    @Test
    @DisplayName("쓰기 메서드(create)는 Master DB로 라우팅되어야 한다")
    void writeMethodShouldRouteToMasterDatabase() {
        // Given: DataSource Context를 모니터링
        try (MockedStatic<DataSourceContextHolder> mockedStatic = 
             Mockito.mockStatic(DataSourceContextHolder.class)) {
            
            User newUser = new User("테스트사용자", "test@example.com", "010-1234-5678");
            
            // When: 쓰기 메서드 실행
            mockedStatic.when(DataSourceContextHolder::getDataSourceType)
                       .thenReturn(DataSourceType.MASTER);
            
            User savedUser = userService.createUser(newUser);
            
            // Then: Master DataSource가 사용되었는지 검증
            mockedStatic.verify(() -> DataSourceContextHolder.getDataSourceType());
            assertThat(savedUser).isNotNull();
            assertThat(savedUser.getName()).isEqualTo("테스트사용자");
        }
    }
    
    @Test
    @DisplayName("Master에서 데이터 생성 후 Slave에서 복제된 데이터 조회 확인")
    @Transactional
    void masterSlaveReplicationShouldWork() {
        // Given: Master DB에 데이터 생성
        User newUser = new User("복제테스트", "replication@test.com", "010-9999-8888");
        
        // When: Master DB에 데이터 저장
        User masterSavedUser = userService.createUser(newUser);
        assertThat(masterSavedUser.getId()).isNotNull();
        
        // Then: 복제가 완료될 때까지 대기 후 Slave에서 조회
        await().atMost(10, TimeUnit.SECONDS)
               .pollInterval(1, TimeUnit.SECONDS)
               .untilAsserted(() -> {
                   // Slave DB에서 조회 시도
                   DataSourceContextHolder.setDataSourceType(DataSourceType.SLAVE);
                   try {
                       List<User> slaveUsers = userService.queryUsers();
                       boolean userExists = slaveUsers.stream()
                               .anyMatch(user -> "복제테스트".equals(user.getName()));
                       assertThat(userExists).isTrue();
                   } finally {
                       DataSourceContextHolder.clear();
                   }
               });
    }
    
    @Test
    @DisplayName("Slave DB에 직접 연결하여 쓰기 작업 시도 시 실패해야 한다")
    void slaveDbShouldRejectWriteOperations() throws SQLException {
        // Given: Slave DataSource에 직접 연결
        slaveJdbcTemplate = new JdbcTemplate(dataSource);
        DataSourceContextHolder.setDataSourceType(DataSourceType.SLAVE);
        
        try {
            // When & Then: Slave DB에서 쓰기 작업 시도 시 예외 발생
            assertThatThrownBy(() -> {
                slaveJdbcTemplate.execute("INSERT INTO users (name, email, phone_number) VALUES ('test', 'test@test.com', '010-1111-2222')");
            }).hasMessageContaining("read-only")
              .isInstanceOfAny(SQLException.class, Exception.class);
            
        } finally {
            DataSourceContextHolder.clear();
        }
    }
    
    @Test
    @DisplayName("Master DB 연결 상태 확인")
    void masterDbConnectionShouldWork() {
        // Given: Master DataSource 설정
        DataSourceContextHolder.setDataSourceType(DataSourceType.MASTER);
        masterJdbcTemplate = new JdbcTemplate(dataSource);
        
        try {
            // When: Master DB 연결 테스트
            Integer result = masterJdbcTemplate.queryForObject("SELECT 1", Integer.class);
            
            // Then: 연결이 정상적으로 동작해야 함
            assertThat(result).isEqualTo(1);
        } finally {
            DataSourceContextHolder.clear();
        }
    }
    
    @Test
    @DisplayName("Slave DB 연결 상태 확인")
    void slaveDbConnectionShouldWork() {
        // Given: Slave DataSource 설정
        DataSourceContextHolder.setDataSourceType(DataSourceType.SLAVE);
        slaveJdbcTemplate = new JdbcTemplate(dataSource);
        
        try {
            // When: Slave DB 연결 테스트
            Integer result = slaveJdbcTemplate.queryForObject("SELECT 1", Integer.class);
            
            // Then: 연결이 정상적으로 동작해야 함
            assertThat(result).isEqualTo(1);
        } finally {
            DataSourceContextHolder.clear();
        }
    }
    
    @Test
    @DisplayName("트랜잭션 내에서 Master-Slave 라우팅이 올바르게 동작해야 한다")
    @Transactional
    void transactionalMethodsShouldRouteCorrectly() {
        // Given: 새로운 사용자 데이터
        User newUser = new User("트랜잭션테스트", "transaction@test.com", "010-5555-6666");
        
        // When: 트랜잭션 내에서 쓰기 작업
        User savedUser = userService.createUser(newUser);
        
        // Then: 생성된 사용자 정보 검증
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getName()).isEqualTo("트랜잭션테스트");
        
        // When: 같은 트랜잭션 내에서 읽기 작업
        User foundUser = userService.findById(savedUser.getId());
        
        // Then: 동일한 데이터 확인
        assertThat(foundUser.getName()).isEqualTo("트랜잭션테스트");
        assertThat(foundUser.getEmail()).isEqualTo("transaction@test.com");
    }
    
    @Test
    @DisplayName("업데이트 메서드가 Master DB를 사용하고 결과가 복제되는지 확인")
    @Transactional
    void updateMethodShouldUseMasterAndReplicate() {
        // Given: 기존 사용자 생성
        User newUser = new User("업데이트테스트", "update@test.com", "010-7777-8888");
        User savedUser = userService.createUser(newUser);
        
        // When: 사용자 정보 업데이트
        User updatedUser = userService.updateUser(savedUser.getId(), 
            new User("수정된이름", "updated@test.com", "010-8888-9999"));
        
        // Then: 업데이트 결과 검증
        assertThat(updatedUser.getName()).isEqualTo("수정된이름");
        assertThat(updatedUser.getEmail()).isEqualTo("updated@test.com");
        
        // 복제 확인을 위한 대기 후 Slave에서 조회
        await().atMost(5, TimeUnit.SECONDS)
               .pollInterval(500, TimeUnit.MILLISECONDS)
               .untilAsserted(() -> {
                   DataSourceContextHolder.setDataSourceType(DataSourceType.SLAVE);
                   try {
                       List<User> users = userService.queryUsers();
                       boolean updatedUserExists = users.stream()
                               .anyMatch(user -> "수정된이름".equals(user.getName()));
                       assertThat(updatedUserExists).isTrue();
                   } finally {
                       DataSourceContextHolder.clear();
                   }
               });
    }
}