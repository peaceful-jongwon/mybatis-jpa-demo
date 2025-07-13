package kr.co.peacefuljw.mybatisjpademo.domain.repository;

import kr.co.peacefuljw.mybatisjpademo.domain.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface CommandUserRepository {
    Optional<User> findById(Long id);
    User save(User user);
    void deleteById(Long id);
    Page<User> findAll(Pageable pageable);
}
