package com.omnicharge.operator_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnicharge.operator_service.dto.OperatorDto;
import com.omnicharge.operator_service.dto.OperatorRequest;
import com.omnicharge.operator_service.dto.StatusUpdateRequest;
import com.omnicharge.operator_service.exception.GlobalExceptionHandler;
import com.omnicharge.operator_service.service.OperatorService;
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
 * OperatorController standalone MockMvc tests.
 *
 * NOTE: Security (@PreAuthorize) is NOT enforced in standaloneSetup.
 *       These tests verify HTTP routing and response bodies only.
 *
 * FIX: GlobalExceptionHandler is registered via .setControllerAdvice()
 *      so that RuntimeException thrown by the service is caught and
 *      converted to 400 BAD_REQUEST — not 500 INTERNAL_SERVER_ERROR.
 *
 * Covers:
 *  POST   /api/operators           — addOperator
 *  PUT    /api/operators/{id}      — updateOperator
 *  PATCH  /api/operators/{id}/status
 *  DELETE /api/operators/{id}
 *  GET    /api/operators/{id}
 *  GET    /api/operators
 *  GET    /api/operators/status/{status}
 *  GET    /api/operators/type/{type}
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OperatorController Web Layer Tests")
class OperatorControllerTest {

    @InjectMocks
    private OperatorController operatorController;

    @Mock
    private OperatorService operatorService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        // ── KEY FIX ──────────────────────────────────────────────────────────
        // standaloneSetup does NOT auto-load @RestControllerAdvice beans.
        // We must register GlobalExceptionHandler explicitly so that
        // RuntimeException → 400 instead of 500 (which caused all 6 errors).
        // ─────────────────────────────────────────────────────────────────────
        mockMvc = MockMvcBuilders
                .standaloneSetup(operatorController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private OperatorDto buildDto(Long id, String name, String status) {
        OperatorDto dto = new OperatorDto();
        dto.setId(id);
        dto.setName(name);
        dto.setType("MOBILE");
        dto.setStatus(status);
        dto.setLogoUrl("http://logo.url/" + name.toLowerCase());
        dto.setDescription(name + " operator");
        return dto;
    }

    private OperatorRequest buildRequest(String name, String status) {
        OperatorRequest req = new OperatorRequest();
        req.setName(name);
        req.setType("MOBILE");
        req.setStatus(status);
        req.setLogoUrl("http://logo.url");
        req.setDescription("Test");
        return req;
    }

    // ── POST /api/operators ───────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/operators - 201 Created on success")
    void addOperator_returns201() throws Exception {
        OperatorDto created = buildDto(1L, "Airtel", "ACTIVE");
        when(operatorService.addOperator(any(OperatorRequest.class))).thenReturn(created);

        mockMvc.perform(post("/api/operators")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest("Airtel", "ACTIVE"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Airtel"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("POST /api/operators - duplicate name returns 400")
    void addOperator_duplicateName_returns400() throws Exception {
        when(operatorService.addOperator(any()))
                .thenThrow(new RuntimeException("An operator already exists with name: Airtel"));

        mockMvc.perform(post("/api/operators")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest("Airtel", "ACTIVE"))))
                .andExpect(status().isBadRequest());
    }

    // ── PUT /api/operators/{id} ───────────────────────────────────────────────

    @Test
    @DisplayName("PUT /api/operators/1 - 200 OK on update")
    void updateOperator_returns200() throws Exception {
        OperatorDto updated = buildDto(1L, "Airtel Updated", "INACTIVE");
        when(operatorService.updateOperator(eq(1L), any(OperatorRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/api/operators/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest("Airtel Updated", "INACTIVE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Airtel Updated"))
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    @DisplayName("PUT /api/operators/99 - not found returns 400")
    void updateOperator_notFound_returns400() throws Exception {
        when(operatorService.updateOperator(eq(99L), any()))
                .thenThrow(new RuntimeException("Operator not found with id: 99"));

        mockMvc.perform(put("/api/operators/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest("X", "ACTIVE"))))
                .andExpect(status().isBadRequest());
    }

    // ── PATCH /api/operators/{id}/status ─────────────────────────────────────

    @Test
    @DisplayName("PATCH /api/operators/1/status - 200 OK on status toggle")
    void updateStatus_returns200() throws Exception {
        OperatorDto patched = buildDto(1L, "Airtel", "INACTIVE");
        when(operatorService.updateOperatorStatus(1L, "INACTIVE")).thenReturn(patched);

        StatusUpdateRequest req = new StatusUpdateRequest();
        req.setStatus("INACTIVE");

        mockMvc.perform(patch("/api/operators/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    // ── DELETE /api/operators/{id} ────────────────────────────────────────────

    @Test
    @DisplayName("DELETE /api/operators/1 - 200 OK on deletion")
    void deleteOperator_returns200() throws Exception {
        doNothing().when(operatorService).deleteOperator(1L);

        mockMvc.perform(delete("/api/operators/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("Operator deleted successfully"));
    }

    @Test
    @DisplayName("DELETE /api/operators/99 - not found returns 400")
    void deleteOperator_notFound_returns400() throws Exception {
        doThrow(new RuntimeException("Operator not found with id: 99"))
                .when(operatorService).deleteOperator(99L);

        mockMvc.perform(delete("/api/operators/99"))
                .andExpect(status().isBadRequest());
    }

    // ── GET /api/operators/{id} ───────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/operators/1 - 200 OK with operator DTO")
    void getOperatorById_returns200() throws Exception {
        OperatorDto dto = buildDto(1L, "Jio", "ACTIVE");
        when(operatorService.getOperatorById(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/operators/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Jio"));
    }

    @Test
    @DisplayName("GET /api/operators/99 - not found returns 400")
    void getOperatorById_notFound_returns400() throws Exception {
        when(operatorService.getOperatorById(99L))
                .thenThrow(new RuntimeException("Operator not found with id: 99"));

        mockMvc.perform(get("/api/operators/99"))
                .andExpect(status().isBadRequest());
    }

    // ── GET /api/operators ────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/operators - 200 OK returns list")
    void getAllOperators_returns200() throws Exception {
        when(operatorService.getAllOperators()).thenReturn(List.of(
                buildDto(1L, "Airtel", "ACTIVE"),
                buildDto(2L, "Jio", "ACTIVE")
        ));

        mockMvc.perform(get("/api/operators"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ── GET /api/operators/status/{status} ────────────────────────────────────

    @Test
    @DisplayName("GET /api/operators/status/ACTIVE - returns filtered list")
    void getByStatus_returns200() throws Exception {
        when(operatorService.getOperatorsByStatus("ACTIVE"))
                .thenReturn(List.of(buildDto(1L, "Airtel", "ACTIVE")));

        mockMvc.perform(get("/api/operators/status/ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    // ── GET /api/operators/type/{type} ────────────────────────────────────────

    @Test
    @DisplayName("GET /api/operators/type/MOBILE - returns filtered list")
    void getByType_returns200() throws Exception {
        when(operatorService.getOperatorsByType("MOBILE"))
                .thenReturn(List.of(buildDto(1L, "Airtel", "ACTIVE")));

        mockMvc.perform(get("/api/operators/type/MOBILE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("MOBILE"));
    }
}