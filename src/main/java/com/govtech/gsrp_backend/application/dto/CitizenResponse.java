package com.govtech.gsrp_backend.application.dto;

import com.govtech.gsrp_backend.domain.enums.CitizenStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CitizenResponse {
    private Long id;
    private String name;
    private String nic;
    private String email;
    private String mobile;
    private String address;
    private CitizenStatus status;
}
