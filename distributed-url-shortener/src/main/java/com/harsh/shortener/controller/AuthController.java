package com.harsh.shortener.controller;

import com.harsh.shortener.dto.RegisterRequest;
import com.harsh.shortener.dto.RegisterResponse;
import com.harsh.shortener.model.AppUser;
import com.harsh.shortener.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.harsh.shortener.dto.LoginRequest;
import com.harsh.shortener.dto.LoginResponse;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        AppUser user = authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(RegisterResponse.from(user));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        return ResponseEntity.ok(authService.login(request));
    }
}