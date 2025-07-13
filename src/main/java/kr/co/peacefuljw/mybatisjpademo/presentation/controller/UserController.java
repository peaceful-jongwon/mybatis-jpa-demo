package kr.co.peacefuljw.mybatisjpademo.presentation.controller;

import jakarta.validation.Valid;
import kr.co.peacefuljw.mybatisjpademo.application.service.UserService;
import kr.co.peacefuljw.mybatisjpademo.domain.model.User;
import kr.co.peacefuljw.mybatisjpademo.presentation.dto.request.CreateUserRequest;
import kr.co.peacefuljw.mybatisjpademo.presentation.dto.response.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> responses = userService.queryUsers().stream()
                .map(UserResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        User user = userService.findById(id);
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        User savedUser = userService.createUser(new User(request.getName(), request.getPhoneNumber()));
        return ResponseEntity.ok(UserResponse.from(savedUser));
    }

}
