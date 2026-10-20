package com.kodegladiators.kaasutracker.controller;

import com.kodegladiators.kaasutracker.model.User;
import com.kodegladiators.kaasutracker.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<User> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject(); // Gets the Supabase UUID string
        Optional<User> userOptional = userRepository.findById(userId);

        return userOptional.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/me")
    public ResponseEntity<User> updateCurrentUser(@AuthenticationPrincipal Jwt jwt, @RequestBody User updateRequest) {
        String userId = jwt.getSubject();
        Optional<User> userOptional = userRepository.findById(userId);

        if (userOptional.isPresent()) {
            User existingUser = userOptional.get();
            // Update the configurable fields
            existingUser.setBaseSalary(updateRequest.getBaseSalary());
            existingUser.setThresholdPercentage(updateRequest.getThresholdPercentage());

            if (updateRequest.getLanguagePref() != null) {
                existingUser.setLanguagePref(updateRequest.getLanguagePref());
            }

            userRepository.save(existingUser);
            return ResponseEntity.ok(existingUser);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // Safety fallback just in case Flutter sends a POST instead of PUT
    @PostMapping("/me")
    public ResponseEntity<User> updateCurrentUserFallback(@AuthenticationPrincipal Jwt jwt, @RequestBody User updateRequest) {
        return updateCurrentUser(jwt, updateRequest);
    }
}