package com.omnicharge.payment_service.controller;

import com.omnicharge.payment_service.dto.PaymentGatewayRequest;
import com.omnicharge.payment_service.dto.TransactionDto;
import com.omnicharge.payment_service.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

	@Autowired
	private PaymentService paymentService;

	// Make payment for a recharge - any authenticated user
	@PostMapping("/pay")
	public ResponseEntity<TransactionDto> makePayment(Authentication authentication,
			@RequestBody PaymentGatewayRequest request) {
		TransactionDto dto = paymentService.makePayment(authentication.getName(), request);
		return ResponseEntity.ok(dto);
	}

	// Get own transactions - any authenticated user
	@GetMapping("/my-transactions")
	public ResponseEntity<List<TransactionDto>> getMyTransactions(Authentication authentication) {
		List<TransactionDto> list = paymentService.getMyTransactions(authentication.getName());
		return ResponseEntity.ok(list);
	}

	// Get transaction by transactionId
	@GetMapping("/txn/{transactionId}")
	public ResponseEntity<TransactionDto> getByTransactionId(@PathVariable String transactionId) {
		TransactionDto dto = paymentService.getByTransactionId(transactionId);
		return ResponseEntity.ok(dto);
	}

	// Get transaction by rechargeId
	@GetMapping("/recharge/{rechargeId}")
	public ResponseEntity<TransactionDto> getByRechargeId(@PathVariable Long rechargeId) {
		TransactionDto dto = paymentService.getByRechargeId(rechargeId);
		return ResponseEntity.ok(dto);
	}

	// Get all transactions - ROLE_ADMIN only
	@GetMapping("/all")
	public ResponseEntity<List<TransactionDto>> getAllTransactions() {
		List<TransactionDto> list = paymentService.getAllTransactions();
		return ResponseEntity.ok(list);
	}

	// Get transactions by status - ROLE_ADMIN only
	@GetMapping("/status/{status}")
	public ResponseEntity<List<TransactionDto>> getByStatus(@PathVariable String status) {
		List<TransactionDto> list = paymentService.getTransactionsByStatus(status);
		return ResponseEntity.ok(list);
	}

	// Get transactions by mobile number - ROLE_ADMIN only
	@GetMapping("/mobile/{mobileNumber}")
	public ResponseEntity<List<TransactionDto>> getByMobile(@PathVariable String mobileNumber) {
		List<TransactionDto> list = paymentService.getTransactionsByMobile(mobileNumber);
		return ResponseEntity.ok(list);
	}
}