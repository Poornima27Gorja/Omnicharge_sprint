package com.omnicharge.recharge_service.controller;

import com.omnicharge.recharge_service.dto.InitiateRechargeRequest;
import com.omnicharge.recharge_service.dto.RechargeRequestDto;
import com.omnicharge.recharge_service.dto.RechargeStatusUpdateRequest;
import com.omnicharge.recharge_service.service.RechargeService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recharge")
public class RechargeController {

	@Autowired
	private RechargeService rechargeService;

	// POST /api/recharge/initiate - must be defined before /{id}
	@PostMapping("/initiate")
	public ResponseEntity<RechargeRequestDto> initiateRecharge(Authentication authentication,
			HttpServletRequest httpRequest, @RequestBody InitiateRechargeRequest request) {
		String authToken = httpRequest.getHeader("Authorization");
		RechargeRequestDto dto = rechargeService.initiateRecharge(authentication.getName(), authToken, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(dto);
	}

	// GET /api/recharge/my-history - must be defined before /{id}
	@GetMapping("/my-history")
	public ResponseEntity<List<RechargeRequestDto>> getMyHistory(Authentication authentication) {
		List<RechargeRequestDto> list = rechargeService.getMyRechargeHistory(authentication.getName());
		return ResponseEntity.ok(list);
	}

	// GET /api/recharge/all - must be defined before /{id}
	@GetMapping("/all")
	public ResponseEntity<List<RechargeRequestDto>> getAllRecharges() {
		List<RechargeRequestDto> list = rechargeService.getAllRecharges();
		return ResponseEntity.ok(list);
	}

	// GET /api/recharge/status/{status} - must be defined before /{id}
	@GetMapping("/status/{status}")
	public ResponseEntity<List<RechargeRequestDto>> getByStatus(@PathVariable String status) {
		List<RechargeRequestDto> list = rechargeService.getRechargesByStatus(status);
		return ResponseEntity.ok(list);
	}

	// GET /api/recharge/mobile/{mobileNumber} - must be defined before /{id}
	@GetMapping("/mobile/{mobileNumber}")
	public ResponseEntity<List<RechargeRequestDto>> getByMobile(@PathVariable String mobileNumber) {
		List<RechargeRequestDto> list = rechargeService.getRechargesByMobile(mobileNumber);
		return ResponseEntity.ok(list);
	}

	// GET /api/recharge/{id} - generic pattern must always be LAST
	@GetMapping("/{id}")
	public ResponseEntity<RechargeRequestDto> getRechargeById(@PathVariable Long id, Authentication authentication) {
		boolean isAdmin = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
		RechargeRequestDto dto = rechargeService.getRechargeById(id, authentication.getName(), isAdmin);
		return ResponseEntity.ok(dto);
	}

	// Called internally by payment-service to update recharge status
	// Not exposed to users - only internal service communication
	@PutMapping("/update-status/{rechargeId}")
	public ResponseEntity<RechargeRequestDto> updateRechargeStatus(@PathVariable Long rechargeId,
			@RequestBody RechargeStatusUpdateRequest request) {
		RechargeRequestDto dto = rechargeService.updateRechargeStatus(rechargeId, request);
		return ResponseEntity.ok(dto);
	}
}
