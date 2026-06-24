package com.govtech.gsrp_backend.application.service.impl;

import com.govtech.gsrp_backend.application.dto.JwtResponse;
import com.govtech.gsrp_backend.application.dto.LoginRequest;
import com.govtech.gsrp_backend.application.dto.MessageResponse;
import com.govtech.gsrp_backend.application.dto.SignupRequest;
import com.govtech.gsrp_backend.application.exception.BusinessException;
import com.govtech.gsrp_backend.application.security.JwtUtils;
import com.govtech.gsrp_backend.application.service.IAuthAppService;
import com.govtech.gsrp_backend.domain.entity.Citizen;
import com.govtech.gsrp_backend.domain.entity.User;
import com.govtech.gsrp_backend.domain.enums.CitizenStatus;
import com.govtech.gsrp_backend.domain.enums.Role;
import com.govtech.gsrp_backend.external.repository.CitizenRepository;
import com.govtech.gsrp_backend.external.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthAppService implements IAuthAppService {

    private final AuthenticationManager authenticationManager;
    private final CitizenRepository citizenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final JwtUtils jwtUtils;

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
        String citizenNic = normalizePublicNic(signUpRequest);
        log.info("Attempting to register new citizen account with NIC: {}", citizenNic);

        validatePublicCitizenRegistration(signUpRequest, citizenNic);

        if (citizenRepository.existsByNic(citizenNic)) {
            log.warn("Registration failed: Citizen NIC {} is already in use", citizenNic);
            throw new BusinessException("Error: Citizen NIC is already in use!");
        }

        if (citizenRepository.existsByEmail(signUpRequest.getEmail())) {
            log.warn("Registration failed: Citizen email {} is already in use", signUpRequest.getEmail());
            throw new BusinessException("Error: Citizen email is already in use!");
        }

        if (userRepository.existsByUsername(citizenNic)) {
            log.warn("Registration failed: Username {} is already taken", citizenNic);
            throw new BusinessException("Error: Username is already taken!");
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            log.warn("Registration failed: Email {} is already in use", signUpRequest.getEmail());
            throw new BusinessException("Error: Email is already in use!");
        }

        Citizen citizen = Citizen.builder()
                .name(signUpRequest.getName().trim())
                .nic(citizenNic)
                .email(signUpRequest.getEmail().trim())
                .mobile(signUpRequest.getMobile().trim())
                .address(signUpRequest.getAddress().trim())
                .status(CitizenStatus.ACTIVE)
                .build();
        citizenRepository.save(citizen);

        User user = User.builder()
                .username(citizenNic)
                .email(signUpRequest.getEmail().trim())
                .password(encoder.encode(signUpRequest.getPassword()))
                .build();

        Set<Role> roles = new HashSet<>();
        roles.add(Role.CITIZEN); // Public registration strictly defaults to CITIZEN role

        user.setRoles(roles);
        userRepository.save(user);

        log.info("Citizen account {} registered successfully with roles: {}", user.getUsername(), roles);
        return new MessageResponse("Citizen account registered successfully. Sign in with your NIC and password.");
    }

    @Override
    @Transactional
    public MessageResponse registerUserByAdmin(SignupRequest signUpRequest) {
        log.info("Admin attempting to register user: {}", signUpRequest.getUsername());

        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            log.warn("Registration failed: Username {} is already taken", signUpRequest.getUsername());
            throw new BusinessException("Error: Username is already taken!");
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            log.warn("Registration failed: Email {} is already in use", signUpRequest.getEmail());
            throw new BusinessException("Error: Email is already in use!");
        }

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

        log.info("User {} registered successfully by admin with roles: {}", user.getUsername(), roles);
        return new MessageResponse("User registered successfully by admin!");
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

    private void validatePublicCitizenRegistration(SignupRequest signUpRequest, String citizenNic) {
        if (!StringUtils.hasText(signUpRequest.getName())) {
            throw new BusinessException("Error: Name is required for citizen registration.");
        }
        if (!StringUtils.hasText(citizenNic)) {
            throw new BusinessException("Error: NIC is required for citizen registration.");
        }
        if (!StringUtils.hasText(signUpRequest.getMobile())) {
            throw new BusinessException("Error: Mobile number is required for citizen registration.");
        }
        if (!StringUtils.hasText(signUpRequest.getAddress())) {
            throw new BusinessException("Error: Address is required for citizen registration.");
        }
    }

    private String normalizePublicNic(SignupRequest signUpRequest) {
        if (StringUtils.hasText(signUpRequest.getNic())) {
            return signUpRequest.getNic().trim();
        }
        if (StringUtils.hasText(signUpRequest.getUsername())) {
            return signUpRequest.getUsername().trim();
        }
        return null;
    }
}
