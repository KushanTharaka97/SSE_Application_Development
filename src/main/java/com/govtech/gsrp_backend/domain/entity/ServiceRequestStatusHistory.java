package com.govtech.gsrp_backend.domain.entity;

import com.govtech.gsrp_backend.domain.enums.RequestStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "service_request_status_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceRequestStatusHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_request_id", nullable = false)
    @NotNull(message = "Service request is mandatory")
    private ServiceRequest serviceRequest;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private RequestStatus previousStatus;

    @NotNull(message = "New status is mandatory")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private RequestStatus newStatus;

    @NotBlank(message = "Changed by is mandatory")
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String changedBy;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime changedAt;
}
