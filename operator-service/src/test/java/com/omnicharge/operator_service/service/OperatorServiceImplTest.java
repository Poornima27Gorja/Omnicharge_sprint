package com.omnicharge.operator_service.service;

import com.omnicharge.operator_service.dto.OperatorDto;
import com.omnicharge.operator_service.dto.OperatorRequest;
import com.omnicharge.operator_service.entity.Operator;
import com.omnicharge.operator_service.repository.OperatorRepository;
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
@DisplayName("OperatorServiceImpl Unit Tests")
class OperatorServiceImplTest {

    @InjectMocks
    private OperatorServiceImpl operatorService;

    @Mock
    private OperatorRepository operatorRepository;

    // ── helpers ───────────────────────────────────────────────────────────────

    private Operator buildOperator(Long id, String name, String status) {
        Operator op = new Operator();
        op.setId(id);
        op.setName(name);
        op.setType("MOBILE");
        op.setStatus(status);
        op.setLogoUrl("http://logo.url");
        op.setDescription("Test operator");
        return op;
    }

    private OperatorRequest buildRequest(String name, String status) {
        OperatorRequest req = new OperatorRequest();
        req.setName(name);
        req.setType("MOBILE");
        req.setStatus(status);
        req.setLogoUrl("http://logo.url");
        req.setDescription("Test operator");
        return req;
    }

    // ── addOperator() ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("addOperator() - success: new operator is saved")
    void addOperator_success() {
        OperatorRequest req = buildRequest("Airtel", "ACTIVE");
        when(operatorRepository.existsByName("Airtel")).thenReturn(false);
        when(operatorRepository.save(any(Operator.class))).thenAnswer(inv -> {
            Operator op = inv.getArgument(0);
            op.setId(1L);
            return op;
        });

        OperatorDto dto = operatorService.addOperator(req);

        assertThat(dto.getName()).isEqualTo("Airtel");
        assertThat(dto.getStatus()).isEqualTo("ACTIVE");
        verify(operatorRepository).save(any(Operator.class));
    }

    @Test
    @DisplayName("addOperator() - fail: duplicate name throws exception")
    void addOperator_duplicateName_throwsException() {
        OperatorRequest req = buildRequest("Airtel", "ACTIVE");
        when(operatorRepository.existsByName("Airtel")).thenReturn(true);

        assertThatThrownBy(() -> operatorService.addOperator(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Operator already exists");

        verify(operatorRepository, never()).save(any());
    }

    // ── updateOperator() ──────────────────────────────────────────────────────

    @Test
    @DisplayName("updateOperator() - success: existing operator is updated")
    void updateOperator_success() {
        Operator existing = buildOperator(1L, "Airtel", "ACTIVE");
        OperatorRequest req = buildRequest("Airtel Updated", "INACTIVE");

        when(operatorRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(operatorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OperatorDto dto = operatorService.updateOperator(1L, req);

        assertThat(dto.getName()).isEqualTo("Airtel Updated");
        assertThat(dto.getStatus()).isEqualTo("INACTIVE");
    }

    @Test
    @DisplayName("updateOperator() - fail: operator not found throws exception")
    void updateOperator_notFound_throwsException() {
        when(operatorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> operatorService.updateOperator(99L, buildRequest("X", "ACTIVE")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Operator not found");
    }

    // ── deleteOperator() ──────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteOperator() - success: existing operator is deleted")
    void deleteOperator_success() {
        Operator op = buildOperator(1L, "Airtel", "ACTIVE");
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(op));

        operatorService.deleteOperator(1L);

        verify(operatorRepository).delete(op);
    }

    @Test
    @DisplayName("deleteOperator() - fail: operator not found throws exception")
    void deleteOperator_notFound_throwsException() {
        when(operatorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> operatorService.deleteOperator(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Operator not found");
    }

    // ── getOperatorById() ─────────────────────────────────────────────────────

    @Test
    @DisplayName("getOperatorById() - success: returns DTO")
    void getOperatorById_success() {
        Operator op = buildOperator(1L, "Jio", "ACTIVE");
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(op));

        OperatorDto dto = operatorService.getOperatorById(1L);

        assertThat(dto.getName()).isEqualTo("Jio");
    }

    @Test
    @DisplayName("getOperatorById() - fail: not found throws exception")
    void getOperatorById_notFound_throwsException() {
        when(operatorRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> operatorService.getOperatorById(5L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Operator not found");
    }

    // ── getAllOperators() ─────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllOperators() - returns all operators as DTOs")
    void getAllOperators_returnsList() {
        when(operatorRepository.findAll()).thenReturn(List.of(
                buildOperator(1L, "Airtel", "ACTIVE"),
                buildOperator(2L, "Jio", "ACTIVE")
        ));

        List<OperatorDto> result = operatorService.getAllOperators();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(OperatorDto::getName)
                .containsExactly("Airtel", "Jio");
    }

    // ── getOperatorsByStatus() ────────────────────────────────────────────────

    @Test
    @DisplayName("getOperatorsByStatus() - returns only operators with given status")
    void getOperatorsByStatus_returnsFiltered() {
        when(operatorRepository.findByStatus("ACTIVE")).thenReturn(List.of(
                buildOperator(1L, "Airtel", "ACTIVE")
        ));

        List<OperatorDto> result = operatorService.getOperatorsByStatus("ACTIVE");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo("ACTIVE");
    }

    // ── getOperatorsByType() ──────────────────────────────────────────────────

    @Test
    @DisplayName("getOperatorsByType() - returns operators filtered by type")
    void getOperatorsByType_returnsFiltered() {
        when(operatorRepository.findByType("MOBILE")).thenReturn(List.of(
                buildOperator(1L, "Airtel", "ACTIVE"),
                buildOperator(2L, "Vi", "ACTIVE")
        ));

        List<OperatorDto> result = operatorService.getOperatorsByType("MOBILE");

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(dto -> "MOBILE".equals(dto.getType()));
    }
}
