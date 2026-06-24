package com.govtech.gsrp_backend.domain.service.impl;

import com.govtech.gsrp_backend.application.dto.CitizenRequest;
import com.govtech.gsrp_backend.application.dto.CitizenResponse;
import com.govtech.gsrp_backend.application.exception.BusinessException;
import com.govtech.gsrp_backend.domain.entity.Citizen;
import com.govtech.gsrp_backend.domain.entity.User;
import com.govtech.gsrp_backend.domain.enums.CitizenStatus;
import com.govtech.gsrp_backend.domain.service.ICitizenAppService;
import com.govtech.gsrp_backend.domain.enums.Role;
import com.govtech.gsrp_backend.external.repository.CitizenRepository;
import com.govtech.gsrp_backend.external.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class CitizenAppServiceImpl implements ICitizenAppService {

    private final CitizenRepository citizenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public CitizenResponse createCitizen(CitizenRequest citizenRequest) {
        log.info("Attempting to create citizen profile with NIC: {}, Email: {}", citizenRequest.getNic(), citizenRequest.getEmail());

        if (citizenRepository.existsByNic(citizenRequest.getNic())) {
            log.warn("Citizen creation failed: Duplicate NIC {}", citizenRequest.getNic());
            throw new BusinessException("Citizen with this NIC already exists");
        }

        if (citizenRepository.existsByEmail(citizenRequest.getEmail())) {
            log.warn("Citizen creation failed: Duplicate Email {}", citizenRequest.getEmail());
            throw new BusinessException("Citizen with this Email already exists");
        }

        // 1. Create and Save Citizen
        Citizen citizen = Citizen.builder()
                .name(citizenRequest.getName())
                .nic(citizenRequest.getNic())
                .email(citizenRequest.getEmail())
                .mobile(citizenRequest.getMobile())
                .address(citizenRequest.getAddress())
                .status(citizenRequest.getStatus())
                .build();

        citizen = citizenRepository.save(citizen);
        log.info("Citizen profile created successfully with ID: {}", citizen.getId());

        // 2. Link an existing user account when public registration already created one.
        // Otherwise auto-create a NIC-based citizen account.
        Optional<User> existingUserOpt = userRepository.findByEmail(citizen.getEmail());
        if (existingUserOpt.isPresent()) {
            User user = existingUserOpt.get();
            Set<Role> roles = new HashSet<>(Optional.ofNullable(user.getRoles()).orElseGet(Collections::emptySet));
            roles.add(Role.CITIZEN);
            user.setRoles(roles);
            userRepository.save(user);
            log.info("Existing user account linked to Citizen profile via email: {}", citizen.getEmail());
        } else {
            if (userRepository.existsByUsername(citizen.getNic())) {
                log.warn("User account creation failed: Username {} is already taken", citizen.getNic());
                throw new BusinessException("User account with this NIC/Username already exists");
            }

            User user = User.builder()
                    .username(citizen.getNic())
                    .email(citizen.getEmail())
                    .password(passwordEncoder.encode(citizen.getNic()))
                    .roles(new HashSet<>(Collections.singletonList(Role.CITIZEN)))
                    .build();

            userRepository.save(user);
            log.info("User account auto-created successfully for Citizen: {}", citizen.getNic());
        }

        return mapToResponse(citizen);
    }

    @Override
    public CitizenResponse getCitizenById(Long id) {
        log.info("Fetching citizen details for ID: {}", id);
        Citizen citizen = citizenRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Citizen not found with ID: {}", id);
                    return new BusinessException("Citizen not found");
                });
        return mapToResponse(citizen);
    }

    @Override
    public Page<CitizenResponse> getCitizens(String query, int page, int size) {
        log.info("Retrieving citizens list - Query: {}, Page: {}, Size: {}", query, page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<Citizen> citizenPage;

        if (query == null || query.trim().isEmpty()) {
            citizenPage = citizenRepository.findAll(pageable);
        } else {
            citizenPage = citizenRepository.findByNameContainingIgnoreCaseOrNicContainingIgnoreCaseOrEmailContainingIgnoreCase(
                    query, query, query, pageable);
        }

        return citizenPage.map(this::mapToResponse);
    }

    @Override
    @Transactional
    public CitizenResponse updateCitizen(Long id, CitizenRequest citizenRequest) {
        log.info("Attempting to update citizen profile ID: {}", id);

        Citizen citizen = citizenRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Citizen update failed: Citizen not found with ID: {}", id);
                    return new BusinessException("Citizen not found");
                });

        // Duplicate checks for NIC and Email if they are being changed
        if (!citizen.getNic().equals(citizenRequest.getNic()) && citizenRepository.existsByNic(citizenRequest.getNic())) {
            log.warn("Citizen update failed: Duplicate NIC {}", citizenRequest.getNic());
            throw new BusinessException("Citizen with this NIC already exists");
        }

        if (!citizen.getEmail().equals(citizenRequest.getEmail()) && citizenRepository.existsByEmail(citizenRequest.getEmail())) {
            log.warn("Citizen update failed: Duplicate Email {}", citizenRequest.getEmail());
            throw new BusinessException("Citizen with this Email already exists");
        }

        String oldNic = citizen.getNic();
        String oldEmail = citizen.getEmail();

        // Update Citizen Entity
        citizen.setName(citizenRequest.getName());
        citizen.setNic(citizenRequest.getNic());
        citizen.setEmail(citizenRequest.getEmail());
        citizen.setMobile(citizenRequest.getMobile());
        citizen.setAddress(citizenRequest.getAddress());
        citizen.setStatus(citizenRequest.getStatus());

        citizen = citizenRepository.save(citizen);
        log.info("Citizen profile updated successfully for ID: {}", id);

        // Sync with corresponding User entity
        Optional<User> userOpt = findLinkedUser(oldNic, oldEmail);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setEmail(citizen.getEmail());
            if (oldNic.equals(user.getUsername())) {
                user.setUsername(citizen.getNic());
                user.setPassword(passwordEncoder.encode(citizen.getNic()));
            }
            userRepository.save(user);
            log.info("Sync completed: User credentials updated for citizen ID: {}", citizen.getId());
        }

        return mapToResponse(citizen);
    }

    @Override
    @Transactional
    public void deleteOrDeactivateCitizen(Long id) {
        log.info("Attempting to deactivate citizen profile ID: {}", id);

        Citizen citizen = citizenRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Citizen deactivation failed: Citizen not found with ID: {}", id);
                    return new BusinessException("Citizen not found");
                });

        // Set status to INACTIVE for soft deletion / deactivation
        citizen.setStatus(CitizenStatus.INACTIVE);
        citizenRepository.save(citizen);
        log.info("Citizen profile status set to INACTIVE for ID: {}", id);

        // Optionally disable matching user credentials by changing roles or deleting user?
        // Since we don't have active/enabled flag in User, we can delete the User record
        // to prevent inactive citizens from logging in.
        Optional<User> userOpt = findLinkedUser(citizen.getNic(), citizen.getEmail());
        userOpt.ifPresent(user -> {
            userRepository.delete(user);
            log.info("Corresponding User account deleted for deactivated Citizen NIC: {}", citizen.getNic());
        });
    }

    private Optional<User> findLinkedUser(String nic, String email) {
        return userRepository.findByUsername(nic)
                .or(() -> userRepository.findByEmail(email));
    }

    private CitizenResponse mapToResponse(Citizen citizen) {
        return CitizenResponse.builder()
                .id(citizen.getId())
                .name(citizen.getName())
                .nic(citizen.getNic())
                .email(citizen.getEmail())
                .mobile(citizen.getMobile())
                .address(citizen.getAddress())
                .status(citizen.getStatus())
                .build();
    }
}
