package kr.co.peacefuljw.mybatisjpademo.application.service;

import kr.co.peacefuljw.mybatisjpademo.common.annotation.ReadOnly;
import kr.co.peacefuljw.mybatisjpademo.common.exception.UserNotFoundException;
import kr.co.peacefuljw.mybatisjpademo.domain.model.User;
import kr.co.peacefuljw.mybatisjpademo.domain.repository.CommandUserRepository;
import kr.co.peacefuljw.mybatisjpademo.domain.repository.QueryUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    private final CommandUserRepository commandUserRepository;
    private final QueryUserRepository queryUserRepository;

    @ReadOnly
    @Transactional(readOnly = true)
    public List<User> queryUsers() {
        return queryUserRepository.queryUsers();
    }

    @ReadOnly
    @Transactional(readOnly = true)
    public User findById(Long id) {
        return commandUserRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + id));
    }

    @Transactional
    public User createUser(User user) {
        return commandUserRepository.save(user);
    }

    @Transactional
    public User updateUser(Long id, User updateUser) {
        User existingUser = findById(id);
        existingUser.updateInfo(updateUser.getName(), updateUser.getEmail(), updateUser.getPhoneNumber());
        return commandUserRepository.save(existingUser);
    }

}
