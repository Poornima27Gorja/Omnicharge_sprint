package com.omnicharge.recharge_service.service;

import com.omnicharge.recharge_service.dto.*;
import com.omnicharge.recharge_service.entity.RechargeRequest;
import com.omnicharge.recharge_service.entity.RechargeStatus;
import com.omnicharge.recharge_service.feign.OperatorFeignClient;
import com.omnicharge.recharge_service.messaging.RechargeEventPublisher;
import com.omnicharge.recharge_service.repository.RechargeRepository;
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
@DisplayName("RechargeServiceImpl Unit Tests")
class RechargeServiceImplTest {

    @InjectMocks
    private RechargeServiceImpl rechargeService;

    @Mock
    private RechargeRepository rechargeRepository;

    @Mock
    private OperatorFeignClient operatorFeignClient;

    @Mock
    private RechargeEventPublisher rechargeEventPublisher;

    // ── helpers ───────────────────────────────────────────────────────────────

    private OperatorResponse activeOperator() {
        OperatorResponse op = new OperatorResponse();
        op.setId(1L);
        op.setName("Airtel");
        op.setStatus("ACTIVE");
        return op;
    }

    private PlanResponse activePlan(Long operatorId) {
        PlanResponse plan = new PlanResponse();
        plan.setId(10L);
        plan.setPlanName("Basic 149");
        plan.setPrice(149.0);
        plan.setValidity("28 days");
        plan.setData("1.5 GB/day");
        plan.setOperatorId(operatorId);
        plan.setStatus("ACTIVE");
        return plan;
    }

    private RechargeRequest savedRecharge(Long id) {
        RechargeRequest r = new RechargeRequest();
        r.setId(id);
        r.setUsername("alice");
        r.setMobileNumber("9876543210");
        r.setOperatorId(1L);
        r.setOperatorName("Airtel");
        r.setPlanId(10L);
        r.setPlanName("Basic 149");
        r.setAmount(149.0);
        r.setValidity("28 days");
        r.setDataInfo("1.5 GB/day");
        r.setStatus(RechargeStatus.PENDING);
        r.setCreatedAt(LocalDateTime.now());
        r.setUpdatedAt(LocalDateTime.now());
        return r;
    }

    // ── initiateRecharge() ────────────────────────────────────────────────────

    @Test
    @DisplayName("initiateRecharge() - success: PENDING recharge created and event published")
    void initiateRecharge_success() {
        InitiateRechargeRequest req = new InitiateRechargeRequest();
        req.setMobileNumber("9876543210");
        req.setOperatorId(1L);
        req.setPlanId(10L);

        when(operatorFeignClient.getOperatorById(1L, "Bearer token")).thenReturn(activeOperator());
        when(operatorFeignClient.getPlanById(10L, "Bearer token")).thenReturn(activePlan(1L));
        when(rechargeRepository.save(any(RechargeRequest.class))).thenAnswer(inv -> {
            RechargeRequest r = inv.getArgument(0);
            r.setId(100L);
            r.setCreatedAt(LocalDateTime.now());
            r.setUpdatedAt(LocalDateTime.now());
            return r;
        });

        RechargeRequestDto dto = rechargeService.initiateRecharge("alice", "Bearer token", req);

        assertThat(dto.getUsername()).isEqualTo("alice");
        assertThat(dto.getMobileNumber()).isEqualTo("9876543210");
        assertThat(dto.getStatus()).isEqualTo("PENDING");
        assertThat(dto.getAmount()).isEqualTo(149.0);
        verify(rechargeEventPublisher).publishRechargeEvent(any(RechargeEventMessage.class));
    }

    @Test
    @DisplayName("initiateRecharge() - fail: invalid mobile number (5-digit) throws exception")
    void initiateRecharge_invalidMobile_throwsException() {
        InitiateRechargeRequest req = new InitiateRechargeRequest();
        req.setMobileNumber("12345");
        req.setOperatorId(1L);
        req.setPlanId(10L);

        assertThatThrownBy(() -> rechargeService.initiateRecharge("alice", "Bearer token", req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid mobile number");

        verify(operatorFeignClient, never()).getOperatorById(anyLong(), anyString());
    }

    @Test
    @DisplayName("initiateRecharge() - fail: mobile starting with 5 (non-Indian) throws exception")
    void initiateRecharge_nonIndianMobile_throwsException() {
        InitiateRechargeRequest req = new InitiateRechargeRequest();
        req.setMobileNumber("5876543210");
        req.setOperatorId(1L);
        req.setPlanId(10L);

        assertThatThrownBy(() -> rechargeService.initiateRecharge("alice", "Bearer token", req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid mobile number");
    }

    @Test
    @DisplayName("initiateRecharge() - fail: inactive operator throws exception")
    void initiateRecharge_inactiveOperator_throwsException() {
        InitiateRechargeRequest req = new InitiateRechargeRequest();
        req.setMobileNumber("9876543210");
        req.setOperatorId(1L);
        req.setPlanId(10L);

        OperatorResponse op = activeOperator();
        op.setStatus("INACTIVE");
        when(operatorFeignClient.getOperatorById(1L, "Bearer token")).thenReturn(op);

        assertThatThrownBy(() -> rechargeService.initiateRecharge("alice", "Bearer token", req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("currently inactive");
    }

    @Test
    @DisplayName("initiateRecharge() - fail: plan does not belong to operator")
    void initiateRecharge_planOperatorMismatch_throwsException() {
        InitiateRechargeRequest req = new InitiateRechargeRequest();
        req.setMobileNumber("9876543210");
        req.setOperatorId(1L);
        req.setPlanId(10L);

        when(operatorFeignClient.getOperatorById(1L, "Bearer token")).thenReturn(activeOperator());
        // Plan belongs to operator 2, not 1
        when(operatorFeignClient.getPlanById(10L, "Bearer token")).thenReturn(activePlan(2L));

        assertThatThrownBy(() -> rechargeService.initiateRecharge("alice", "Bearer token", req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Plan does not belong");
    }

    @Test
    @DisplayName("initiateRecharge() - fail: inactive plan throws exception")
    void initiateRecharge_inactivePlan_throwsException() {
        InitiateRechargeRequest req = new InitiateRechargeRequest();
        req.setMobileNumber("9876543210");
        req.setOperatorId(1L);
        req.setPlanId(10L);

        when(operatorFeignClient.getOperatorById(1L, "Bearer token")).thenReturn(activeOperator());
        PlanResponse plan = activePlan(1L);
        plan.setStatus("INACTIVE");
        when(operatorFeignClient.getPlanById(10L, "Bearer token")).thenReturn(plan);

        assertThatThrownBy(() -> rechargeService.initiateRecharge("alice", "Bearer token", req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("currently inactive");
    }

    @Test
    @DisplayName("initiateRecharge() - fail: feign exception for operator maps to RuntimeException")
    void initiateRecharge_feignExceptionForOperator_throwsException() {
        InitiateRechargeRequest req = new InitiateRechargeRequest();
        req.setMobileNumber("9876543210");
        req.setOperatorId(99L);
        req.setPlanId(10L);

        when(operatorFeignClient.getOperatorById(99L, "Bearer token"))
                .thenThrow(new RuntimeException("Feign error"));

        assertThatThrownBy(() -> rechargeService.initiateRecharge("alice", "Bearer token", req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Operator not found");
    }

    // ── updateRechargeStatus() ────────────────────────────────────────────────

    /** Helper — builds RechargeStatusUpdateRequest using setters (no parameterized constructor) */
    private RechargeStatusUpdateRequest statusRequest(String status, String failureReason) {
        RechargeStatusUpdateRequest req = new RechargeStatusUpdateRequest();
        req.setStatus(status);
        req.setFailureReason(failureReason);
        return req;
    }

    @Test
    @DisplayName("updateRechargeStatus() - success: status updated to SUCCESS")
    void updateRechargeStatus_success() {
        RechargeRequest existing = savedRecharge(100L);
        when(rechargeRepository.findById(100L)).thenReturn(Optional.of(existing));
        when(rechargeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RechargeRequestDto dto = rechargeService.updateRechargeStatus(100L, statusRequest("SUCCESS", null));

        assertThat(dto.getStatus()).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("updateRechargeStatus() - success: FAILED status saved with failure reason")
    void updateRechargeStatus_failed_withReason() {
        RechargeRequest existing = savedRecharge(100L);
        when(rechargeRepository.findById(100L)).thenReturn(Optional.of(existing));
        when(rechargeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RechargeRequestDto dto = rechargeService.updateRechargeStatus(100L, statusRequest("FAILED", "Insufficient funds"));

        assertThat(dto.getStatus()).isEqualTo("FAILED");
        assertThat(dto.getFailureReason()).isEqualTo("Insufficient funds");
    }

    @Test
    @DisplayName("updateRechargeStatus() - fail: recharge not found throws exception")
    void updateRechargeStatus_notFound_throwsException() {
        when(rechargeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                rechargeService.updateRechargeStatus(999L, statusRequest("SUCCESS", null)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Recharge not found");
    }

    // ── getMyRechargeHistory() ────────────────────────────────────────────────

    @Test
    @DisplayName("getMyRechargeHistory() - returns all recharges for a user")
    void getMyRechargeHistory_returnsList() {
        when(rechargeRepository.findByUsernameOrderByCreatedAtDesc("alice"))
                .thenReturn(List.of(savedRecharge(1L), savedRecharge(2L)));

        List<RechargeRequestDto> result = rechargeService.getMyRechargeHistory("alice");

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(dto -> "alice".equals(dto.getUsername()));
    }

    // ── getRechargeById() ─────────────────────────────────────────────────────

    @Test
    @DisplayName("getRechargeById() - USER can access own recharge")
    void getRechargeById_userOwnsRecharge_success() {
        when(rechargeRepository.findById(1L)).thenReturn(Optional.of(savedRecharge(1L)));

        RechargeRequestDto dto = rechargeService.getRechargeById(1L, "alice", false);

        assertThat(dto.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getRechargeById() - USER cannot access another user's recharge")
    void getRechargeById_userAccessOthers_throwsException() {
        when(rechargeRepository.findById(1L)).thenReturn(Optional.of(savedRecharge(1L)));

        assertThatThrownBy(() -> rechargeService.getRechargeById(1L, "bob", false))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    @DisplayName("getRechargeById() - ADMIN can access any recharge")
    void getRechargeById_adminAccessAny_success() {
        when(rechargeRepository.findById(1L)).thenReturn(Optional.of(savedRecharge(1L)));

        RechargeRequestDto dto = rechargeService.getRechargeById(1L, "adminUser", true);

        assertThat(dto.getId()).isEqualTo(1L);
    }

    // ── getRechargesByStatus() ────────────────────────────────────────────────

    @Test
    @DisplayName("getRechargesByStatus() - valid status returns list")
    void getRechargesByStatus_valid_returnsList() {
        when(rechargeRepository.findByStatus(RechargeStatus.PENDING))
                .thenReturn(List.of(savedRecharge(1L)));

        List<RechargeRequestDto> result = rechargeService.getRechargesByStatus("PENDING");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("getRechargesByStatus() - invalid status throws exception")
    void getRechargesByStatus_invalid_throwsException() {
        assertThatThrownBy(() -> rechargeService.getRechargesByStatus("UNKNOWN"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid status");
    }
}
