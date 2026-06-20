package com.govtech.gsrp_backend.application.dto;

import com.govtech.gsrp_backend.domain.enums.CitizenStatus;
import jakarta.validation.constraints.Email;
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
public class CitizenRequest {
    @NotBlank(message = "Name is mandatory")
    @Size(max = 100)
    private String name;

    @NotBlank(message = "NIC is mandatory")
    @Size(max = 20)
    private String nic;

    @NotBlank(message = "Email is mandatory")
    @Email(message = "Invalid email format")
    @Size(max = 100)
    private String email;

    @NotBlank(message = "Mobile number is mandatory")
    @Size(max = 15)
    private String mobile;

    @NotBlank(message = "Address is mandatory")
    @Size(max = 255)
    private String address;

    @NotNull(message = "Status is mandatory")
    private CitizenStatus status;
}
