package com.omnicharge.payment_service.service;

import com.omnicharge.payment_service.dto.*;
import com.omnicharge.payment_service.entity.PaymentMethod;
import com.omnicharge.payment_service.entity.Transaction;
import com.omnicharge.payment_service.entity.TransactionStatus;
import com.omnicharge.payment_service.feign.RechargeServiceFeignClient;
import com.omnicharge.payment_service.messaging.PaymentResultPublisher;
import com.omnicharge.payment_service.repository.TransactionRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PaymentServiceImpl implements PaymentService {

	@Autowired
	private TransactionRepository transactionRepository;

	@Autowired
	private RechargeServiceFeignClient rechargeServiceFeignClient;

	@Autowired
	private PaymentResultPublisher paymentResultPublisher;

	@Autowired
	private DummyPaymentGatewayService paymentGatewayService;

	// Step 1 - Called by RabbitMQ when recharge is initiated
	// Creates a PENDING transaction and waits for user to make payment
	@Override
	@CircuitBreaker(name = "paymentService", fallbackMethod = "paymentFallback")
	public TransactionDto processPayment(RechargeEventMessage event) {

		// Check if transaction already exists
		if (transactionRepository.findByRechargeId(event.getRechargeId()).isPresent()) {
			return mapToDto(transactionRepository.findByRechargeId(event.getRechargeId()).get());
		}

		// Create transaction in PENDING state
		// User must call makePayment to complete it
		Transaction transaction = new Transaction();
		transaction.setTransactionId(generateTransactionId());
		transaction.setRechargeId(event.getRechargeId());
		transaction.setUsername(event.getUsername());
		transaction.setMobileNumber(event.getMobileNumber());
		transaction.setOperatorName(event.getOperatorName());
		transaction.setPlanName(event.getPlanName());
		transaction.setAmount(event.getAmount());
		transaction.setValidity(event.getValidity());
		transaction.setDataInfo(event.getDataInfo());
		transaction.setStatus(TransactionStatus.PENDING);

		transactionRepository.save(transaction);

		System.out.println();
		System.out.println("==================================================");
		System.out.println("  TRANSACTION CREATED - AWAITING PAYMENT");
		System.out.println("==================================================");
		System.out.println("  Transaction ID : " + transaction.getTransactionId());
		System.out.println("  Recharge ID    : " + event.getRechargeId());
		System.out.println("  Username       : " + event.getUsername());
		System.out.println("  Amount         : Rs. " + event.getAmount());
		System.out.println("  Status         : PENDING");
		System.out.println("  Use /api/transactions/pay to complete payment");
		System.out.println("==================================================");
		System.out.println();

		return mapToDto(transaction);
	}

	// Fallback when circuit breaker is OPEN
	public TransactionDto paymentFallback(RechargeEventMessage event, Exception ex) {
		Transaction transaction = new Transaction();
		transaction.setTransactionId(generateTransactionId());
		transaction.setRechargeId(event.getRechargeId());
		transaction.setUsername(event.getUsername());
		transaction.setMobileNumber(event.getMobileNumber());
		transaction.setOperatorName(event.getOperatorName());
		transaction.setPlanName(event.getPlanName());
		transaction.setAmount(event.getAmount());
		transaction.setValidity(event.getValidity());
		transaction.setDataInfo(event.getDataInfo());
		transaction.setStatus(TransactionStatus.FAILED);
		transaction.setFailureReason("Payment service temporarily unavailable. Please try again later");

		transactionRepository.save(transaction);
		updateRechargeStatus(event.getRechargeId(), transaction);
		publishNotification(event.getRechargeId(), transaction);

		return mapToDto(transaction);
	}

	// Step 2 - Called by user to actually make the payment
	@Override
	public TransactionDto makePayment(String username, PaymentGatewayRequest request) {

		// Find the pending transaction for this recharge
		Transaction transaction = transactionRepository.findByRechargeId(request.getRechargeId())
				.orElseThrow(() -> new RuntimeException("No pending transaction found for recharge id: "
						+ request.getRechargeId() + ". Please initiate recharge first"));

		// Only the owner can pay for their own recharge
		if (!transaction.getUsername().equals(username)) {
			throw new RuntimeException("Access denied. You can only pay for your own recharge");
		}

		// Check if already paid
		if (transaction.getStatus() != TransactionStatus.PENDING) {
			throw new RuntimeException(
					"Transaction is already " + transaction.getStatus().name() + ". Cannot process again");
		}

		System.out.println();
		System.out.println("==================================================");
		System.out.println("  PAYMENT GATEWAY - PROCESSING PAYMENT");
		System.out.println("==================================================");
		System.out.println("  Transaction ID  : " + transaction.getTransactionId());
		System.out.println("  Amount          : Rs. " + transaction.getAmount());
		System.out.println("  Payment Method  : " + request.getPaymentMethod());
		System.out.println("==================================================");

		// Process through dummy payment gateway
		PaymentGatewayResponse gatewayResponse = paymentGatewayService.processPayment(request, transaction.getAmount());

		// Update transaction based on gateway response
		if (gatewayResponse.isSuccess()) {
			transaction.setStatus(TransactionStatus.SUCCESS);
			transaction.setPaymentReference(gatewayResponse.getPaymentReference());
		} else {
			transaction.setStatus(TransactionStatus.FAILED);
			transaction.setFailureReason(gatewayResponse.getFailureReason());
		}

		transaction.setPaymentMethod(PaymentMethod.valueOf(request.getPaymentMethod().toUpperCase()));
		transactionRepository.save(transaction);

		System.out.println();
		System.out.println("--------------------------------------------------");
		System.out.println("  PAYMENT GATEWAY RESULT");
		System.out.println("--------------------------------------------------");
		System.out.println("  Status          : " + transaction.getStatus());
		System.out.println("  Payment Ref     : " + transaction.getPaymentReference());
		if (transaction.getFailureReason() != null) {
			System.out.println("  Failure Reason  : " + transaction.getFailureReason());
		}
		System.out.println("--------------------------------------------------");
		System.out.println();

		// Update recharge status
		updateRechargeStatus(transaction.getRechargeId(), transaction);

		// Send notification with final status
		publishNotification(transaction.getRechargeId(), transaction);

		return mapToDto(transaction);
	}

	private void updateRechargeStatus(Long rechargeId, Transaction transaction) {
		try {
			RechargeStatusUpdateRequest updateRequest = new RechargeStatusUpdateRequest(transaction.getStatus().name(),
					transaction.getFailureReason());
			rechargeServiceFeignClient.updateRechargeStatus(rechargeId, updateRequest);
		} catch (Exception ex) {
			System.err.println("Failed to update recharge status: " + ex.getMessage());
		}
	}

	private void publishNotification(Long rechargeId, Transaction transaction) {
		try {
			PaymentResultMessage result = new PaymentResultMessage();
			result.setRechargeId(rechargeId);
			result.setTransactionId(transaction.getTransactionId());
			result.setUsername(transaction.getUsername());
			result.setMobileNumber(transaction.getMobileNumber());
			result.setOperatorName(transaction.getOperatorName());
			result.setPlanName(transaction.getPlanName());
			result.setAmount(transaction.getAmount());
			result.setValidity(transaction.getValidity());
			result.setDataInfo(transaction.getDataInfo());
			result.setStatus(transaction.getStatus().name());
			result.setFailureReason(transaction.getFailureReason());
			result.setProcessedAt(LocalDateTime.now());
			paymentResultPublisher.publishPaymentResult(result);
		} catch (Exception ex) {
			System.err.println("Failed to publish notification: " + ex.getMessage());
		}
	}

	@Override
	public List<TransactionDto> getMyTransactions(String username) {
		return transactionRepository.findByUsernameOrderByCreatedAtDesc(username).stream().map(this::mapToDto)
				.collect(Collectors.toList());
	}

	@Override
	public TransactionDto getByTransactionId(String transactionId) {
		Transaction transaction = transactionRepository.findByTransactionId(transactionId)
				.orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));
		return mapToDto(transaction);
	}

	@Override
	public TransactionDto getByRechargeId(Long rechargeId) {
		Transaction transaction = transactionRepository.findByRechargeId(rechargeId)
				.orElseThrow(() -> new RuntimeException("Transaction not found for recharge id: " + rechargeId));
		return mapToDto(transaction);
	}

	@Override
	public List<TransactionDto> getAllTransactions() {
		return transactionRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapToDto)
				.collect(Collectors.toList());
	}

	@Override
	public List<TransactionDto> getTransactionsByStatus(String status) {
		try {
			TransactionStatus txStatus = TransactionStatus.valueOf(status.toUpperCase());
			return transactionRepository.findByStatus(txStatus).stream().map(this::mapToDto)
					.collect(Collectors.toList());
		} catch (IllegalArgumentException ex) {
			throw new RuntimeException("Invalid status. Valid: PENDING, SUCCESS, FAILED");
		}
	}

	@Override
	public List<TransactionDto> getTransactionsByMobile(String mobileNumber) {
		return transactionRepository.findByMobileNumber(mobileNumber).stream().map(this::mapToDto)
				.collect(Collectors.toList());
	}

	private String generateTransactionId() {
		return "TXN" + UUID.randomUUID().toString().replace("-", "").toUpperCase().substring(0, 12);
	}

	private TransactionDto mapToDto(Transaction transaction) {
		TransactionDto dto = new TransactionDto();
		dto.setId(transaction.getId());
		dto.setTransactionId(transaction.getTransactionId());
		dto.setRechargeId(transaction.getRechargeId());
		dto.setUsername(transaction.getUsername());
		dto.setMobileNumber(transaction.getMobileNumber());
		dto.setOperatorName(transaction.getOperatorName());
		dto.setPlanName(transaction.getPlanName());
		dto.setAmount(transaction.getAmount());
		dto.setValidity(transaction.getValidity());
		dto.setDataInfo(transaction.getDataInfo());
		dto.setStatus(transaction.getStatus().name());
		dto.setPaymentMethod(transaction.getPaymentMethod() != null ? transaction.getPaymentMethod().name() : null);
		dto.setPaymentReference(transaction.getPaymentReference());
		dto.setFailureReason(transaction.getFailureReason());
		dto.setCreatedAt(transaction.getCreatedAt());
		dto.setUpdatedAt(transaction.getUpdatedAt());
		return dto;
	}
}