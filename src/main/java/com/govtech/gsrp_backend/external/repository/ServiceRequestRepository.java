package com.govtech.gsrp_backend.external.repository;

import com.govtech.gsrp_backend.domain.entity.ServiceRequest;
import com.govtech.gsrp_backend.domain.enums.RequestStatus;
import com.govtech.gsrp_backend.domain.enums.ServiceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Long> {
    List<ServiceRequest> findByCitizenReferenceId(Long citizenId);
    Page<ServiceRequest> findByCitizenReferenceId(Long citizenId, Pageable pageable);

    @Query("SELECT s FROM ServiceRequest s WHERE " +
           "(:citizenId IS NULL OR s.citizenReference.id = :citizenId) AND " +
           "(:status IS NULL OR s.status = :status) AND " +
           "(:serviceType IS NULL OR s.serviceType = :serviceType)")
    Page<ServiceRequest> findByFilters(
            @Param("citizenId") Long citizenId,
            @Param("status") RequestStatus status,
            @Param("serviceType") ServiceType serviceType,
            Pageable pageable);
}
