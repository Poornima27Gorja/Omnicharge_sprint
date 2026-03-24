package com.omnicharge.operator_service.service;

import com.omnicharge.operator_service.dto.OperatorDto;
import com.omnicharge.operator_service.dto.OperatorRequest;

import java.util.List;

public interface OperatorService {

    OperatorDto addOperator(OperatorRequest request);

    OperatorDto updateOperator(Long id, OperatorRequest request);

    void deleteOperator(Long id);

    OperatorDto getOperatorById(Long id);

    List<OperatorDto> getAllOperators();

    List<OperatorDto> getOperatorsByStatus(String status);

    List<OperatorDto> getOperatorsByType(String type);
}
