package com.govtech.gsrp_backend.external.repository;

import com.govtech.gsrp_backend.domain.entity.ServiceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Long> {
    List<ServiceRequest> findByCitizenReferenceId(Long citizenId);
}
