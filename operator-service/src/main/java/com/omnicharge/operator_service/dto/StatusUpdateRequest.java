package com.omnicharge.operator_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Used by PATCH /api/operators/{id}/status and PATCH /api/plans/{id}/status.
 *
 * Why a dedicated DTO instead of reusing the full request?
 * Real-world reason: status toggles (ACTIVE / INACTIVE) are the most common
 * admin action — done constantly without touching any other field.
 * Sending a full PUT body just to flip a status is wasteful and error-prone.
 */
public class StatusUpdateRequest {

    @NotBlank(message = "Status is required")
    @Pattern(
        regexp = "^(ACTIVE|INACTIVE)$",
        message = "Status must be either ACTIVE or INACTIVE"
    )
    private String status;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}