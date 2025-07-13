package kr.co.peacefuljw.mybatisjpademo.infrastructure.persistence.repository;

import kr.co.peacefuljw.mybatisjpademo.infrastructure.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {

}
