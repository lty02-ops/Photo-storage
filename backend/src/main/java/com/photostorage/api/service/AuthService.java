package com.photostorage.api.service;

import com.photostorage.api.common.ApiException;
import com.photostorage.api.dto.AuthResponse;
import com.photostorage.api.dto.UserResponse;
import com.photostorage.api.model.AppUser;
import com.photostorage.api.model.UserRole;
import com.photostorage.api.repository.UserRepository;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Map<String, Long> sessions = new ConcurrentHashMap<>();

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AuthResponse signup(String email, String password) {
        String normalizedEmail = email.trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ApiException(HttpStatus.CONFLICT, "Email is already registered.");
        }

        UserRole role = userRepository.count() == 0 ? UserRole.ADMIN : UserRole.USER;
        AppUser user = new AppUser(normalizedEmail, passwordEncoder.encode(password), role);
        AppUser savedUser = userRepository.save(user);
        return createSession(savedUser);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(String email, String password) {
        AppUser user = userRepository.findByEmail(email.trim().toLowerCase())
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password."));

        if (user.getPasswordHash() == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Use social login for this account.");
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password.");
        }

        return createSession(user);
    }

    @Transactional
    public AuthResponse oauthLogin(String provider, String providerUserId, String email) {
        String normalizedProvider = provider.trim().toLowerCase();
        String normalizedEmail = email.trim().toLowerCase();

        AppUser user = userRepository.findByProviderAndProviderUserId(normalizedProvider, providerUserId)
            .or(() -> userRepository.findByEmail(normalizedEmail))
            .orElseGet(() -> {
                UserRole role = userRepository.count() == 0 ? UserRole.ADMIN : UserRole.USER;
                return userRepository.save(new AppUser(normalizedEmail, normalizedProvider, providerUserId, role));
            });

        return createSession(user);
    }

    public Optional<AppUser> authenticate(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        Long userId = sessions.get(token);
        if (userId == null) {
            return Optional.empty();
        }
        return userRepository.findById(userId);
    }

    public AuthResponse createSession(AppUser user) {
        String token = UUID.randomUUID().toString();
        sessions.put(token, user.getId());
        return new AuthResponse(token, UserResponse.from(user));
    }
}
