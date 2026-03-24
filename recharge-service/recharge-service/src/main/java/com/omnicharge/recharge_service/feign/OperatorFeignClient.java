package com.omnicharge.recharge_service.feign;

import com.omnicharge.recharge_service.dto.OperatorResponse;
import com.omnicharge.recharge_service.dto.PlanResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

// Feign client calls operator-service via Eureka service discovery
@FeignClient(name = "operator-service")
public interface OperatorFeignClient {

    // Get operator details by id from operator-service
    @GetMapping("/api/operators/{id}")
    OperatorResponse getOperatorById(
            @PathVariable("id") Long id,
            @RequestHeader("Authorization") String token);

    // Get plan details by id from operator-service
    @GetMapping("/api/plans/{id}")
    PlanResponse getPlanById(
            @PathVariable("id") Long id,
            @RequestHeader("Authorization") String token);
}
