package com.omnicharge.operator_service.service;

import com.omnicharge.operator_service.dto.RechargePlanDto;
import com.omnicharge.operator_service.dto.RechargePlanRequest;
import com.omnicharge.operator_service.entity.Operator;
import com.omnicharge.operator_service.entity.RechargePlan;
import com.omnicharge.operator_service.repository.OperatorRepository;
import com.omnicharge.operator_service.repository.RechargePlanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RechargePlanServiceImpl implements RechargePlanService {

    @Autowired
    private RechargePlanRepository rechargePlanRepository;

    @Autowired
    private OperatorRepository operatorRepository;

    @Override
    public RechargePlanDto addPlan(RechargePlanRequest request) {
        Operator operator = operatorRepository.findById(request.getOperatorId())
                .orElseThrow(() -> new RuntimeException(
                        "Operator not found with id: " + request.getOperatorId()));

        RechargePlan plan = new RechargePlan();
        plan.setPlanName(request.getPlanName());
        plan.setPrice(request.getPrice());
        plan.setValidity(request.getValidity());
        plan.setData(request.getData());
        plan.setCalls(request.getCalls());
        plan.setSms(request.getSms());
        plan.setDescription(request.getDescription());
        plan.setCategory(request.getCategory());
        plan.setStatus(request.getStatus());
        plan.setOperator(operator);

        rechargePlanRepository.save(plan);
        return mapToDto(plan);
    }

    @Override
    public RechargePlanDto updatePlan(Long id, RechargePlanRequest request) {
        RechargePlan plan = rechargePlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plan not found with id: " + id));

        Operator operator = operatorRepository.findById(request.getOperatorId())
                .orElseThrow(() -> new RuntimeException(
                        "Operator not found with id: " + request.getOperatorId()));

        plan.setPlanName(request.getPlanName());
        plan.setPrice(request.getPrice());
        plan.setValidity(request.getValidity());
        plan.setData(request.getData());
        plan.setCalls(request.getCalls());
        plan.setSms(request.getSms());
        plan.setDescription(request.getDescription());
        plan.setCategory(request.getCategory());
        plan.setStatus(request.getStatus());
        plan.setOperator(operator);

        rechargePlanRepository.save(plan);
        return mapToDto(plan);
    }

    @Override
    public void deletePlan(Long id) {
        RechargePlan plan = rechargePlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plan not found with id: " + id));
        rechargePlanRepository.delete(plan);
    }

    @Override
    public RechargePlanDto getPlanById(Long id) {
        RechargePlan plan = rechargePlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plan not found with id: " + id));
        return mapToDto(plan);
    }

    @Override
    public List<RechargePlanDto> getAllPlans() {
        return rechargePlanRepository.findAll()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<RechargePlanDto> getPlansByOperator(Long operatorId) {
        return rechargePlanRepository.findByOperatorId(operatorId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<RechargePlanDto> getActivePlansByOperator(Long operatorId) {
        return rechargePlanRepository.findByOperatorIdAndStatus(operatorId, "ACTIVE")
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<RechargePlanDto> getPlansByCategory(String category) {
        return rechargePlanRepository.findByCategory(category)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private RechargePlanDto mapToDto(RechargePlan plan) {
        RechargePlanDto dto = new RechargePlanDto();
        dto.setId(plan.getId());
        dto.setPlanName(plan.getPlanName());
        dto.setPrice(plan.getPrice());
        dto.setValidity(plan.getValidity());
        dto.setData(plan.getData());
        dto.setCalls(plan.getCalls());
        dto.setSms(plan.getSms());
        dto.setDescription(plan.getDescription());
        dto.setCategory(plan.getCategory());
        dto.setStatus(plan.getStatus());
        dto.setOperatorId(plan.getOperator().getId());
        dto.setOperatorName(plan.getOperator().getName());
        return dto;
    }
}
