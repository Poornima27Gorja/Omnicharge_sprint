package com.omnicharge.operator_service.service;

import com.omnicharge.operator_service.dto.OperatorDto;
import com.omnicharge.operator_service.dto.OperatorRequest;
import com.omnicharge.operator_service.entity.Operator;
import com.omnicharge.operator_service.repository.OperatorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class OperatorServiceImpl implements OperatorService {

    private static final Logger log = LoggerFactory.getLogger(OperatorServiceImpl.class);

    @Autowired
    private OperatorRepository operatorRepository;

    @Override
    @CacheEvict(value = "operators", allEntries = true)
    public OperatorDto addOperator(OperatorRequest request) {
        if (operatorRepository.existsByName(request.getName())) {
            throw new RuntimeException("Operator already exists with name: " + request.getName());
        }

        Operator operator = new Operator();
        operator.setName(request.getName());
        operator.setType(request.getType().toUpperCase());
        operator.setStatus(request.getStatus().toUpperCase());
        operator.setLogoUrl(request.getLogoUrl());
        operator.setDescription(request.getDescription());

        operatorRepository.save(operator);
        log.info("Operator added: {} — operators cache evicted", operator.getName());
        return mapToDto(operator);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "operators",      allEntries = true),
            @CacheEvict(value = "operator-by-id", key = "#id")
    })
    public OperatorDto updateOperator(Long id, OperatorRequest request) {
        Operator operator = operatorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Operator not found with id: " + id));

        // If name is being changed, check it doesn't conflict with another operator
        if (!operator.getName().equalsIgnoreCase(request.getName())
                && operatorRepository.existsByName(request.getName())) {
            throw new RuntimeException("Another operator already exists with name: " + request.getName());
        }

        operator.setName(request.getName());
        operator.setType(request.getType().toUpperCase());
        operator.setStatus(request.getStatus().toUpperCase());
        operator.setLogoUrl(request.getLogoUrl());
        operator.setDescription(request.getDescription());

        operatorRepository.save(operator);
        log.info("Operator updated: id={} — cache evicted", id);
        return mapToDto(operator);
    }

    /**
     * PATCH — changes only the status column.
     * Used by the admin dashboard toggle (Active / Inactive switch).
     * Evicts the same caches as a full update.
     */
    @Override
    @Caching(evict = {
            @CacheEvict(value = "operators",      allEntries = true),
            @CacheEvict(value = "operator-by-id", key = "#id")
    })
    public OperatorDto updateOperatorStatus(Long id, String status) {
        Operator operator = operatorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Operator not found with id: " + id));

        String normalised = status.toUpperCase();
        if (!normalised.equals("ACTIVE") && !normalised.equals("INACTIVE")) {
            throw new RuntimeException("Invalid status. Allowed values: ACTIVE, INACTIVE");
        }

        if (operator.getStatus().equals(normalised)) {
            throw new RuntimeException(
                "Operator is already " + normalised + ". No change made.");
        }

        operator.setStatus(normalised);
        operatorRepository.save(operator);
        log.info("Operator id={} status changed to {} — cache evicted", id, normalised);
        return mapToDto(operator);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "operators",      allEntries = true),
            @CacheEvict(value = "operator-by-id", key = "#id")
    })
    public void deleteOperator(Long id) {
        Operator operator = operatorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Operator not found with id: " + id));
        operatorRepository.delete(operator);
        log.info("Operator deleted: id={} — cache evicted", id);
    }

    @Override
    @Cacheable(value = "operator-by-id", key = "#id")
    public OperatorDto getOperatorById(Long id) {
        log.info("Cache MISS — loading operator from DB: id={}", id);
        Operator operator = operatorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Operator not found with id: " + id));
        return mapToDto(operator);
    }

    @Override
    @Cacheable(value = "operators")
    public List<OperatorDto> getAllOperators() {
        log.info("Cache MISS — loading all operators from DB");
        return operatorRepository.findAll()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<OperatorDto> getOperatorsByStatus(String status) {
        return operatorRepository.findByStatus(status.toUpperCase())
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<OperatorDto> getOperatorsByType(String type) {
        return operatorRepository.findByType(type.toUpperCase())
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