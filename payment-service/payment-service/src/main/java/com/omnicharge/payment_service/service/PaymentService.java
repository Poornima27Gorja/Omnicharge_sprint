package com.omnicharge.payment_service.service;

import com.omnicharge.payment_service.dto.PaymentGatewayRequest;
import com.omnicharge.payment_service.dto.RechargeEventMessage;
import com.omnicharge.payment_service.dto.TransactionDto;

import java.util.List;

public interface PaymentService {

    // Called by RabbitMQ consumer - creates pending transaction
    TransactionDto processPayment(RechargeEventMessage event);

    // Called by user to actually pay - processes through payment gateway
    TransactionDto makePayment(String username, PaymentGatewayRequest request);

    List<TransactionDto> getMyTransactions(String username);

    TransactionDto getByTransactionId(String transactionId);

    TransactionDto getByRechargeId(Long rechargeId);

    List<TransactionDto> getAllTransactions();

    List<TransactionDto> getTransactionsByStatus(String status);

    List<TransactionDto> getTransactionsByMobile(String mobileNumber);
}