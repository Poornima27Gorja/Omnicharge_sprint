package com.omnicharge.notification_service.messaging;

import com.omnicharge.notification_service.dto.PaymentResultMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Tests for NotificationConsumer.
 *
 * Since the consumer's job is to log + (in production) dispatch SMS/Email,
 * we verify:
 *  1. No exception is thrown for SUCCESS messages.
 *  2. No exception is thrown for FAILED messages (including null failureReason).
 *  3. The method completes without throwing for various edge-case inputs.
 *
 * In a real project you would also wire in a mail/SMS mock and verify calls.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationConsumer Unit Tests")
class NotificationConsumerTest {

    @InjectMocks
    private NotificationConsumer notificationConsumer;

    // ── helpers ───────────────────────────────────────────────────────────────

    private PaymentResultMessage buildMessage(String status, String failureReason) {
        PaymentResultMessage msg = new PaymentResultMessage();
        msg.setRechargeId(100L);
        msg.setTransactionId("TXN123456789ABC");
        msg.setUsername("alice");
        msg.setMobileNumber("9876543210");
        msg.setOperatorName("Airtel");
        msg.setPlanName("Basic 149");
        msg.setAmount(149.0);
        msg.setValidity("28 days");
        msg.setDataInfo("1.5 GB/day");
        msg.setStatus(status);
        msg.setFailureReason(failureReason);
        msg.setProcessedAt(LocalDateTime.now());
        return msg;
    }

    // ── consumePaymentResult() ────────────────────────────────────────────────

    @Test
    @DisplayName("consumePaymentResult() - SUCCESS message processed without exception")
    void consume_successMessage_noException() {
        PaymentResultMessage msg = buildMessage("SUCCESS", null);

        assertThatNoException().isThrownBy(
                () -> notificationConsumer.consumePaymentResult(msg));
    }

    @Test
    @DisplayName("consumePaymentResult() - FAILED message with reason processed without exception")
    void consume_failedMessageWithReason_noException() {
        PaymentResultMessage msg = buildMessage("FAILED", "Insufficient funds in your account");

        assertThatNoException().isThrownBy(
                () -> notificationConsumer.consumePaymentResult(msg));
    }

    @Test
    @DisplayName("consumePaymentResult() - FAILED message with null reason handled gracefully")
    void consume_failedMessageNullReason_noException() {
        PaymentResultMessage msg = buildMessage("FAILED", null);

        assertThatNoException().isThrownBy(
                () -> notificationConsumer.consumePaymentResult(msg));
    }

    @Test
    @DisplayName("consumePaymentResult() - zero amount handled without exception")
    void consume_zeroAmount_noException() {
        PaymentResultMessage msg = buildMessage("FAILED", "Plan price is zero");
        msg.setAmount(0.0);

        assertThatNoException().isThrownBy(
                () -> notificationConsumer.consumePaymentResult(msg));
    }

    @Test
    @DisplayName("consumePaymentResult() - large amount handled without exception")
    void consume_largeAmount_noException() {
        PaymentResultMessage msg = buildMessage("SUCCESS", null);
        msg.setAmount(9999.0);

        assertThatNoException().isThrownBy(
                () -> notificationConsumer.consumePaymentResult(msg));
    }
}
