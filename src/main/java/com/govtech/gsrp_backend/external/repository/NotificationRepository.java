package com.govtech.gsrp_backend.external.repository;

import com.govtech.gsrp_backend.domain.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByCitizenId(Long citizenId);
}
