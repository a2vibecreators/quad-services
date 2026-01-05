package com.quad.services.service;

import com.quad.services.entity.User;
import com.quad.services.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    /**
     * Find user by email address
     * @param email User's email address
     * @return Optional containing user if found
     */
    public Optional<User> findByEmail(String email) {
        log.info("Looking up user by email: {}", email);
        return userRepository.findByEmail(email);
    }

    /**
     * Check if user exists by email
     * @param email User's email address
     * @return true if user exists, false otherwise
     */
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
}
