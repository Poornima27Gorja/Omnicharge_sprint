package com.omnicharge.notification_service.messaging;

import com.omnicharge.notification_service.dto.PaymentResultMessage;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationConsumer {

    // Now listens to notification queue which receives ONLY after payment is done
    @RabbitListener(queues = "${rabbitmq.notification.queue}")
    public void consumePaymentResult(PaymentResultMessage result) {

        System.out.println();
        System.out.println("==================================================");
        System.out.println("      NOTIFICATION SERVICE - PAYMENT RESULT        ");
        System.out.println("==================================================");
        System.out.println("  Recharge ID     : " + result.getRechargeId());
        System.out.println("  Transaction ID  : " + result.getTransactionId());
        System.out.println("  Username        : " + result.getUsername());
        System.out.println("  Mobile          : " + result.getMobileNumber());
        System.out.println("  Operator        : " + result.getOperatorName());
        System.out.println("  Plan            : " + result.getPlanName());
        System.out.println("  Amount          : Rs. " + result.getAmount());
        System.out.println("  Validity        : " + result.getValidity());
        System.out.println("  Data            : " + result.getDataInfo());
        System.out.println("  Payment Status  : " + result.getStatus());
        if (result.getFailureReason() != null) {
            System.out.println("  Failure Reason  : " + result.getFailureReason());
        }
        System.out.println("  Processed At    : " + result.getProcessedAt());
        System.out.println("==================================================");

        sendSmsNotification(result);
        sendEmailNotification(result);

        System.out.println();
        System.out.println("  NOTIFICATIONS SENT SUCCESSFULLY");
        System.out.println("  RechargeId     : " + result.getRechargeId());
        System.out.println("  TransactionId  : " + result.getTransactionId());
        System.out.println("  Final Status   : " + result.getStatus());
        System.out.println("==================================================");
        System.out.println();
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

        System.out.println();
        System.out.println("--------------------------------------------------");
        System.out.println("                 SMS NOTIFICATION                  ");
        System.out.println("--------------------------------------------------");
        System.out.println("  To      : " + result.getMobileNumber());
        System.out.println("  Message : " + sms);
        System.out.println("--------------------------------------------------");
    }

    private void sendEmailNotification(PaymentResultMessage result) {
        System.out.println();
        System.out.println("--------------------------------------------------");
        System.out.println("                EMAIL NOTIFICATION                 ");
        System.out.println("--------------------------------------------------");
        System.out.println("  To      : " + result.getUsername() + "@omnicharge.com");
        System.out.println("  Subject : Recharge " + result.getStatus()
                + " - OmniCharge");
        System.out.println("  Body    :");
        System.out.println();
        System.out.println("  Dear " + result.getUsername() + ",");
        System.out.println();

        if ("SUCCESS".equals(result.getStatus())) {
            System.out.println("  Your recharge was SUCCESSFUL.");
        } else {
            System.out.println("  Your recharge has FAILED.");
            System.out.println("  Reason : " + result.getFailureReason());
        }

        System.out.println();
        System.out.println("  Transaction ID : " + result.getTransactionId());
        System.out.println("  Recharge ID    : " + result.getRechargeId());
        System.out.println("  Mobile         : " + result.getMobileNumber());
        System.out.println("  Operator       : " + result.getOperatorName());
        System.out.println("  Plan           : " + result.getPlanName());
        System.out.println("  Amount         : Rs. " + result.getAmount());
        System.out.println("  Validity       : " + result.getValidity());
        System.out.println("  Data           : " + result.getDataInfo());
        System.out.println("  Processed At   : " + result.getProcessedAt());
        System.out.println();
        System.out.println("  Thank you for using OmniCharge.");
        System.out.println("  Team OmniCharge");
        System.out.println("--------------------------------------------------");
    }
}