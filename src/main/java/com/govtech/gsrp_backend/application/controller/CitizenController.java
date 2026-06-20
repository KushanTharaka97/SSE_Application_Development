package com.govtech.gsrp_backend.application.controller;

import com.govtech.gsrp_backend.application.dto.CitizenRequest;
import com.govtech.gsrp_backend.application.dto.CitizenResponse;
import com.govtech.gsrp_backend.domain.service.ICitizenAppService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/citizens")
public class CitizenController {

    @Autowired
    private ICitizenAppService citizenAppService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CitizenResponse> createCitizen(@Valid @RequestBody CitizenRequest citizenRequest) {
        log.info("REST request to create Citizen : {}", citizenRequest.getNic());
        CitizenResponse response = citizenAppService.createCitizen(citizenRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SERVICE_AGENT')")
    public ResponseEntity<CitizenResponse> getCitizenById(@PathVariable Long id) {
        log.info("REST request to get Citizen by ID : {}", id);
        CitizenResponse response = citizenAppService.getCitizenById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<CitizenResponse>> getCitizens(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("REST request to get Citizens list page: {}, size: {}, query: {}", page, size, query);
        Page<CitizenResponse> response = citizenAppService.getCitizens(query, page, size);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CitizenResponse> updateCitizen(
            @PathVariable Long id,
            @Valid @RequestBody CitizenRequest citizenRequest) {
        log.info("REST request to update Citizen ID : {}", id);
        CitizenResponse response = citizenAppService.updateCitizen(id, citizenRequest);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCitizen(@PathVariable Long id) {
        log.info("REST request to deactivate Citizen ID : {}", id);
        citizenAppService.deleteOrDeactivateCitizen(id);
        return ResponseEntity.noContent().build();
    }
}
