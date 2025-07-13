package kr.co.peacefuljw.mybatisjpademo.infrastructure.persistence.mybatis.mapper;

import kr.co.peacefuljw.mybatisjpademo.domain.model.User;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UserMapper {
    List<User> queryUsers();
}
