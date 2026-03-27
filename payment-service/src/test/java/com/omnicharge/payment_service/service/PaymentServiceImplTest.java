package com.omnicharge.payment_service.service;

import com.omnicharge.payment_service.dto.*;
import com.omnicharge.payment_service.entity.Transaction;
import com.omnicharge.payment_service.entity.TransactionStatus;
import com.omnicharge.payment_service.feign.RechargeServiceFeignClient;
import com.omnicharge.payment_service.messaging.PaymentResultPublisher;
import com.omnicharge.payment_service.repository.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentServiceImpl Unit Tests")
class PaymentServiceImplTest {

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private RechargeServiceFeignClient rechargeServiceFeignClient;

    @Mock
    private PaymentResultPublisher paymentResultPublisher;

    @Mock
    private DummyPaymentGatewayService paymentGatewayService;

    // ── helpers ───────────────────────────────────────────────────────────────

    private RechargeEventMessage buildEvent(Long rechargeId) {
        RechargeEventMessage event = new RechargeEventMessage();
        event.setRechargeId(rechargeId);
        event.setUsername("alice");
        event.setMobileNumber("9876543210");
        event.setOperatorName("Airtel");
        event.setPlanName("Basic 149");
        event.setAmount(149.0);
        event.setValidity("28 days");
        event.setDataInfo("1.5 GB/day");
        event.setStatus("PENDING");
        event.setCreatedAt(LocalDateTime.now());
        return event;
    }

    private Transaction buildPendingTransaction(Long rechargeId) {
        Transaction t = new Transaction();
        t.setId(1L);
        t.setTransactionId("TXN123456789");
        t.setRechargeId(rechargeId);
        t.setUsername("alice");
        t.setMobileNumber("9876543210");
        t.setOperatorName("Airtel");
        t.setPlanName("Basic 149");
        t.setAmount(149.0);
        t.setValidity("28 days");
        t.setDataInfo("1.5 GB/day");
        t.setStatus(TransactionStatus.PENDING);
        t.setCreatedAt(LocalDateTime.now());
        t.setUpdatedAt(LocalDateTime.now());
        return t;
    }

    // ── processPayment() ──────────────────────────────────────────────────────

    @Test
    @DisplayName("processPayment() - success: PENDING transaction created for new recharge event")
    void processPayment_createsNewPendingTransaction() {
        RechargeEventMessage event = buildEvent(100L);
        when(transactionRepository.findByRechargeId(100L)).thenReturn(Optional.empty());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            t.setId(1L);
            t.setCreatedAt(LocalDateTime.now());
            t.setUpdatedAt(LocalDateTime.now());
            return t;
        });

        TransactionDto dto = paymentService.processPayment(event);

        assertThat(dto.getStatus()).isEqualTo("PENDING");
        assertThat(dto.getUsername()).isEqualTo("alice");
        assertThat(dto.getRechargeId()).isEqualTo(100L);
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    @DisplayName("processPayment() - duplicate event: returns existing transaction without creating new one")
    void processPayment_duplicateEvent_returnsExisting() {
        RechargeEventMessage event = buildEvent(100L);
        Transaction existing = buildPendingTransaction(100L);
        when(transactionRepository.findByRechargeId(100L)).thenReturn(Optional.of(existing));

        TransactionDto dto = paymentService.processPayment(event);

        assertThat(dto.getTransactionId()).isEqualTo("TXN123456789");
        // save should NOT be called again for a duplicate
        verify(transactionRepository, never()).save(any());
    }

    // ── makePayment() ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("makePayment() - success: payment succeeds, recharge updated and notification sent")
    void makePayment_success() {
        Transaction pending = buildPendingTransaction(100L);
        PaymentGatewayRequest req = new PaymentGatewayRequest();
        req.setRechargeId(100L);
        req.setPaymentMethod("UPI");
        req.setUpiId("alice@upi");

        PaymentGatewayResponse gatewayResp = new PaymentGatewayResponse();
        gatewayResp.setSuccess(true);
        gatewayResp.setStatus("SUCCESS");
        gatewayResp.setPaymentReference("REF-UPI-alice@upi-ABCD1234");

        when(transactionRepository.findByRechargeId(100L)).thenReturn(Optional.of(pending));
        when(paymentGatewayService.processPayment(any(), eq(149.0))).thenReturn(gatewayResp);
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TransactionDto dto = paymentService.makePayment("alice", req);

        assertThat(dto.getStatus()).isEqualTo("SUCCESS");
        assertThat(dto.getPaymentReference()).isEqualTo("REF-UPI-alice@upi-ABCD1234");
        verify(rechargeServiceFeignClient).updateRechargeStatus(eq(100L), any());
        verify(paymentResultPublisher).publishPaymentResult(any());
    }

    @Test
    @DisplayName("makePayment() - fail: gateway rejects payment, status set to FAILED")
    void makePayment_gatewayFail_statusFailed() {
        Transaction pending = buildPendingTransaction(100L);
        PaymentGatewayRequest req = new PaymentGatewayRequest();
        req.setRechargeId(100L);
        req.setPaymentMethod("UPI");
        req.setUpiId("fail@upi");

        PaymentGatewayResponse gatewayResp = new PaymentGatewayResponse();
        gatewayResp.setSuccess(false);
        gatewayResp.setStatus("FAILED");
        gatewayResp.setFailureReason("UPI transaction declined by bank");

        when(transactionRepository.findByRechargeId(100L)).thenReturn(Optional.of(pending));
        when(paymentGatewayService.processPayment(any(), eq(149.0))).thenReturn(gatewayResp);
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TransactionDto dto = paymentService.makePayment("alice", req);

        assertThat(dto.getStatus()).isEqualTo("FAILED");
        assertThat(dto.getFailureReason()).isEqualTo("UPI transaction declined by bank");
    }

    @Test
    @DisplayName("makePayment() - fail: no pending transaction found throws exception")
    void makePayment_noPendingTransaction_throwsException() {
        PaymentGatewayRequest req = new PaymentGatewayRequest();
        req.setRechargeId(999L);
        req.setPaymentMethod("UPI");

        when(transactionRepository.findByRechargeId(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.makePayment("alice", req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No pending transaction found");
    }

    @Test
    @DisplayName("makePayment() - fail: user trying to pay for another's transaction")
    void makePayment_wrongUser_throwsException() {
        Transaction pending = buildPendingTransaction(100L); // belongs to "alice"
        PaymentGatewayRequest req = new PaymentGatewayRequest();
        req.setRechargeId(100L);
        req.setPaymentMethod("UPI");

        when(transactionRepository.findByRechargeId(100L)).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> paymentService.makePayment("bob", req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    @DisplayName("makePayment() - fail: already processed transaction cannot be paid again")
    void makePayment_alreadyProcessed_throwsException() {
        Transaction done = buildPendingTransaction(100L);
        done.setStatus(TransactionStatus.SUCCESS); // already done

        PaymentGatewayRequest req = new PaymentGatewayRequest();
        req.setRechargeId(100L);
        req.setPaymentMethod("UPI");

        when(transactionRepository.findByRechargeId(100L)).thenReturn(Optional.of(done));

        assertThatThrownBy(() -> paymentService.makePayment("alice", req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Transaction is already SUCCESS");
    }

    // ── paymentFallback() ─────────────────────────────────────────────────────

    @Test
    @DisplayName("paymentFallback() - circuit breaker open: creates FAILED transaction")
    void paymentFallback_createsFailed() {
        RechargeEventMessage event = buildEvent(200L);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            t.setId(2L);
            t.setCreatedAt(LocalDateTime.now());
            t.setUpdatedAt(LocalDateTime.now());
            return t;
        });

        TransactionDto dto = paymentService.paymentFallback(event, new RuntimeException("Circuit open"));

        assertThat(dto.getStatus()).isEqualTo("FAILED");
        assertThat(dto.getFailureReason()).contains("temporarily unavailable");
        verify(rechargeServiceFeignClient).updateRechargeStatus(eq(200L), any());
        verify(paymentResultPublisher).publishPaymentResult(any());
    }

    // ── getMyTransactions() ───────────────────────────────────────────────────

    @Test
    @DisplayName("getMyTransactions() - returns all transactions for a user")
    void getMyTransactions_returnsList() {
        when(transactionRepository.findByUsernameOrderByCreatedAtDesc("alice"))
                .thenReturn(List.of(buildPendingTransaction(100L), buildPendingTransaction(101L)));

        List<TransactionDto> result = paymentService.getMyTransactions("alice");

        assertThat(result).hasSize(2);
    }

    // ── getByTransactionId() ──────────────────────────────────────────────────

    @Test
    @DisplayName("getByTransactionId() - found: returns DTO")
    void getByTransactionId_found() {
        Transaction t = buildPendingTransaction(100L);
        when(transactionRepository.findByTransactionId("TXN123456789")).thenReturn(Optional.of(t));

        TransactionDto dto = paymentService.getByTransactionId("TXN123456789");

        assertThat(dto.getTransactionId()).isEqualTo("TXN123456789");
    }

    @Test
    @DisplayName("getByTransactionId() - not found: throws exception")
    void getByTransactionId_notFound_throwsException() {
        when(transactionRepository.findByTransactionId("INVALID")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getByTransactionId("INVALID"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Transaction not found");
    }

    // ── getTransactionsByStatus() ─────────────────────────────────────────────

    @Test
    @DisplayName("getTransactionsByStatus() - valid status returns list")
    void getTransactionsByStatus_valid() {
        when(transactionRepository.findByStatus(TransactionStatus.SUCCESS))
                .thenReturn(List.of());

        List<TransactionDto> result = paymentService.getTransactionsByStatus("SUCCESS");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getTransactionsByStatus() - invalid status throws exception")
    void getTransactionsByStatus_invalid_throwsException() {
        assertThatThrownBy(() -> paymentService.getTransactionsByStatus("GARBAGE"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid status");
    }
}
