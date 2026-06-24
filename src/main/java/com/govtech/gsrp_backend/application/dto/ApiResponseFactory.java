package com.govtech.gsrp_backend.application.dto;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.OffsetDateTime;

public final class ApiResponseFactory {

    private ApiResponseFactory() {
    }

    public static <T> ResponseEntity<ApiSuccessResponse<T>> ok(String message, T data) {
        return build(HttpStatus.OK, message, data);
    }

    public static <T> ResponseEntity<ApiSuccessResponse<T>> created(String message, T data) {
        return build(HttpStatus.CREATED, message, data);
    }

    public static ResponseEntity<ApiSuccessResponse<Void>> ok(String message) {
        return build(HttpStatus.OK, message, null);
    }

    private static <T> ResponseEntity<ApiSuccessResponse<T>> build(HttpStatus status, String message, T data) {
        ApiSuccessResponse<T> response = ApiSuccessResponse.<T>builder()
                .timestamp(OffsetDateTime.now().toString())
                .status(status.value())
                .message(message)
                .data(data)
                .build();
        return ResponseEntity.status(status).body(response);
    }
}
