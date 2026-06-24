package com.govtech.gsrp_backend.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiSuccessResponse<T> {
    private String timestamp;
    private int status;
    private String message;
    private T data;
}
