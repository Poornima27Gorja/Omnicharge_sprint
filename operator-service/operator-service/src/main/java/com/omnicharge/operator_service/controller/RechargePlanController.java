package com.omnicharge.operator_service.controller;

import com.omnicharge.operator_service.dto.RechargePlanDto;
import com.omnicharge.operator_service.dto.RechargePlanRequest;
import com.omnicharge.operator_service.service.RechargePlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plans")
public class RechargePlanController {

    @Autowired
    private RechargePlanService rechargePlanService;

    // Add new plan - ROLE_ADMIN only
    @PostMapping
    public ResponseEntity<RechargePlanDto> addPlan(@RequestBody RechargePlanRequest request) {
        RechargePlanDto dto = rechargePlanService.addPlan(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    // Update plan - ROLE_ADMIN only
    @PutMapping("/{id}")
    public ResponseEntity<RechargePlanDto> updatePlan(@PathVariable Long id,
                                                       @RequestBody RechargePlanRequest request) {
        RechargePlanDto dto = rechargePlanService.updatePlan(id, request);
        return ResponseEntity.ok(dto);
    }

    // Delete plan - ROLE_ADMIN only
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deletePlan(@PathVariable Long id) {
        rechargePlanService.deletePlan(id);
        return ResponseEntity.ok("Plan deleted successfully");
    }

    // Get plan by id - any authenticated user
    @GetMapping("/{id}")
    public ResponseEntity<RechargePlanDto> getPlanById(@PathVariable Long id) {
        RechargePlanDto dto = rechargePlanService.getPlanById(id);
        return ResponseEntity.ok(dto);
    }

    // Get all plans - any authenticated user
    @GetMapping
    public ResponseEntity<List<RechargePlanDto>> getAllPlans() {
        List<RechargePlanDto> plans = rechargePlanService.getAllPlans();
        return ResponseEntity.ok(plans);
    }

    // Get all plans by operator - any authenticated user
    @GetMapping("/operator/{operatorId}")
    public ResponseEntity<List<RechargePlanDto>> getPlansByOperator(
            @PathVariable Long operatorId) {
        List<RechargePlanDto> plans = rechargePlanService.getPlansByOperator(operatorId);
        return ResponseEntity.ok(plans);
    }

    // Get active plans by operator - any authenticated user
    @GetMapping("/operator/{operatorId}/active")
    public ResponseEntity<List<RechargePlanDto>> getActivePlansByOperator(
            @PathVariable Long operatorId) {
        List<RechargePlanDto> plans = rechargePlanService.getActivePlansByOperator(operatorId);
        return ResponseEntity.ok(plans);
    }

    // Get plans by category - any authenticated user
    @GetMapping("/category/{category}")
    public ResponseEntity<List<RechargePlanDto>> getPlansByCategory(
            @PathVariable String category) {
        List<RechargePlanDto> plans = rechargePlanService.getPlansByCategory(category);
        return ResponseEntity.ok(plans);
    }
}
