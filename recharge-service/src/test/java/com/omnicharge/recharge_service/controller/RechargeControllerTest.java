package com.omnicharge.recharge_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnicharge.recharge_service.dto.InitiateRechargeRequest;
import com.omnicharge.recharge_service.dto.RechargeRequestDto;
import com.omnicharge.recharge_service.dto.RechargeStatusUpdateRequest;
import com.omnicharge.recharge_service.exception.GlobalExceptionHandler;
import com.omnicharge.recharge_service.service.RechargeService;
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

import java.util.Collection;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * RechargeController standalone MockMvc tests.
 *
 * FIX: GlobalExceptionHandler registered via .setControllerAdvice()
 *      so RuntimeException → 400 BAD_REQUEST (not 500 INTERNAL_SERVER_ERROR).
 *      This was causing 2 test errors in the original run:
 *        - "POST /api/recharge/initiate - invalid mobile returns 400"
 *        - "GET  /api/recharge/status/INVALID - invalid status returns 400"
 *
 * Covers:
 *  POST /api/recharge/initiate
 *  GET  /api/recharge/my-history
 *  GET  /api/recharge/all
 *  GET  /api/recharge/status/{status}
 *  GET  /api/recharge/mobile/{mobile}
 *  GET  /api/recharge/{id}
 *  PUT  /api/recharge/update-status/{rechargeId}
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RechargeController Web Layer Tests")
class RechargeControllerTest {

    @InjectMocks
    private RechargeController rechargeController;

    @Mock
    private RechargeService rechargeService;

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
                .standaloneSetup(rechargeController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private RechargeRequestDto buildDto(Long id, String username, String status) {
        RechargeRequestDto dto = new RechargeRequestDto();
        dto.setId(id);
        dto.setUsername(username);
        dto.setMobileNumber("9876543210");
        dto.setOperatorId(1L);
        dto.setOperatorName("Airtel");
        dto.setPlanId(10L);
        dto.setPlanName("Basic 149");
        dto.setAmount(149.0);
        dto.setValidity("28 days");
        dto.setDataInfo("1.5 GB/day");
        dto.setStatus(status);
        return dto;
    }

    // ── POST /api/recharge/initiate ───────────────────────────────────────────

    @Test
    @DisplayName("POST /api/recharge/initiate - 201 Created on success")
    void initiateRecharge_returns201() throws Exception {
        RechargeRequestDto dto = buildDto(100L, "alice", "PENDING");
        InitiateRechargeRequest req = new InitiateRechargeRequest();
        req.setMobileNumber("9876543210");
        req.setOperatorId(1L);
        req.setPlanId(10L);

        when(authentication.getName()).thenReturn("alice");
        when(rechargeService.initiateRecharge(eq("alice"), any(), any(InitiateRechargeRequest.class)))
                .thenReturn(dto);

        mockMvc.perform(post("/api/recharge/initiate")
                        .principal(authentication)
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test
    @DisplayName("POST /api/recharge/initiate - invalid mobile returns 400")
    void initiateRecharge_invalidMobile_returns400() throws Exception {
        InitiateRechargeRequest req = new InitiateRechargeRequest();
        req.setMobileNumber("12345");
        req.setOperatorId(1L);
        req.setPlanId(10L);

        when(authentication.getName()).thenReturn("alice");
        // Service throws RuntimeException for invalid mobile.
        // GlobalExceptionHandler converts this to 400 BAD_REQUEST.
        when(rechargeService.initiateRecharge(eq("alice"), any(), any()))
                .thenThrow(new RuntimeException("Invalid mobile number. Must be a valid 10 digit Indian mobile number"));

        mockMvc.perform(post("/api/recharge/initiate")
                        .principal(authentication)
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ── GET /api/recharge/my-history ──────────────────────────────────────────

    @Test
    @DisplayName("GET /api/recharge/my-history - 200 OK returns user's recharge list")
    void getMyHistory_returns200() throws Exception {
        when(authentication.getName()).thenReturn("alice");
        when(rechargeService.getMyRechargeHistory("alice")).thenReturn(List.of(
                buildDto(1L, "alice", "SUCCESS"),
                buildDto(2L, "alice", "PENDING")
        ));

        mockMvc.perform(get("/api/recharge/my-history").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ── GET /api/recharge/all ─────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/recharge/all - 200 OK returns all recharges")
    void getAllRecharges_returns200() throws Exception {
        when(rechargeService.getAllRecharges()).thenReturn(List.of(
                buildDto(1L, "alice", "SUCCESS"),
                buildDto(2L, "bob", "FAILED")
        ));

        mockMvc.perform(get("/api/recharge/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ── GET /api/recharge/status/{status} ─────────────────────────────────────

    @Test
    @DisplayName("GET /api/recharge/status/PENDING - 200 OK returns filtered list")
    void getByStatus_returns200() throws Exception {
        when(rechargeService.getRechargesByStatus("PENDING")).thenReturn(List.of(
                buildDto(1L, "alice", "PENDING")
        ));

        mockMvc.perform(get("/api/recharge/status/PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    @DisplayName("GET /api/recharge/status/INVALID - invalid status returns 400")
    void getByStatus_invalidStatus_returns400() throws Exception {
        // Service throws RuntimeException for unknown status values.
        // GlobalExceptionHandler converts this to 400 BAD_REQUEST.
        when(rechargeService.getRechargesByStatus("INVALID"))
                .thenThrow(new RuntimeException("Invalid status. Valid values: PENDING, SUCCESS, FAILED"));

        mockMvc.perform(get("/api/recharge/status/INVALID"))
                .andExpect(status().isBadRequest());
    }

    // ── GET /api/recharge/mobile/{mobileNumber} ───────────────────────────────

    @Test
    @DisplayName("GET /api/recharge/mobile/9876543210 - 200 OK returns list")
    void getByMobile_returns200() throws Exception {
        when(rechargeService.getRechargesByMobile("9876543210")).thenReturn(List.of(
                buildDto(1L, "alice", "SUCCESS")
        ));

        mockMvc.perform(get("/api/recharge/mobile/9876543210"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].mobileNumber").value("9876543210"));
    }

    // ── GET /api/recharge/{id} ────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/recharge/1 - 200 OK user can access own recharge")
    void getRechargeById_userOwner_returns200() throws Exception {
        RechargeRequestDto dto = buildDto(1L, "alice", "SUCCESS");

        when(authentication.getName()).thenReturn("alice");
        //noinspection unchecked
        when(authentication.getAuthorities()).thenReturn((Collection) List.of());
        when(rechargeService.getRechargeById(1L, "alice", false)).thenReturn(dto);

        mockMvc.perform(get("/api/recharge/1").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    // ── PUT /api/recharge/update-status/{rechargeId} ──────────────────────────

    @Test
    @DisplayName("PUT /api/recharge/update-status/100 - 200 OK updates status")
    void updateRechargeStatus_returns200() throws Exception {
        RechargeRequestDto updated = buildDto(100L, "alice", "SUCCESS");

        RechargeStatusUpdateRequest req = new RechargeStatusUpdateRequest();
        req.setStatus("SUCCESS");

        when(rechargeService.updateRechargeStatus(eq(100L), any(RechargeStatusUpdateRequest.class)))
                .thenReturn(updated);

        mockMvc.perform(put("/api/recharge/update-status/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }
}