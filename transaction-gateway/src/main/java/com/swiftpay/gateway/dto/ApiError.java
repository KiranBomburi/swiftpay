package com.swiftpay.gateway.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@Schema(description = "Standard error response envelope")
public class ApiError {

    @Schema(description = "HTTP status code", example = "400")
    private int status;

    @Schema(description = "Short error code", example = "INSUFFICIENT_FUNDS")
    private String error;

    @Schema(description = "Human-readable error message")
    private String message;

    @Schema(description = "Request path that triggered the error", example = "/v1/payments")
    private String path;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    // For validation errors — list of field-level issues
    private List<String> details;
}
