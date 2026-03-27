package com.omnicharge.operator_service.controller;

import com.omnicharge.operator_service.dto.OperatorDto;
import com.omnicharge.operator_service.dto.OperatorRequest;
import com.omnicharge.operator_service.dto.StatusUpdateRequest;
import com.omnicharge.operator_service.service.OperatorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/operators")
@Tag(name = "Operators", description = "Telecom operator management")
@SecurityRequirement(name = "bearerAuth")
public class OperatorController {

    @Autowired
    private OperatorService operatorService;

    // ─── Admin-only write operations ─────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Add operator [ADMIN ONLY]",
        description = "Creates a new telecom operator. Requires ROLE_ADMIN token."
    )
    public ResponseEntity<OperatorDto> addOperator(@Valid @RequestBody OperatorRequest request) {
        OperatorDto dto = operatorService.addOperator(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Update operator [ADMIN ONLY]",
        description = "Fully updates an existing operator. Requires ROLE_ADMIN token."
    )
    public ResponseEntity<OperatorDto> updateOperator(
            @PathVariable Long id,
            @Valid @RequestBody OperatorRequest request) {
        OperatorDto dto = operatorService.updateOperator(id, request);
        return ResponseEntity.ok(dto);
    }

    /**
     * PATCH — status-only update (no need to send full operator payload).
     *
     * Example:
     *   PATCH /api/operators/3/status
     *   Body: { "status": "INACTIVE" }
     *
     * This is the recommended way to toggle operator availability.
     * Setting an operator INACTIVE will hide its plans from the recharge flow.
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Update operator status [ADMIN ONLY]",
        description = "Toggles operator status between ACTIVE and INACTIVE. " +
                      "Cheaper than a full PUT — only the status column is updated. " +
                      "Body: { \"status\": \"INACTIVE\" }"
    )
    public ResponseEntity<OperatorDto> updateOperatorStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest request) {
        OperatorDto dto = operatorService.updateOperatorStatus(id, request.getStatus());
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Delete operator [ADMIN ONLY]",
        description = "Permanently deletes an operator and all its plans. Requires ROLE_ADMIN token."
    )
    public ResponseEntity<String> deleteOperator(@PathVariable Long id) {
        operatorService.deleteOperator(id);
        return ResponseEntity.ok("Operator deleted successfully");
    }

    // ─── Read-only operations (any authenticated user) ────────────────────────────

    @GetMapping("/{id}")
    @Operation(summary = "Get operator by ID")
    public ResponseEntity<OperatorDto> getOperatorById(@PathVariable Long id) {
        OperatorDto dto = operatorService.getOperatorById(id);
        return ResponseEntity.ok(dto);
    }

    @GetMapping
    @Operation(summary = "Get all operators")
    public ResponseEntity<List<OperatorDto>> getAllOperators() {
        List<OperatorDto> operators = operatorService.getAllOperators();
        return ResponseEntity.ok(operators);
    }

    @GetMapping("/status/{status}")
    @Operation(
        summary = "Get operators by status",
        description = "Filter operators by ACTIVE or INACTIVE status."
    )
    public ResponseEntity<List<OperatorDto>> getOperatorsByStatus(@PathVariable String status) {
        List<OperatorDto> operators = operatorService.getOperatorsByStatus(status);
        return ResponseEntity.ok(operators);
    }

    @GetMapping("/type/{type}")
    @Operation(
        summary = "Get operators by type",
        description = "Filter operators by type: MOBILE, BROADBAND, DTH, LANDLINE."
    )
    public ResponseEntity<List<OperatorDto>> getOperatorsByType(@PathVariable String type) {
        List<OperatorDto> operators = operatorService.getOperatorsByType(type);
        return ResponseEntity.ok(operators);
    }
}