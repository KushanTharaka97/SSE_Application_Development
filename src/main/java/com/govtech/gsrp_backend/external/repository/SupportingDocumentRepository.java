package com.govtech.gsrp_backend.external.repository;

import com.govtech.gsrp_backend.domain.entity.SupportingDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupportingDocumentRepository extends JpaRepository<SupportingDocument, Long> {
    List<SupportingDocument> findByRequestReferenceIdOrderByIdAsc(Long serviceRequestId);
}
