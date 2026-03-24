package com.omnicharge.operator_service.controller;

import com.omnicharge.operator_service.dto.OperatorDto;
import com.omnicharge.operator_service.dto.OperatorRequest;
import com.omnicharge.operator_service.service.OperatorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/operators")
public class OperatorController {

	@Autowired
	private OperatorService operatorService;

	// Add new operator - ROLE_ADMIN only
	@PostMapping
	public ResponseEntity<OperatorDto> addOperator(@RequestBody OperatorRequest request) {
		OperatorDto dto = operatorService.addOperator(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(dto);
	}

	// Update operator - ROLE_ADMIN only
	@PutMapping("/{id}")
	public ResponseEntity<OperatorDto> updateOperator(@PathVariable Long id, @RequestBody OperatorRequest request) {
		OperatorDto dto = operatorService.updateOperator(id, request);
		return ResponseEntity.ok(dto);
	}

	// Delete operator - ROLE_ADMIN only
	@DeleteMapping("/{id}")
	public ResponseEntity<String> deleteOperator(@PathVariable Long id) {
		operatorService.deleteOperator(id);
		return ResponseEntity.ok("Operator deleted successfully");
	}

	// Get operator by id - any authenticated user
	@GetMapping("/{id}")
	public ResponseEntity<OperatorDto> getOperatorById(@PathVariable Long id) {
		OperatorDto dto = operatorService.getOperatorById(id);
		return ResponseEntity.ok(dto);
	}

	// Get all operators - any authenticated user
	@GetMapping
	public ResponseEntity<List<OperatorDto>> getAllOperators() {
		List<OperatorDto> operators = operatorService.getAllOperators();
		return ResponseEntity.ok(operators);
	}

	// Get operators by status - any authenticated user
	@GetMapping("/status/{status}")
	public ResponseEntity<List<OperatorDto>> getOperatorsByStatus(@PathVariable String status) {
		List<OperatorDto> operators = operatorService.getOperatorsByStatus(status);
		return ResponseEntity.ok(operators);
	}

	// Get operators by type - any authenticated user
	@GetMapping("/type/{type}")
	public ResponseEntity<List<OperatorDto>> getOperatorsByType(@PathVariable String type) {
		List<OperatorDto> operators = operatorService.getOperatorsByType(type);
		return ResponseEntity.ok(operators);
	}
}
