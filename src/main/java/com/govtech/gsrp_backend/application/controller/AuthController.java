package com.govtech.gsrp_backend.application.controller;

import com.govtech.gsrp_backend.application.dto.ApiResponseFactory;
import com.govtech.gsrp_backend.application.dto.ApiSuccessResponse;
import com.govtech.gsrp_backend.application.dto.JwtResponse;
import com.govtech.gsrp_backend.application.dto.LoginRequest;
import com.govtech.gsrp_backend.application.dto.MessageResponse;
import com.govtech.gsrp_backend.application.dto.SignupRequest;
import com.govtech.gsrp_backend.application.service.IAuthAppService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final IAuthAppService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiSuccessResponse<JwtResponse>> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("Received login request for username: {}", loginRequest.getUsername());
        JwtResponse response = authService.authenticateUser(loginRequest);
        log.info("Login request processed successfully for username: {}", loginRequest.getUsername());
        return ApiResponseFactory.ok("User authenticated successfully.", response);
    }

    @PostMapping("/register")
    public ResponseEntity<ApiSuccessResponse<MessageResponse>> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        log.info("Received registration request for username: {}, email: {}", signUpRequest.getUsername(), signUpRequest.getEmail());
        MessageResponse response = authService.registerUser(signUpRequest);
        log.info("Registration request processed successfully for username: {}", signUpRequest.getUsername());
        return ApiResponseFactory.created("User registered successfully.", response);
    }

    @PostMapping("/admin/register")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<MessageResponse>> registerUserByAdmin(@Valid @RequestBody SignupRequest signUpRequest) {
        log.info("Admin registration request received for username: {}, email: {}", signUpRequest.getUsername(), signUpRequest.getEmail());
        MessageResponse response = authService.registerUserByAdmin(signUpRequest);
        log.info("Admin registration request processed successfully for username: {}", signUpRequest.getUsername());
        return ApiResponseFactory.created("User registered successfully by admin.", response);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiSuccessResponse<JwtResponse>> getCurrentUser() {
        log.info("Received request to fetch current authenticated user profile.");
        JwtResponse response = authService.getCurrentUser();
        log.info("Successfully retrieved profile for username: {}", response.getUsername());
        return ApiResponseFactory.ok("Authenticated user profile retrieved successfully.", response);
    }
}
