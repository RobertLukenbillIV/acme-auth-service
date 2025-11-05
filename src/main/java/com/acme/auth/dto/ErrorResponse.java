package com.acme.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard error response matching acme-contracts ErrorResponse schema")
public class ErrorResponse {

    @Schema(description = "Error code", example = "VALIDATION_ERROR")
    private String code;

    @Schema(description = "Error message", example = "Validation failed")
    private String message;

    @Schema(description = "Validation error details")
    private List<ValidationErrorDetail> details;

    @Schema(description = "Timestamp when error occurred")
    private Instant timestamp;

    @Schema(description = "Request path", example = "/api/auth/signup")
    private String path;

    @Schema(description = "Request ID for tracking")
    private UUID requestId;

    public ErrorResponse(String code, String message) {
        this.code = code;
        this.message = message;
        this.timestamp = Instant.now();
    }

    public ErrorResponse(String code, String message, String path) {
        this.code = code;
        this.message = message;
        this.timestamp = Instant.now();
        this.path = path;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Validation error detail")
    public static class ValidationErrorDetail {
        @Schema(description = "Field name", example = "email")
        private String field;

        @Schema(description = "Error message", example = "Email is required")
        private String message;

        @Schema(description = "Error code", example = "NotBlank")
        private String code;
    }
}
