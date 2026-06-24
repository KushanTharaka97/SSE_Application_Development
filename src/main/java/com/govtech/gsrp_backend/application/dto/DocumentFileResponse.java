package com.govtech.gsrp_backend.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentFileResponse {
    private Resource resource;
    private MediaType mediaType;
    private String fileName;
}
