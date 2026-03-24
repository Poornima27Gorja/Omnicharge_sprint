package com.omnicharge.payment_service.feign;

import com.omnicharge.payment_service.dto.RechargeStatusUpdateRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

// Feign client to call recharge-service and update status
@FeignClient(name = "recharge-service")
public interface RechargeServiceFeignClient {

	@PutMapping("/api/recharge/update-status/{rechargeId}")
	void updateRechargeStatus(@PathVariable("rechargeId") Long rechargeId,
			@RequestBody RechargeStatusUpdateRequest request);
}