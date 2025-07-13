package kr.co.peacefuljw.mybatisjpademo.domain.repository;

import kr.co.peacefuljw.mybatisjpademo.domain.model.User;

import java.util.List;

public interface QueryUserRepository {

    List<User> queryUsers();
}
