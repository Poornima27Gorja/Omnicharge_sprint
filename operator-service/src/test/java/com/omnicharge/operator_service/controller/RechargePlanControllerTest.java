package com.omnicharge.operator_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnicharge.operator_service.dto.RechargePlanDto;
import com.omnicharge.operator_service.dto.RechargePlanRequest;
import com.omnicharge.operator_service.dto.StatusUpdateRequest;
import com.omnicharge.operator_service.exception.GlobalExceptionHandler;
import com.omnicharge.operator_service.service.RechargePlanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * RechargePlanController standalone MockMvc tests.
 *
 * FIX: GlobalExceptionHandler is registered via .setControllerAdvice()
 *      so RuntimeException → 400 BAD_REQUEST (not 500).
 *
 * Covers:
 *  POST   /api/plans                        — addPlan
 *  PUT    /api/plans/{id}                   — updatePlan
 *  PATCH  /api/plans/{id}/status
 *  DELETE /api/plans/{id}
 *  GET    /api/plans/{id}
 *  GET    /api/plans
 *  GET    /api/plans/operator/{operatorId}
 *  GET    /api/plans/operator/{operatorId}/active
 *  GET    /api/plans/category/{category}
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RechargePlanController Web Layer Tests")
class RechargePlanControllerTest {

    @InjectMocks
    private RechargePlanController rechargePlanController;

    @Mock
    private RechargePlanService rechargePlanService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        // ── KEY FIX ──────────────────────────────────────────────────────────
        // Register GlobalExceptionHandler so RuntimeException → 400 not 500.
        // ─────────────────────────────────────────────────────────────────────
        mockMvc = MockMvcBuilders
                .standaloneSetup(rechargePlanController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private RechargePlanDto buildDto(Long id, String planName, String status, Long operatorId) {
        RechargePlanDto dto = new RechargePlanDto();
        dto.setId(id);
        dto.setPlanName(planName);
        dto.setPrice(149.0);
        dto.setValidity("28 days");
        dto.setData("1.5 GB/day");
        dto.setCalls("Unlimited");
        dto.setSms("100/day");
        dto.setCategory("COMBO");
        dto.setStatus(status);
        dto.setOperatorId(operatorId);
        dto.setOperatorName("Airtel");
        return dto;
    }

    private RechargePlanRequest buildRequest(Long operatorId) {
        RechargePlanRequest req = new RechargePlanRequest();
        req.setOperatorId(operatorId);
        req.setPlanName("Basic 149");
        req.setPrice(149.0);
        req.setValidity("28 days");
        req.setData("1.5 GB/day");
        req.setCalls("Unlimited");
        req.setSms("100/day");
        req.setDescription("Popular plan");
        req.setCategory("COMBO");
        req.setStatus("ACTIVE");
        return req;
    }

    // ── POST /api/plans ───────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/plans - 201 Created on success")
    void addPlan_returns201() throws Exception {
        RechargePlanDto created = buildDto(10L, "Basic 149", "ACTIVE", 1L);
        when(rechargePlanService.addPlan(any(RechargePlanRequest.class))).thenReturn(created);

        mockMvc.perform(post("/api/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest(1L))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.planName").value("Basic 149"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("POST /api/plans - operator not found returns 400")
    void addPlan_operatorNotFound_returns400() throws Exception {
        when(rechargePlanService.addPlan(any()))
                .thenThrow(new RuntimeException("Operator not found with id: 99"));

        mockMvc.perform(post("/api/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest(99L))))
                .andExpect(status().isBadRequest());
    }

    // ── PUT /api/plans/{id} ───────────────────────────────────────────────────

    @Test
    @DisplayName("PUT /api/plans/10 - 200 OK on update")
    void updatePlan_returns200() throws Exception {
        RechargePlanDto updated = buildDto(10L, "Super 199", "ACTIVE", 1L);
        updated.setPrice(199.0);
        when(rechargePlanService.updatePlan(eq(10L), any(RechargePlanRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/api/plans/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest(1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planName").value("Super 199"));
    }

    // ── PATCH /api/plans/{id}/status ──────────────────────────────────────────

    @Test
    @DisplayName("PATCH /api/plans/10/status - 200 OK on toggle")
    void updatePlanStatus_returns200() throws Exception {
        RechargePlanDto patched = buildDto(10L, "Basic 149", "INACTIVE", 1L);
        when(rechargePlanService.updatePlanStatus(10L, "INACTIVE")).thenReturn(patched);

        StatusUpdateRequest req = new StatusUpdateRequest();
        req.setStatus("INACTIVE");

        mockMvc.perform(patch("/api/plans/10/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    // ── DELETE /api/plans/{id} ────────────────────────────────────────────────

    @Test
    @DisplayName("DELETE /api/plans/10 - 200 OK on deletion")
    void deletePlan_returns200() throws Exception {
        doNothing().when(rechargePlanService).deletePlan(10L);

        mockMvc.perform(delete("/api/plans/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("Plan deleted successfully"));
    }

    @Test
    @DisplayName("DELETE /api/plans/99 - not found returns 400")
    void deletePlan_notFound_returns400() throws Exception {
        doThrow(new RuntimeException("Plan not found with id: 99"))
                .when(rechargePlanService).deletePlan(99L);

        mockMvc.perform(delete("/api/plans/99"))
                .andExpect(status().isBadRequest());
    }

    // ── GET /api/plans/{id} ───────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/plans/10 - 200 OK with plan DTO")
    void getPlanById_returns200() throws Exception {
        RechargePlanDto dto = buildDto(10L, "Basic 149", "ACTIVE", 1L);
        when(rechargePlanService.getPlanById(10L)).thenReturn(dto);

        mockMvc.perform(get("/api/plans/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    @DisplayName("GET /api/plans/99 - not found returns 400")
    void getPlanById_notFound_returns400() throws Exception {
        when(rechargePlanService.getPlanById(99L))
                .thenThrow(new RuntimeException("Plan not found with id: 99"));

        mockMvc.perform(get("/api/plans/99"))
                .andExpect(status().isBadRequest());
    }

    // ── GET /api/plans ────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/plans - 200 OK returns all plans")
    void getAllPlans_returns200() throws Exception {
        when(rechargePlanService.getAllPlans()).thenReturn(List.of(
                buildDto(10L, "Basic 149", "ACTIVE", 1L),
                buildDto(11L, "Premium 299", "ACTIVE", 1L)
        ));

        mockMvc.perform(get("/api/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ── GET /api/plans/operator/{operatorId} ──────────────────────────────────

    @Test
    @DisplayName("GET /api/plans/operator/1 - 200 OK returns all plans for operator")
    void getPlansByOperator_returns200() throws Exception {
        when(rechargePlanService.getPlansByOperator(1L)).thenReturn(List.of(
                buildDto(10L, "Basic 149", "ACTIVE", 1L)
        ));

        mockMvc.perform(get("/api/plans/operator/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].operatorId").value(1));
    }

    // ── GET /api/plans/operator/{operatorId}/active ───────────────────────────

    @Test
    @DisplayName("GET /api/plans/operator/1/active - 200 OK returns only ACTIVE plans")
    void getActivePlansByOperator_returns200() throws Exception {
        when(rechargePlanService.getActivePlansByOperator(1L)).thenReturn(List.of(
                buildDto(10L, "Basic 149", "ACTIVE", 1L)
        ));

        mockMvc.perform(get("/api/plans/operator/1/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    // ── GET /api/plans/category/{category} ────────────────────────────────────

    @Test
    @DisplayName("GET /api/plans/category/COMBO - 200 OK returns plans by category")
    void getPlansByCategory_returns200() throws Exception {
        when(rechargePlanService.getPlansByCategory("COMBO")).thenReturn(List.of(
                buildDto(10L, "Basic 149", "ACTIVE", 1L)
        ));

        mockMvc.perform(get("/api/plans/category/COMBO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("COMBO"));
    }
}