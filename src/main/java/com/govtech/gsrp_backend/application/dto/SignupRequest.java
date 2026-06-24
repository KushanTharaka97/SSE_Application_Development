package com.govtech.gsrp_backend.application.dto;

import com.govtech.gsrp_backend.domain.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

@Data
public class SignupRequest {
    @Size(max = 100)
    private String name;

    @NotBlank
    @Size(min = 3, max = 50)
    private String username;

    @Size(max = 20)
    private String nic;

    @NotBlank
    @Size(min = 6, max = 100)
    private String password;

    @NotBlank
    @Email
    @Size(max = 100)
    private String email;

    @Size(max = 15)
    private String mobile;

    @Size(max = 255)
    private String address;

    private Set<Role> roles;
}
