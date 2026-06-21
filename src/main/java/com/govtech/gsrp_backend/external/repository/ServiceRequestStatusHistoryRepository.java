package com.govtech.gsrp_backend.external.repository;

import com.govtech.gsrp_backend.domain.entity.ServiceRequestStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceRequestStatusHistoryRepository extends JpaRepository<ServiceRequestStatusHistory, Long> {
    List<ServiceRequestStatusHistory> findByServiceRequestIdOrderByChangedAtAscIdAsc(Long serviceRequestId);
}
