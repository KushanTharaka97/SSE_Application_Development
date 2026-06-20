package com.govtech.gsrp_backend.domain.entity;

import com.govtech.gsrp_backend.domain.enums.DocumentType;
import com.govtech.gsrp_backend.domain.enums.VerificationStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "supporting_documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportingDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_request_id", nullable = false)
    @NotNull(message = "Request reference is mandatory")
    private ServiceRequest requestReference;


    @NotNull(message = "Document type is mandatory")
    @Enumerated(EnumType.STRING)
    @Column( nullable = false, length = 50)
    private DocumentType type;

    @NotBlank(message = "Document name is mandatory")
    @Size(max = 255)
    @Column(nullable = false, length = 255)
    private String name;

    @NotBlank(message = "Document reference is mandatory")
    @Size(max = 255)
    @Column(nullable = false, length = 255)
    private String documentReference;

    @NotNull(message = "Verification status is mandatory")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private VerificationStatus verificationStatus;
}
