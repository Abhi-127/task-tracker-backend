package com.tasktracker.backend.controller;

import com.tasktracker.backend.dto.UpdateProfileRequest;
import com.tasktracker.backend.dto.UserResponse;
import com.tasktracker.backend.model.User;
import com.tasktracker.backend.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getProfile(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(UserResponse.from(currentUser));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateProfile(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        currentUser.setName(request.name().trim());
        currentUser.setBio(request.bio());
        User updated = userRepository.save(currentUser);
        return ResponseEntity.ok(UserResponse.from(updated));
    }
}
