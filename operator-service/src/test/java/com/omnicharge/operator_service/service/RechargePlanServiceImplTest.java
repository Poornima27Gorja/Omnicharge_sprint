package com.omnicharge.operator_service.service;

import com.omnicharge.operator_service.dto.RechargePlanDto;
import com.omnicharge.operator_service.dto.RechargePlanRequest;
import com.omnicharge.operator_service.entity.Operator;
import com.omnicharge.operator_service.entity.RechargePlan;
import com.omnicharge.operator_service.repository.OperatorRepository;
import com.omnicharge.operator_service.repository.RechargePlanRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RechargePlanServiceImpl Unit Tests")
class RechargePlanServiceImplTest {

    @InjectMocks
    private RechargePlanServiceImpl rechargePlanService;

    @Mock
    private RechargePlanRepository rechargePlanRepository;

    @Mock
    private OperatorRepository operatorRepository;

    // ── helpers ───────────────────────────────────────────────────────────────

    private Operator buildOperator(Long id) {
        Operator op = new Operator();
        op.setId(id);
        op.setName("Airtel");
        op.setType("MOBILE");
        op.setStatus("ACTIVE");
        return op;
    }

    private RechargePlan buildPlan(Long id, Operator op, String status) {
        RechargePlan plan = new RechargePlan();
        plan.setId(id);
        plan.setPlanName("Basic 149");
        plan.setPrice(149.0);
        plan.setValidity("28 days");
        plan.setData("1.5 GB/day");
        plan.setCalls("Unlimited");
        plan.setSms("100/day");
        plan.setDescription("Popular plan");
        plan.setCategory("POPULAR");
        plan.setStatus(status);
        plan.setOperator(op);
        return plan;
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
        req.setCategory("POPULAR");
        req.setStatus("ACTIVE");
        return req;
    }

    // ── addPlan() ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("addPlan() - success: plan is saved and DTO returned")
    void addPlan_success() {
        Operator op = buildOperator(1L);
        RechargePlanRequest req = buildRequest(1L);

        when(operatorRepository.findById(1L)).thenReturn(Optional.of(op));
        when(rechargePlanRepository.save(any(RechargePlan.class))).thenAnswer(inv -> {
            RechargePlan p = inv.getArgument(0);
            p.setId(10L);
            return p;
        });

        RechargePlanDto dto = rechargePlanService.addPlan(req);

        assertThat(dto.getPlanName()).isEqualTo("Basic 149");
        assertThat(dto.getPrice()).isEqualTo(149.0);
        assertThat(dto.getOperatorName()).isEqualTo("Airtel");
        verify(rechargePlanRepository).save(any(RechargePlan.class));
    }

    @Test
    @DisplayName("addPlan() - fail: operator not found throws exception")
    void addPlan_operatorNotFound_throwsException() {
        when(operatorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rechargePlanService.addPlan(buildRequest(99L)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Operator not found");
    }

    // ── updatePlan() ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("updatePlan() - success: plan is updated with new values")
    void updatePlan_success() {
        Operator op = buildOperator(1L);
        RechargePlan existingPlan = buildPlan(10L, op, "ACTIVE");
        RechargePlanRequest req = buildRequest(1L);
        req.setPlanName("Super 199");
        req.setPrice(199.0);

        when(rechargePlanRepository.findById(10L)).thenReturn(Optional.of(existingPlan));
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(op));
        when(rechargePlanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RechargePlanDto dto = rechargePlanService.updatePlan(10L, req);

        assertThat(dto.getPlanName()).isEqualTo("Super 199");
        assertThat(dto.getPrice()).isEqualTo(199.0);
    }

    @Test
    @DisplayName("updatePlan() - fail: plan not found throws exception")
    void updatePlan_planNotFound_throwsException() {
        when(rechargePlanRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rechargePlanService.updatePlan(99L, buildRequest(1L)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Plan not found");
    }

    // ── deletePlan() ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("deletePlan() - success: plan is deleted")
    void deletePlan_success() {
        Operator op = buildOperator(1L);
        RechargePlan plan = buildPlan(10L, op, "ACTIVE");
        when(rechargePlanRepository.findById(10L)).thenReturn(Optional.of(plan));

        rechargePlanService.deletePlan(10L);

        verify(rechargePlanRepository).delete(plan);
    }

    @Test
    @DisplayName("deletePlan() - fail: plan not found throws exception")
    void deletePlan_notFound_throwsException() {
        when(rechargePlanRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rechargePlanService.deletePlan(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Plan not found");
    }

    // ── getPlanById() ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getPlanById() - success: returns DTO")
    void getPlanById_success() {
        Operator op = buildOperator(1L);
        RechargePlan plan = buildPlan(10L, op, "ACTIVE");
        when(rechargePlanRepository.findById(10L)).thenReturn(Optional.of(plan));

        RechargePlanDto dto = rechargePlanService.getPlanById(10L);

        assertThat(dto.getId()).isEqualTo(10L);
        assertThat(dto.getOperatorId()).isEqualTo(1L);
    }

    // ── getPlansByOperator() ──────────────────────────────────────────────────

    @Test
    @DisplayName("getPlansByOperator() - returns all plans for an operator")
    void getPlansByOperator_returnsList() {
        Operator op = buildOperator(1L);
        when(rechargePlanRepository.findByOperatorId(1L)).thenReturn(List.of(
                buildPlan(10L, op, "ACTIVE"),
                buildPlan(11L, op, "INACTIVE")
        ));

        List<RechargePlanDto> result = rechargePlanService.getPlansByOperator(1L);

        assertThat(result).hasSize(2);
    }

    // ── getActivePlansByOperator() ────────────────────────────────────────────

    @Test
    @DisplayName("getActivePlansByOperator() - returns only ACTIVE plans")
    void getActivePlansByOperator_onlyActive() {
        Operator op = buildOperator(1L);
        when(rechargePlanRepository.findByOperatorIdAndStatus(1L, "ACTIVE")).thenReturn(List.of(
                buildPlan(10L, op, "ACTIVE")
        ));

        List<RechargePlanDto> result = rechargePlanService.getActivePlansByOperator(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo("ACTIVE");
    }
}
