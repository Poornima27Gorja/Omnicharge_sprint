package com.omnicharge.operator_service.service;

import com.omnicharge.operator_service.dto.OperatorDto;
import com.omnicharge.operator_service.dto.OperatorRequest;

import java.util.List;

public interface OperatorService {

    OperatorDto addOperator(OperatorRequest request);

    OperatorDto updateOperator(Long id, OperatorRequest request);

    /** PATCH — toggles only the status field (ACTIVE / INACTIVE). */
    OperatorDto updateOperatorStatus(Long id, String status);

    void deleteOperator(Long id);

    OperatorDto getOperatorById(Long id);

    List<OperatorDto> getAllOperators();

    List<OperatorDto> getOperatorsByStatus(String status);

    List<OperatorDto> getOperatorsByType(String type);
}