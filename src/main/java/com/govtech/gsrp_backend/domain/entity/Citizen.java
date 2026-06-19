package com.govtech.gsrp_backend.domain.entity;

import com.govtech.gsrp_backend.domain.enums.CitizenStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "citizens")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Citizen {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name is mandatory")
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String name;

    @NotBlank(message = "NIC is mandatory")
    @Size(max = 20)
    @Column(unique = true, nullable = false, length = 20)
    private String nic;

    @NotBlank(message = "Email is mandatory")
    @Email(message = "Invalid email format")
    @Size(max = 100)
    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @NotBlank(message = "Mobile number is mandatory")
    @Size(max = 15)
    @Column(nullable = false, length = 15)
    private String mobile;

    @NotBlank(message = "Address is mandatory")
    @Size(max = 255)
    @Column(nullable = false, length = 255)
    private String address;

    @NotNull(message = "Status is mandatory")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private CitizenStatus status;
}
