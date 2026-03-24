package com.omnicharge.payment_service.messaging;

import com.omnicharge.payment_service.dto.RechargeEventMessage;
import com.omnicharge.payment_service.dto.TransactionDto;
import com.omnicharge.payment_service.service.PaymentService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RechargeEventConsumer {

    @Autowired
    private PaymentService paymentService;

    @RabbitListener(queues = "${rabbitmq.payment.queue}")
    public void consumeRechargeEvent(RechargeEventMessage event) {

        System.out.println();
        System.out.println("==================================================");
        System.out.println("        PAYMENT SERVICE - EVENT RECEIVED           ");
        System.out.println("==================================================");
        System.out.println("  Recharge ID : " + event.getRechargeId());
        System.out.println("  Username    : " + event.getUsername());
        System.out.println("  Mobile      : " + event.getMobileNumber());
        System.out.println("  Operator    : " + event.getOperatorName());
        System.out.println("  Plan        : " + event.getPlanName());
        System.out.println("  Amount      : Rs. " + event.getAmount());
        System.out.println("==================================================");

        TransactionDto result = paymentService.processPayment(event);

        System.out.println();
        System.out.println("--------------------------------------------------");
        System.out.println("           PAYMENT PROCESSING RESULT               ");
        System.out.println("--------------------------------------------------");
        System.out.println("  Recharge ID    : " + event.getRechargeId());
        System.out.println("  Transaction ID : " + result.getTransactionId());
        System.out.println("  Status         : " + result.getStatus());
        if (result.getFailureReason() != null) {
            System.out.println("  Failure Reason : " + result.getFailureReason());
        }
        System.out.println("--------------------------------------------------");
        System.out.println("  Publishing result to Notification Service...");
        System.out.println("--------------------------------------------------");
        System.out.println();
    }
}