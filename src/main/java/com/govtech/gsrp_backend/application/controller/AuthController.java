package com.govtech.gsrp_backend.application.controller;

import com.govtech.gsrp_backend.application.dto.*;
import com.govtech.gsrp_backend.application.service.IAuthAppService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private IAuthAppService authService;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("Received login request for username: {}", loginRequest.getUsername());
        JwtResponse response = authService.authenticateUser(loginRequest);
        log.info("Login request processed successfully for username: {}", loginRequest.getUsername());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        log.info("Received registration request for username: {}, email: {}", signUpRequest.getUsername(), signUpRequest.getEmail());
        MessageResponse response = authService.registerUser(signUpRequest);
        log.info("Registration request processed successfully for username: {}", signUpRequest.getUsername());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/admin/register")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> registerUserByAdmin(@Valid @RequestBody SignupRequest signUpRequest) {
        log.info("Admin registration request received for username: {}, email: {}", signUpRequest.getUsername(), signUpRequest.getEmail());
        MessageResponse response = authService.registerUserByAdmin(signUpRequest);
        log.info("Admin registration request processed successfully for username: {}", signUpRequest.getUsername());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        log.info("Received request to fetch current authenticated user profile.");
        JwtResponse response = authService.getCurrentUser();
        log.info("Successfully retrieved profile for username: {}", response.getUsername());
        return ResponseEntity.ok(response);
    }
}
