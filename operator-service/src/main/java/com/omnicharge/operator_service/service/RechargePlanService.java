package com.omnicharge.operator_service.service;

import com.omnicharge.operator_service.dto.RechargePlanDto;
import com.omnicharge.operator_service.dto.RechargePlanRequest;

import java.util.List;

public interface RechargePlanService {

    RechargePlanDto addPlan(RechargePlanRequest request);

    RechargePlanDto updatePlan(Long id, RechargePlanRequest request);

    /** PATCH — toggles only the status field (ACTIVE / INACTIVE). */
    RechargePlanDto updatePlanStatus(Long id, String status);

    void deletePlan(Long id);

    RechargePlanDto getPlanById(Long id);

    List<RechargePlanDto> getAllPlans();

    List<RechargePlanDto> getPlansByOperator(Long operatorId);

    List<RechargePlanDto> getActivePlansByOperator(Long operatorId);

    List<RechargePlanDto> getPlansByCategory(String category);
}