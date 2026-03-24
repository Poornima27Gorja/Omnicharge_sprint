package com.omnicharge.payment_service.service;

import com.omnicharge.payment_service.dto.PaymentGatewayRequest;
import com.omnicharge.payment_service.dto.PaymentGatewayResponse;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class DummyPaymentGatewayService {

	// Main method called by payment service
	public PaymentGatewayResponse processPayment(PaymentGatewayRequest request, Double amount) {
		String method = request.getPaymentMethod().toUpperCase();

		switch (method) {
		case "CARD":
			return processCardPayment(request, amount);
		case "UPI":
			return processUpiPayment(request, amount);
		case "NETBANKING":
			return processNetbankingPayment(request, amount);
		case "WALLET":
			return processWalletPayment(request, amount);
		default:
			return failureResponse("Invalid payment method: " + method, method, amount);
		}
	}

	// Simulate CARD payment
	private PaymentGatewayResponse processCardPayment(PaymentGatewayRequest request, Double amount) {
		// Validate card number - must be 16 digits
		if (request.getCardNumber() == null || !request.getCardNumber().matches("\\d{16}")) {
			return failureResponse("Invalid card number. Must be 16 digits", "CARD", amount);
		}

		// Validate expiry - must be MM/YY format
		if (request.getCardExpiry() == null || !request.getCardExpiry().matches("(0[1-9]|1[0-2])/\\d{2}")) {
			return failureResponse("Invalid card expiry. Format must be MM/YY", "CARD", amount);
		}

		// Validate CVV - must be 3 digits
		if (request.getCardCvv() == null || !request.getCardCvv().matches("\\d{3}")) {
			return failureResponse("Invalid CVV. Must be 3 digits", "CARD", amount);
		}

		// Validate card holder name
		if (request.getCardHolderName() == null || request.getCardHolderName().trim().isEmpty()) {
			return failureResponse("Card holder name is required", "CARD", amount);
		}

		// Simulate specific card scenarios
		// Card ending in 0000 is always declined
		if (request.getCardNumber().endsWith("0000")) {
			return failureResponse("Card declined by bank. Please contact your bank", "CARD", amount);
		}

		// Card ending in 1111 simulates insufficient funds
		if (request.getCardNumber().endsWith("1111")) {
			return failureResponse("Insufficient funds in your account", "CARD", amount);
		}

		// Card ending in 2222 simulates expired card
		if (request.getCardNumber().endsWith("2222")) {
			return failureResponse("Card has expired", "CARD", amount);
		}

		// All other valid cards succeed
		return successResponse("CARD", amount, "CARD-" + request.getCardNumber().substring(12));
	}

	// Simulate UPI payment
	private PaymentGatewayResponse processUpiPayment(PaymentGatewayRequest request, Double amount) {
		// Validate UPI id format - must be something@bank
		if (request.getUpiId() == null || !request.getUpiId().matches("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z]{3,}$")) {
			return failureResponse("Invalid UPI ID format. Example: rahul@upi or rahul@okaxis", "UPI", amount);
		}

		// Simulate specific UPI scenarios
		// UPI id with "fail" fails always
		if (request.getUpiId().toLowerCase().contains("fail")) {
			return failureResponse("UPI transaction declined by bank", "UPI", amount);
		}

		// UPI id with "timeout" simulates timeout
		if (request.getUpiId().toLowerCase().contains("timeout")) {
			return failureResponse("UPI payment timed out. Please try again", "UPI", amount);
		}

		// All other valid UPI ids succeed
		return successResponse("UPI", amount, "UPI-" + request.getUpiId());
	}

	// Simulate NETBANKING payment
	private PaymentGatewayResponse processNetbankingPayment(PaymentGatewayRequest request, Double amount) {
		// Validate bank code
		if (request.getBankCode() == null || request.getBankCode().trim().isEmpty()) {
			return failureResponse("Bank code is required", "NETBANKING", amount);
		}

		// Valid bank codes
		java.util.List<String> validBanks = java.util.Arrays.asList("SBI", "HDFC", "ICICI", "AXIS", "KOTAK", "BOB",
				"PNB", "CANARA", "UNION", "INDUSIND");

		if (!validBanks.contains(request.getBankCode().toUpperCase())) {
			return failureResponse("Invalid bank code. Valid codes: SBI, HDFC, ICICI, AXIS, KOTAK, "
					+ "BOB, PNB, CANARA, UNION, INDUSIND", "NETBANKING", amount);
		}

		// Validate account number
		if (request.getAccountNumber() == null || !request.getAccountNumber().matches("\\d{9,18}")) {
			return failureResponse("Invalid account number. Must be 9 to 18 digits", "NETBANKING", amount);
		}

		// Simulate bank maintenance
		if ("BOB".equals(request.getBankCode().toUpperCase())) {
			return failureResponse("Bank of Baroda netbanking is under maintenance. Try after some time", "NETBANKING",
					amount);
		}

		return successResponse("NETBANKING", amount, "NB-" + request.getBankCode().toUpperCase() + "-"
				+ request.getAccountNumber().substring(request.getAccountNumber().length() - 4));
	}

	// Simulate WALLET payment
	private PaymentGatewayResponse processWalletPayment(PaymentGatewayRequest request, Double amount) {
		// Validate wallet type
		java.util.List<String> validWallets = java.util.Arrays.asList("PAYTM", "PHONEPE", "GOOGLEPAY", "AMAZONPAY",
				"MOBIKWIK");

		if (request.getWalletType() == null || !validWallets.contains(request.getWalletType().toUpperCase())) {
			return failureResponse("Invalid wallet type. Valid: PAYTM, PHONEPE, GOOGLEPAY, " + "AMAZONPAY, MOBIKWIK",
					"WALLET", amount);
		}

		// Validate wallet mobile
		if (request.getWalletMobile() == null || !request.getWalletMobile().matches("^[6-9]\\d{9}$")) {
			return failureResponse("Invalid wallet mobile number", "WALLET", amount);
		}

		// Simulate insufficient wallet balance for high amounts
		if (amount > 500) {
			return failureResponse(
					"Insufficient wallet balance. Please add money to your " + request.getWalletType() + " wallet",
					"WALLET", amount);
		}

		return successResponse("WALLET", amount,
				request.getWalletType().toUpperCase() + "-" + request.getWalletMobile().substring(6));
	}

	private PaymentGatewayResponse successResponse(String method, Double amount, String reference) {
		PaymentGatewayResponse response = new PaymentGatewayResponse();
		response.setSuccess(true);
		response.setStatus("SUCCESS");
		response.setPaymentMethod(method);
		response.setAmount(amount);
		response.setPaymentReference(
				"REF-" + reference + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
		response.setMessage("Payment processed successfully");
		return response;
	}

	private PaymentGatewayResponse failureResponse(String reason, String method, Double amount) {
		PaymentGatewayResponse response = new PaymentGatewayResponse();
		response.setSuccess(false);
		response.setStatus("FAILED");
		response.setPaymentMethod(method);
		response.setAmount(amount);
		response.setFailureReason(reason);
		response.setMessage("Payment failed");
		return response;
	}
}