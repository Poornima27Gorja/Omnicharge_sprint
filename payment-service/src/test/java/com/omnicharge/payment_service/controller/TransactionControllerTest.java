package com.omnicharge.payment_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnicharge.payment_service.dto.PaymentGatewayRequest;
import com.omnicharge.payment_service.dto.TransactionDto;
import com.omnicharge.payment_service.exception.GlobalExceptionHandler;
import com.omnicharge.payment_service.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TransactionController standalone MockMvc tests.
 *
 * FIX: GlobalExceptionHandler registered via .setControllerAdvice()
 *      so RuntimeException → 400 BAD_REQUEST (not 500 INTERNAL_SERVER_ERROR).
 *      This was causing 4 test errors in the original run:
 *        - "GET  /api/transactions/status/INVALID - invalid status returns 400"
 *        - "GET  /api/transactions/txn/INVALID - not found returns 400"
 *        - "POST /api/transactions/pay - no pending transaction returns 400"
 *        - "POST /api/transactions/pay - wrong user access denied returns 400"
 *
 * Covers:
 *  POST /api/transactions/pay
 *  GET  /api/transactions/my-transactions
 *  GET  /api/transactions/txn/{transactionId}
 *  GET  /api/transactions/recharge/{rechargeId}
 *  GET  /api/transactions/all
 *  GET  /api/transactions/status/{status}
 *  GET  /api/transactions/mobile/{mobileNumber}
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionController Web Layer Tests")
class TransactionControllerTest {

    @InjectMocks
    private TransactionController transactionController;

    @Mock
    private PaymentService paymentService;

    @Mock
    private Authentication authentication;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        // ── KEY FIX ──────────────────────────────────────────────────────────
        // Register GlobalExceptionHandler so RuntimeException → 400 not 500.
        // ─────────────────────────────────────────────────────────────────────
        mockMvc = MockMvcBuilders
                .standaloneSetup(transactionController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private TransactionDto buildDto(String txnId, Long rechargeId, String status) {
        TransactionDto dto = new TransactionDto();
        dto.setId(1L);
        dto.setTransactionId(txnId);
        dto.setRechargeId(rechargeId);
        dto.setUsername("alice");
        dto.setMobileNumber("9876543210");
        dto.setOperatorName("Airtel");
        dto.setPlanName("Basic 149");
        dto.setAmount(149.0);
        dto.setValidity("28 days");
        dto.setDataInfo("1.5 GB/day");
        dto.setStatus(status);
        dto.setPaymentMethod("UPI");
        dto.setCreatedAt(LocalDateTime.now());
        dto.setUpdatedAt(LocalDateTime.now());
        return dto;
    }

    // ── POST /api/transactions/pay ────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/transactions/pay - 200 OK on successful payment")
    void makePayment_returns200() throws Exception {
        PaymentGatewayRequest req = new PaymentGatewayRequest();
        req.setRechargeId(100L);
        req.setPaymentMethod("UPI");
        req.setUpiId("alice@okicici");

        TransactionDto dto = buildDto("TXN123456789ABC", 100L, "SUCCESS");
        dto.setPaymentReference("REF-UPI-alice@okicici-ABCD1234");

        when(authentication.getName()).thenReturn("alice");
        when(paymentService.makePayment(eq("alice"), any(PaymentGatewayRequest.class))).thenReturn(dto);

        mockMvc.perform(post("/api/transactions/pay")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.transactionId").value("TXN123456789ABC"));
    }

    @Test
    @DisplayName("POST /api/transactions/pay - no pending transaction returns 400")
    void makePayment_noPending_returns400() throws Exception {
        PaymentGatewayRequest req = new PaymentGatewayRequest();
        req.setRechargeId(999L);
        req.setPaymentMethod("UPI");

        when(authentication.getName()).thenReturn("alice");
        // Service throws RuntimeException.
        // GlobalExceptionHandler converts to 400 BAD_REQUEST.
        when(paymentService.makePayment(eq("alice"), any()))
                .thenThrow(new RuntimeException(
                        "No pending transaction found for recharge id: 999. Please initiate recharge first"));

        mockMvc.perform(post("/api/transactions/pay")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/transactions/pay - wrong user access denied returns 400")
    void makePayment_wrongUser_returns400() throws Exception {
        PaymentGatewayRequest req = new PaymentGatewayRequest();
        req.setRechargeId(100L);
        req.setPaymentMethod("UPI");

        when(authentication.getName()).thenReturn("bob");
        when(paymentService.makePayment(eq("bob"), any()))
                .thenThrow(new RuntimeException("Access denied. You can only pay for your own recharge"));

        mockMvc.perform(post("/api/transactions/pay")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ── GET /api/transactions/my-transactions ─────────────────────────────────

    @Test
    @DisplayName("GET /api/transactions/my-transactions - 200 OK returns user's transactions")
    void getMyTransactions_returns200() throws Exception {
        when(authentication.getName()).thenReturn("alice");
        when(paymentService.getMyTransactions("alice")).thenReturn(List.of(
                buildDto("TXN001", 100L, "SUCCESS"),
                buildDto("TXN002", 101L, "PENDING")
        ));

        mockMvc.perform(get("/api/transactions/my-transactions").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ── GET /api/transactions/txn/{transactionId} ─────────────────────────────

    @Test
    @DisplayName("GET /api/transactions/txn/TXN001 - 200 OK returns transaction")
    void getByTransactionId_returns200() throws Exception {
        TransactionDto dto = buildDto("TXN001", 100L, "SUCCESS");
        when(paymentService.getByTransactionId("TXN001")).thenReturn(dto);

        mockMvc.perform(get("/api/transactions/txn/TXN001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value("TXN001"));
    }

    @Test
    @DisplayName("GET /api/transactions/txn/INVALID - not found returns 400")
    void getByTransactionId_notFound_returns400() throws Exception {
        when(paymentService.getByTransactionId("INVALID"))
                .thenThrow(new RuntimeException("Transaction not found: INVALID"));

        mockMvc.perform(get("/api/transactions/txn/INVALID"))
                .andExpect(status().isBadRequest());
    }

    // ── GET /api/transactions/recharge/{rechargeId} ───────────────────────────

    @Test
    @DisplayName("GET /api/transactions/recharge/100 - 200 OK returns transaction")
    void getByRechargeId_returns200() throws Exception {
        TransactionDto dto = buildDto("TXN001", 100L, "SUCCESS");
        when(paymentService.getByRechargeId(100L)).thenReturn(dto);

        mockMvc.perform(get("/api/transactions/recharge/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rechargeId").value(100));
    }

    // ── GET /api/transactions/all ─────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/transactions/all - 200 OK returns all transactions")
    void getAllTransactions_returns200() throws Exception {
        when(paymentService.getAllTransactions()).thenReturn(List.of(
                buildDto("TXN001", 100L, "SUCCESS"),
                buildDto("TXN002", 101L, "FAILED")
        ));

        mockMvc.perform(get("/api/transactions/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ── GET /api/transactions/status/{status} ─────────────────────────────────

    @Test
    @DisplayName("GET /api/transactions/status/SUCCESS - 200 OK returns list")
    void getByStatus_valid_returns200() throws Exception {
        when(paymentService.getTransactionsByStatus("SUCCESS")).thenReturn(List.of(
                buildDto("TXN001", 100L, "SUCCESS")
        ));

        mockMvc.perform(get("/api/transactions/status/SUCCESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("SUCCESS"));
    }

    @Test
    @DisplayName("GET /api/transactions/status/INVALID - invalid status returns 400")
    void getByStatus_invalid_returns400() throws Exception {
        // Service throws RuntimeException for unknown status values.
        // GlobalExceptionHandler converts to 400 BAD_REQUEST.
        when(paymentService.getTransactionsByStatus("INVALID"))
                .thenThrow(new RuntimeException("Invalid status. Valid: PENDING, SUCCESS, FAILED"));

        mockMvc.perform(get("/api/transactions/status/INVALID"))
                .andExpect(status().isBadRequest());
    }

    // ── GET /api/transactions/mobile/{mobileNumber} ───────────────────────────

    @Test
    @DisplayName("GET /api/transactions/mobile/9876543210 - 200 OK returns list")
    void getByMobile_returns200() throws Exception {
        when(paymentService.getTransactionsByMobile("9876543210")).thenReturn(List.of(
                buildDto("TXN001", 100L, "SUCCESS")
        ));

        mockMvc.perform(get("/api/transactions/mobile/9876543210"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].mobileNumber").value("9876543210"));
    }
}