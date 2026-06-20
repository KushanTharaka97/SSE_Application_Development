package com.govtech.gsrp_backend.external.repository;

import com.govtech.gsrp_backend.domain.entity.Citizen;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CitizenRepository extends JpaRepository<Citizen, Long> {
    Optional<Citizen> findByNic(String nic);
    Optional<Citizen> findByEmail(String email);
    boolean existsByNic(String nic);
    boolean existsByEmail(String email);
    Page<Citizen> findByNameContainingIgnoreCaseOrNicContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String name, String nic, String email, Pageable pageable);
}
