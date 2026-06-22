package com.govtech.gsrp_backend.application.dto;

import com.govtech.gsrp_backend.domain.enums.ServiceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceRequestSubmitDTO {
    @NotNull(message = "Service type is mandatory")
    private ServiceType serviceType;

    @NotBlank(message = "Description is mandatory")
    @Size(max = 1000)
    private String description;

    // Optional for Admin to submit on behalf of a citizen
    private Long citizenId;
}
