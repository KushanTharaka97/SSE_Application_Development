package com.govtech.gsrp_backend.external.repository;

import com.govtech.gsrp_backend.domain.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByCitizenIdOrderByCreatedDateDesc(Long citizenId);
    Optional<Notification> findByIdAndCitizenId(Long id, Long citizenId);
}
