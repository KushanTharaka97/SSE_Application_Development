package com.govtech.gsrp_backend.application.service.impl;

import com.govtech.gsrp_backend.application.dto.JwtResponse;
import com.govtech.gsrp_backend.application.dto.LoginRequest;
import com.govtech.gsrp_backend.application.dto.MessageResponse;
import com.govtech.gsrp_backend.application.dto.SignupRequest;
import com.govtech.gsrp_backend.application.exception.BusinessException;
import com.govtech.gsrp_backend.application.security.JwtUtils;
import com.govtech.gsrp_backend.application.service.IAuthAppService;
import com.govtech.gsrp_backend.domain.entity.User;
import com.govtech.gsrp_backend.domain.util.Role;
import com.govtech.gsrp_backend.external.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AuthAppService implements IAuthAppService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private JwtUtils jwtUtils;

    @Override
    public JwtResponse authenticateUser(LoginRequest loginRequest) {
        log.info("Attempting to authenticate user: {}", loginRequest.getUsername());
        
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> {
                    log.error("User not found after successful authentication: {}", userDetails.getUsername());
                    return new BusinessException("User not found");
                });

        log.info("User {} authenticated successfully with roles: {}", user.getUsername(), roles);

        return new JwtResponse(jwt,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                roles);
    }

    @Override
    @Transactional
    public MessageResponse registerUser(SignupRequest signUpRequest) {
        log.info("Attempting to register new user: {}", signUpRequest.getUsername());

        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            log.warn("Registration failed: Username {} is already taken", signUpRequest.getUsername());
            throw new BusinessException("Error: Username is already taken!");
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            log.warn("Registration failed: Email {} is already in use", signUpRequest.getEmail());
            throw new BusinessException("Error: Email is already in use!");
        }

        // Create new user's account
        User user = User.builder()
                .username(signUpRequest.getUsername())
                .email(signUpRequest.getEmail())
                .password(encoder.encode(signUpRequest.getPassword()))
                .build();

        Set<Role> roles = new HashSet<>();
        if (signUpRequest.getRoles() == null || signUpRequest.getRoles().isEmpty()) {
            roles.add(Role.CITIZEN);
        } else {
            roles.addAll(signUpRequest.getRoles());
        }

        user.setRoles(roles);
        userRepository.save(user);

        log.info("User {} registered successfully with roles: {}", user.getUsername(), roles);
        return new MessageResponse("User registered successfully!");
    }

    @Override
    public JwtResponse getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
            log.warn("Attempted to access /me without authentication");
            throw new BusinessException("Error: Unauthorized");
        }

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        log.debug("Fetching current user info for: {}", userDetails.getUsername());

        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new BusinessException("User not found"));
        
        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        return new JwtResponse(null,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                roles);
    }
}
