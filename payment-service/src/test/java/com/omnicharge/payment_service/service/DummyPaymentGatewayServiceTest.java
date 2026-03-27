package com.omnicharge.payment_service.service;

import com.omnicharge.payment_service.dto.PaymentGatewayRequest;
import com.omnicharge.payment_service.dto.PaymentGatewayResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("DummyPaymentGatewayService Unit Tests")
class DummyPaymentGatewayServiceTest {

    private final DummyPaymentGatewayService gateway = new DummyPaymentGatewayService();

    // ── CARD ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("CARD - success: valid card details return SUCCESS")
    void card_validDetails_success() {
        PaymentGatewayRequest req = new PaymentGatewayRequest();
        req.setPaymentMethod("CARD");
        req.setCardNumber("4111111111119999");
        req.setCardExpiry("12/26");
        req.setCardCvv("123");
        req.setCardHolderName("Alice Smith");

        PaymentGatewayResponse resp = gateway.processPayment(req, 149.0);

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getStatus()).isEqualTo("SUCCESS");
        assertThat(resp.getPaymentReference()).isNotBlank();
    }

    @Test
    @DisplayName("CARD - fail: card ending in 0000 is declined")
    void card_declinedCard_failure() {
        PaymentGatewayRequest req = new PaymentGatewayRequest();
        req.setPaymentMethod("CARD");
        req.setCardNumber("4111111111110000");
        req.setCardExpiry("12/26");
        req.setCardCvv("123");
        req.setCardHolderName("Alice Smith");

        PaymentGatewayResponse resp = gateway.processPayment(req, 149.0);

        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getFailureReason()).contains("declined by bank");
    }

    @Test
    @DisplayName("CARD - fail: card ending in 1111 → insufficient funds")
    void card_insufficientFunds_failure() {
        PaymentGatewayRequest req = new PaymentGatewayRequest();
        req.setPaymentMethod("CARD");
        req.setCardNumber("4111111111111111");
        req.setCardExpiry("12/26");
        req.setCardCvv("123");
        req.setCardHolderName("Alice Smith");

        PaymentGatewayResponse resp = gateway.processPayment(req, 149.0);

        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getFailureReason()).contains("Insufficient funds");
    }

    @Test
    @DisplayName("CARD - fail: card ending in 2222 → expired card")
    void card_expiredCard_failure() {
        PaymentGatewayRequest req = new PaymentGatewayRequest();
        req.setPaymentMethod("CARD");
        req.setCardNumber("4111111111112222");
        req.setCardExpiry("12/26");
        req.setCardCvv("123");
        req.setCardHolderName("Alice Smith");

        PaymentGatewayResponse resp = gateway.processPayment(req, 149.0);

        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getFailureReason()).contains("expired");
    }

    @Test
    @DisplayName("CARD - fail: card number not 16 digits → invalid")
    void card_invalidCardNumber_failure() {
        PaymentGatewayRequest req = new PaymentGatewayRequest();
        req.setPaymentMethod("CARD");
        req.setCardNumber("1234");
        req.setCardExpiry("12/26");
        req.setCardCvv("123");
        req.setCardHolderName("Alice");

        PaymentGatewayResponse resp = gateway.processPayment(req, 149.0);

        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getFailureReason()).contains("Invalid card number");
    }

    @Test
    @DisplayName("CARD - fail: invalid expiry format → failure")
    void card_invalidExpiry_failure() {
        PaymentGatewayRequest req = new PaymentGatewayRequest();
        req.setPaymentMethod("CARD");
        req.setCardNumber("4111111111119999");
        req.setCardExpiry("13/26"); // month 13 is invalid
        req.setCardCvv("123");
        req.setCardHolderName("Alice");

        PaymentGatewayResponse resp = gateway.processPayment(req, 149.0);

        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getFailureReason()).contains("Invalid card expiry");
    }

    @Test
    @DisplayName("CARD - fail: CVV not 3 digits → failure")
    void card_invalidCvv_failure() {
        PaymentGatewayRequest req = new PaymentGatewayRequest();
        req.setPaymentMethod("CARD");
        req.setCardNumber("4111111111119999");
        req.setCardExpiry("12/26");
        req.setCardCvv("12"); // only 2 digits
        req.setCardHolderName("Alice");

        PaymentGatewayResponse resp = gateway.processPayment(req, 149.0);

        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getFailureReason()).contains("Invalid CVV");
    }

    // ── UPI ───────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("UPI - success: valid UPI id succeeds")
    void upi_valid_success() {
        PaymentGatewayRequest req = new PaymentGatewayRequest();
        req.setPaymentMethod("UPI");
        req.setUpiId("alice@okicici");

        PaymentGatewayResponse resp = gateway.processPayment(req, 149.0);

        assertThat(resp.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("UPI - fail: UPI id containing 'fail' is declined")
    void upi_failKeyword_failure() {
        PaymentGatewayRequest req = new PaymentGatewayRequest();
        req.setPaymentMethod("UPI");
        req.setUpiId("failuser@upi");

        PaymentGatewayResponse resp = gateway.processPayment(req, 149.0);

        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getFailureReason()).contains("declined");
    }

    @Test
    @DisplayName("UPI - fail: UPI id containing 'timeout' simulates timeout")
    void upi_timeoutKeyword_failure() {
        PaymentGatewayRequest req = new PaymentGatewayRequest();
        req.setPaymentMethod("UPI");
        req.setUpiId("timeout@upi");

        PaymentGatewayResponse resp = gateway.processPayment(req, 149.0);

        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getFailureReason()).contains("timed out");
    }

    @Test
    @DisplayName("UPI - fail: invalid UPI format (missing @) → failure")
    void upi_invalidFormat_failure() {
        PaymentGatewayRequest req = new PaymentGatewayRequest();
        req.setPaymentMethod("UPI");
        req.setUpiId("invalidemail");

        PaymentGatewayResponse resp = gateway.processPayment(req, 149.0);

        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getFailureReason()).contains("Invalid UPI ID");
    }

    // ── NETBANKING ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("NETBANKING - success: valid bank and account")
    void netbanking_valid_success() {
        PaymentGatewayRequest req = new PaymentGatewayRequest();
        req.setPaymentMethod("NETBANKING");
        req.setBankCode("HDFC");
        req.setAccountNumber("123456789012");

        PaymentGatewayResponse resp = gateway.processPayment(req, 149.0);

        assertThat(resp.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("NETBANKING - fail: invalid bank code → failure")
    void netbanking_invalidBankCode_failure() {
        PaymentGatewayRequest req = new PaymentGatewayRequest();
        req.setPaymentMethod("NETBANKING");
        req.setBankCode("UNKNOWN");
        req.setAccountNumber("123456789012");

        PaymentGatewayResponse resp = gateway.processPayment(req, 149.0);

        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getFailureReason()).contains("Invalid bank code");
    }

    @Test
    @DisplayName("NETBANKING - fail: BOB is under maintenance")
    void netbanking_bobMaintenance_failure() {
        PaymentGatewayRequest req = new PaymentGatewayRequest();
        req.setPaymentMethod("NETBANKING");
        req.setBankCode("BOB");
        req.setAccountNumber("123456789012");

        PaymentGatewayResponse resp = gateway.processPayment(req, 149.0);

        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getFailureReason()).contains("maintenance");
    }

    @Test
    @DisplayName("NETBANKING - fail: account number too short (< 9 digits)")
    void netbanking_invalidAccountNumber_failure() {
        PaymentGatewayRequest req = new PaymentGatewayRequest();
        req.setPaymentMethod("NETBANKING");
        req.setBankCode("SBI");
        req.setAccountNumber("1234"); // too short

        PaymentGatewayResponse resp = gateway.processPayment(req, 149.0);

        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getFailureReason()).contains("Invalid account number");
    }

    // ── WALLET ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("WALLET - success: low amount with valid wallet")
    void wallet_lowAmount_success() {
        PaymentGatewayRequest req = new PaymentGatewayRequest();
        req.setPaymentMethod("WALLET");
        req.setWalletType("PAYTM");
        req.setWalletMobile("9876543210");

        PaymentGatewayResponse resp = gateway.processPayment(req, 149.0);

        assertThat(resp.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("WALLET - fail: amount > 500 → insufficient wallet balance")
    void wallet_highAmount_failure() {
        PaymentGatewayRequest req = new PaymentGatewayRequest();
        req.setPaymentMethod("WALLET");
        req.setWalletType("PAYTM");
        req.setWalletMobile("9876543210");

        PaymentGatewayResponse resp = gateway.processPayment(req, 699.0);

        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getFailureReason()).contains("Insufficient wallet balance");
    }

    @Test
    @DisplayName("WALLET - fail: invalid wallet type → failure")
    void wallet_invalidType_failure() {
        PaymentGatewayRequest req = new PaymentGatewayRequest();
        req.setPaymentMethod("WALLET");
        req.setWalletType("FAKETWALLET");
        req.setWalletMobile("9876543210");

        PaymentGatewayResponse resp = gateway.processPayment(req, 149.0);

        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getFailureReason()).contains("Invalid wallet type");
    }

    // ── Invalid payment method ────────────────────────────────────────────────

    @Test
    @DisplayName("processPayment() - unknown method returns FAILED response")
    void unknownMethod_returnsFailure() {
        PaymentGatewayRequest req = new PaymentGatewayRequest();
        req.setPaymentMethod("CRYPTO");

        PaymentGatewayResponse resp = gateway.processPayment(req, 149.0);

        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getFailureReason()).contains("Invalid payment method");
    }
}
