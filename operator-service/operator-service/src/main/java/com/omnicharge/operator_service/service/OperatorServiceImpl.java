package com.omnicharge.operator_service.service;

import com.omnicharge.operator_service.dto.OperatorDto;
import com.omnicharge.operator_service.dto.OperatorRequest;
import com.omnicharge.operator_service.entity.Operator;
import com.omnicharge.operator_service.repository.OperatorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class OperatorServiceImpl implements OperatorService {

    @Autowired
    private OperatorRepository operatorRepository;

    @Override
    public OperatorDto addOperator(OperatorRequest request) {
        if (operatorRepository.existsByName(request.getName())) {
            throw new RuntimeException("Operator already exists with name: " + request.getName());
        }

        Operator operator = new Operator();
        operator.setName(request.getName());
        operator.setType(request.getType());
        operator.setStatus(request.getStatus());
        operator.setLogoUrl(request.getLogoUrl());
        operator.setDescription(request.getDescription());

        operatorRepository.save(operator);
        return mapToDto(operator);
    }

    @Override
    public OperatorDto updateOperator(Long id, OperatorRequest request) {
        Operator operator = operatorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Operator not found with id: " + id));

        operator.setName(request.getName());
        operator.setType(request.getType());
        operator.setStatus(request.getStatus());
        operator.setLogoUrl(request.getLogoUrl());
        operator.setDescription(request.getDescription());

        operatorRepository.save(operator);
        return mapToDto(operator);
    }

    @Override
    public void deleteOperator(Long id) {
        Operator operator = operatorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Operator not found with id: " + id));
        operatorRepository.delete(operator);
    }

    @Override
    public OperatorDto getOperatorById(Long id) {
        Operator operator = operatorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Operator not found with id: " + id));
        return mapToDto(operator);
    }

    @Override
    public List<OperatorDto> getAllOperators() {
        return operatorRepository.findAll()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<OperatorDto> getOperatorsByStatus(String status) {
        return operatorRepository.findByStatus(status)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<OperatorDto> getOperatorsByType(String type) {
        return operatorRepository.findByType(type)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private OperatorDto mapToDto(Operator operator) {
        OperatorDto dto = new OperatorDto();
        dto.setId(operator.getId());
        dto.setName(operator.getName());
        dto.setType(operator.getType());
        dto.setStatus(operator.getStatus());
        dto.setLogoUrl(operator.getLogoUrl());
        dto.setDescription(operator.getDescription());
        return dto;
    }
}
