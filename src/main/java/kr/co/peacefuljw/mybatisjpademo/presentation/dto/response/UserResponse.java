package kr.co.peacefuljw.mybatisjpademo.presentation.dto.response;

import kr.co.peacefuljw.mybatisjpademo.domain.model.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private String phoneNumber;

    public static UserResponse from(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .phoneNumber(user.getPhoneNumber())
                .build();
    }
}

