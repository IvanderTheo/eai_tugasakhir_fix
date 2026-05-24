package com.example.adminservice.service;

import com.example.adminservice.config.KafkaConfig;
import com.example.adminservice.entity.User;
import com.example.adminservice.repository.UserRepository;
import com.example.adminservice.security.JwtRoleUtils;
import com.example.adminservice.security.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Slf4j
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public User registerUser(String username, String password, String nama, String email, String role) {
        if (userRepository.existsByUsername(username)) {
            log.warn("Username {} already exists", username);
            throw new RuntimeException("Username already exists");
        }

        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .nama(nama)
                .email(email)
                .role(role)
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);
        
        // Publish event to Kafka
        kafkaTemplate.send(KafkaConfig.USER_REGISTERED_TOPIC, savedUser.getId().toString(), savedUser);
        log.info("User registered with ID: {} and published to Kafka", savedUser.getId());

        return savedUser;
    }

    @Transactional(readOnly = true)
    public User getUserByUsername(String username) {
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isEmpty()) {
            log.warn("User with username {} not found", username);
            throw new RuntimeException("User not found");
        }
        return user.get();
    }

    @Transactional(readOnly = true)
    public String authenticateUser(String username, String password) {
        User user = getUserByUsername(username);
        
        if (!passwordEncoder.matches(password, user.getPassword())) {
            log.warn("Invalid password for user: {}", username);
            throw new RuntimeException("Invalid credentials");
        }

        if (!user.getIsActive()) {
            log.warn("User account is inactive: {}", username);
            throw new RuntimeException("User account is inactive");
        }

        String token = jwtUtil.generateTokenFromUsernameAndRoles(
                username,
                java.util.List.of(JwtRoleUtils.toRoleAuthority(user.getRole())));
        log.info("User {} authenticated successfully", username);
        return token;
    }

    @Transactional(readOnly = true)
    public String refreshToken(String username) {
        User user = getUserByUsername(username);
        if (!user.getIsActive()) {
            throw new RuntimeException("User account is inactive");
        }
        return jwtUtil.generateTokenFromUsernameAndRoles(
                username,
                java.util.List.of(JwtRoleUtils.toRoleAuthority(user.getRole())));
    }
}

