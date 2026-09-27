package com.harsh.shortener.service;

import com.harsh.shortener.dto.LoginRequest;
import com.harsh.shortener.dto.LoginResponse;
import com.harsh.shortener.dto.RegisterRequest;
import com.harsh.shortener.model.AppUser;
import com.harsh.shortener.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttemptService;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    public AuthService(
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            LoginAttemptService loginAttemptService
    ) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.loginAttemptService = loginAttemptService;
    }

    @Transactional
    public AppUser register(RegisterRequest request) {
        String email = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        String password = request.password();

        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Password must be at most 72 bytes when encoded as UTF-8"
            );
        }

        if (appUserRepository.existsByEmail(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An account with this email already exists"
            );
        }

        String passwordHash = passwordEncoder.encode(password);
        AppUser user = new AppUser(email, passwordHash);

        return appUserRepository.save(user);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String email = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (loginAttemptService.isBlocked(email)) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Too many failed login attempts. Try again later."
            );
        }

        AppUser user = appUserRepository.findByEmail(email)
                .orElse(null);

        if (user == null || !passwordEncoder.matches(
                request.password(),
                user.getPasswordHash()
        )) {
            loginAttemptService.recordFailure(email);

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid email or password"
            );
        }

        loginAttemptService.reset(email);

        String token = jwtService.generateToken(user);

        return new LoginResponse(
                token,
                "Bearer",
                expirationMs / 1000
        );
    }
}