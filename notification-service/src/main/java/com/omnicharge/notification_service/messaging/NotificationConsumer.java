package com.omnicharge.notification_service.messaging;

import com.omnicharge.notification_service.dto.PaymentResultMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

    @RabbitListener(queues = "${rabbitmq.notification.queue}")
    public void consumePaymentResult(PaymentResultMessage result) {

        log.info("==================================================");
        log.info("      NOTIFICATION SERVICE - PAYMENT RESULT        ");
        log.info("==================================================");
        log.info("  Recharge ID     : {}", result.getRechargeId());
        log.info("  Transaction ID  : {}", result.getTransactionId());
        log.info("  Username        : {}", result.getUsername());
        log.info("  Mobile          : {}", result.getMobileNumber());
        log.info("  Operator        : {}", result.getOperatorName());
        log.info("  Plan            : {}", result.getPlanName());
        log.info("  Amount          : Rs. {}", result.getAmount());
        log.info("  Validity        : {}", result.getValidity());
        log.info("  Data            : {}", result.getDataInfo());
        log.info("  Payment Status  : {}", result.getStatus());

        if (result.getFailureReason() != null) {
            log.warn("  Failure Reason  : {}", result.getFailureReason());
        }
        log.info("  Processed At    : {}", result.getProcessedAt());
        log.info("==================================================");

        sendSmsNotification(result);
        sendEmailNotification(result);

        log.info("  NOTIFICATIONS SENT SUCCESSFULLY");
        log.info("  RechargeId     : {}", result.getRechargeId());
        log.info("  TransactionId  : {}", result.getTransactionId());
        log.info("  Final Status   : {}", result.getStatus());
        log.info("==================================================");
    }

    private void sendSmsNotification(PaymentResultMessage result) {
        String sms;

        if ("SUCCESS".equals(result.getStatus())) {
            sms = String.format(
                    "Dear %s, your recharge of Rs.%.0f for %s " +
                    "with %s plan %s is SUCCESSFUL. " +
                    "TxnID: %s | Validity: %s | Data: %s - Team OmniCharge",
                    result.getUsername(),
                    result.getAmount(),
                    result.getMobileNumber(),
                    result.getOperatorName(),
                    result.getPlanName(),
                    result.getTransactionId(),
                    result.getValidity(),
                    result.getDataInfo()
            );
        } else {
            sms = String.format(
                    "Dear %s, your recharge of Rs.%.0f for %s FAILED. " +
                    "Reason: %s. Please try again - Team OmniCharge",
                    result.getUsername(),
                    result.getAmount(),
                    result.getMobileNumber(),
                    result.getFailureReason() != null
                            ? result.getFailureReason() : "Unknown error"
            );
        }

        log.info("--------------------------------------------------");
        log.info("                 SMS NOTIFICATION                  ");
        log.info("--------------------------------------------------");
        log.info("  To      : {}", result.getMobileNumber());
        log.info("  Message : {}", sms);
        log.info("--------------------------------------------------");
    }

    private void sendEmailNotification(PaymentResultMessage result) {
        log.info("--------------------------------------------------");
        log.info("                EMAIL NOTIFICATION                 ");
        log.info("--------------------------------------------------");
        log.info("  To      : {}@omnicharge.com", result.getUsername());
        log.info("  Subject : Recharge {} - OmniCharge", result.getStatus());
        log.info("  Body    :");
        log.info("  Dear {},", result.getUsername());

        if ("SUCCESS".equals(result.getStatus())) {
            log.info("  Your recharge was SUCCESSFUL.");
        } else {
            log.warn("  Your recharge has FAILED.");
            log.warn("  Reason : {}", result.getFailureReason());
        }

        log.info("  Transaction ID : {}", result.getTransactionId());
        log.info("  Recharge ID    : {}", result.getRechargeId());
        log.info("  Mobile         : {}", result.getMobileNumber());
        log.info("  Operator       : {}", result.getOperatorName());
        log.info("  Plan           : {}", result.getPlanName());
        log.info("  Amount         : Rs. {}", result.getAmount());
        log.info("  Validity       : {}", result.getValidity());
        log.info("  Data           : {}", result.getDataInfo());
        log.info("  Processed At   : {}", result.getProcessedAt());
        log.info("  Thank you for using OmniCharge.");
        log.info("  Team OmniCharge");
        log.info("--------------------------------------------------");
    }
}