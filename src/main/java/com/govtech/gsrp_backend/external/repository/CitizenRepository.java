package com.govtech.gsrp_backend.external.repository;

import com.govtech.gsrp_backend.domain.entity.Citizen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CitizenRepository extends JpaRepository<Citizen, Long> {
    Optional<Citizen> findByNic(String nic);
    Optional<Citizen> findByEmail(String email);
}
