package com.omnicharge.payment_service.messaging;

import com.omnicharge.payment_service.dto.RechargeEventMessage;
import com.omnicharge.payment_service.dto.TransactionDto;
import com.omnicharge.payment_service.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RechargeEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(RechargeEventConsumer.class);

    @Autowired
    private PaymentService paymentService;

    @RabbitListener(queues = "${rabbitmq.payment.queue}")
    public void consumeRechargeEvent(RechargeEventMessage event) {

        log.info("==================================================");
        log.info("        PAYMENT SERVICE - EVENT RECEIVED           ");
        log.info("==================================================");
        log.info("  Recharge ID : {}", event.getRechargeId());
        log.info("  Username    : {}", event.getUsername());
        log.info("  Mobile      : {}", event.getMobileNumber());
        log.info("  Operator    : {}", event.getOperatorName());
        log.info("  Plan        : {}", event.getPlanName());
        log.info("  Amount      : Rs. {}", event.getAmount());
        log.info("==================================================");

        TransactionDto result = paymentService.processPayment(event);

        log.info("--------------------------------------------------");
        log.info("           PAYMENT PROCESSING RESULT               ");
        log.info("--------------------------------------------------");
        log.info("  Recharge ID    : {}", event.getRechargeId());
        log.info("  Transaction ID : {}", result.getTransactionId());
        log.info("  Status         : {}", result.getStatus());
        if (result.getFailureReason() != null) {
            log.warn("  Failure Reason : {}", result.getFailureReason());
        }
        log.info("  Publishing result to Notification Service...");
        log.info("--------------------------------------------------");
    }
}
