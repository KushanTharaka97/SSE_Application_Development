package com.govtech.gsrp_backend.domain.service;

import com.govtech.gsrp_backend.application.dto.CitizenRequest;
import com.govtech.gsrp_backend.application.dto.CitizenResponse;
import org.springframework.data.domain.Page;

public interface ICitizenAppService {
    CitizenResponse createCitizen(CitizenRequest citizenRequest);
    CitizenResponse getCitizenById(Long id);
    Page<CitizenResponse> getCitizens(String query, int page, int size);
    CitizenResponse updateCitizen(Long id, CitizenRequest citizenRequest);
    void deleteOrDeactivateCitizen(Long id);
}
