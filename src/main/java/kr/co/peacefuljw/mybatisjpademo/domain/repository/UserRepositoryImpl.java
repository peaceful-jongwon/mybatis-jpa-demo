package kr.co.peacefuljw.mybatisjpademo.domain.repository;

import kr.co.peacefuljw.mybatisjpademo.domain.model.User;
import kr.co.peacefuljw.mybatisjpademo.infrastructure.persistence.entity.UserEntity;
import kr.co.peacefuljw.mybatisjpademo.infrastructure.persistence.mybatis.mapper.UserMapper;
import kr.co.peacefuljw.mybatisjpademo.infrastructure.persistence.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserRepositoryImpl implements CommandUserRepository, QueryUserRepository {

    private final UserJpaRepository repository;
    private final UserMapper mapper;

    @Override
    public Optional<User> findById(Long id) {
        return repository.findById(id)
                .map(userEntity -> User.builder()
                        .id(userEntity.getId())
                        .name(userEntity.getName())
                        .createdAt(userEntity.getCreatedAt())
                        .updatedAt(userEntity.getUpdatedAt())
                        .build());
    }

    @Override
    public User save(User user) {
        return toDomain(repository.save(
            UserEntity.builder()
                    .name(user.getName())
                    .phoneNumber(user.getPhoneNumber())
                .build()));
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    @Override
    public Page<User> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(this::toDomain);
    }

    private User toDomain(UserEntity entity) {
        return User.builder()
            .id(entity.getId())
            .name(entity.getName())
            .phoneNumber(entity.getPhoneNumber())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }

    @Override
    public List<User> queryUsers() {
        return mapper.queryUsers();
    }
}
