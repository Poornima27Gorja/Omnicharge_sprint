package com.omnicharge.operator_service.controller;

import com.omnicharge.operator_service.dto.RechargePlanDto;
import com.omnicharge.operator_service.dto.RechargePlanRequest;
import com.omnicharge.operator_service.dto.StatusUpdateRequest;
import com.omnicharge.operator_service.service.RechargePlanService;
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
@RequestMapping("/api/plans")
@Tag(name = "Recharge Plans", description = "Recharge plan management per operator")
@SecurityRequirement(name = "bearerAuth")
public class RechargePlanController {

    @Autowired
    private RechargePlanService rechargePlanService;

    // ─── Admin-only write operations ─────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Add recharge plan [ADMIN ONLY]",
        description = "Creates a new recharge plan under an existing operator. Requires ROLE_ADMIN token."
    )
    public ResponseEntity<RechargePlanDto> addPlan(@Valid @RequestBody RechargePlanRequest request) {
        RechargePlanDto dto = rechargePlanService.addPlan(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Update recharge plan [ADMIN ONLY]",
        description = "Fully updates an existing plan (price, validity, data, etc.). Requires ROLE_ADMIN token."
    )
    public ResponseEntity<RechargePlanDto> updatePlan(
            @PathVariable Long id,
            @Valid @RequestBody RechargePlanRequest request) {
        RechargePlanDto dto = rechargePlanService.updatePlan(id, request);
        return ResponseEntity.ok(dto);
    }

    /**
     * PATCH — status-only update.
     *
     * Example:
     *   PATCH /api/plans/5/status
     *   Body: { "status": "INACTIVE" }
     *
     * Real-world use: admin deactivates a plan mid-month (e.g. regulatory requirement)
     * without altering any other plan data. Reactivate with { "status": "ACTIVE" }.
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Update plan status [ADMIN ONLY]",
        description = "Toggles plan status between ACTIVE and INACTIVE without touching other fields. " +
                      "Body: { \"status\": \"INACTIVE\" }"
    )
    public ResponseEntity<RechargePlanDto> updatePlanStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest request) {
        RechargePlanDto dto = rechargePlanService.updatePlanStatus(id, request.getStatus());
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Delete recharge plan [ADMIN ONLY]",
        description = "Permanently deletes a plan. Prefer INACTIVE status over deletion. Requires ROLE_ADMIN token."
    )
    public ResponseEntity<String> deletePlan(@PathVariable Long id) {
        rechargePlanService.deletePlan(id);
        return ResponseEntity.ok("Plan deleted successfully");
    }

    // ─── Read-only operations (any authenticated user) ────────────────────────────

    @GetMapping("/{id}")
    @Operation(summary = "Get plan by ID")
    public ResponseEntity<RechargePlanDto> getPlanById(@PathVariable Long id) {
        RechargePlanDto dto = rechargePlanService.getPlanById(id);
        return ResponseEntity.ok(dto);
    }

    @GetMapping
    @Operation(summary = "Get all plans")
    public ResponseEntity<List<RechargePlanDto>> getAllPlans() {
        List<RechargePlanDto> plans = rechargePlanService.getAllPlans();
        return ResponseEntity.ok(plans);
    }

    @GetMapping("/operator/{operatorId}")
    @Operation(summary = "Get all plans by operator (active + inactive)")
    public ResponseEntity<List<RechargePlanDto>> getPlansByOperator(@PathVariable Long operatorId) {
        List<RechargePlanDto> plans = rechargePlanService.getPlansByOperator(operatorId);
        return ResponseEntity.ok(plans);
    }

    @GetMapping("/operator/{operatorId}/active")
    @Operation(
        summary = "Get active plans by operator",
        description = "Returns only ACTIVE plans for an operator. Use this on the recharge screen."
    )
    public ResponseEntity<List<RechargePlanDto>> getActivePlansByOperator(@PathVariable Long operatorId) {
        List<RechargePlanDto> plans = rechargePlanService.getActivePlansByOperator(operatorId);
        return ResponseEntity.ok(plans);
    }

    @GetMapping("/category/{category}")
    @Operation(
        summary = "Get plans by category",
        description = "Filter plans by category: TOPUP, DATA, COMBO, SMS, ROAMING, INTERNATIONAL, SPECIAL."
    )
    public ResponseEntity<List<RechargePlanDto>> getPlansByCategory(@PathVariable String category) {
        List<RechargePlanDto> plans = rechargePlanService.getPlansByCategory(category);
        return ResponseEntity.ok(plans);
    }
}