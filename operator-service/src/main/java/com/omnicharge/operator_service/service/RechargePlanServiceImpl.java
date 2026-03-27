package com.omnicharge.operator_service.service;

import com.omnicharge.operator_service.dto.RechargePlanDto;
import com.omnicharge.operator_service.dto.RechargePlanRequest;
import com.omnicharge.operator_service.entity.Operator;
import com.omnicharge.operator_service.entity.RechargePlan;
import com.omnicharge.operator_service.repository.OperatorRepository;
import com.omnicharge.operator_service.repository.RechargePlanRepository;
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
public class RechargePlanServiceImpl implements RechargePlanService {

    private static final Logger log = LoggerFactory.getLogger(RechargePlanServiceImpl.class);

    @Autowired
    private RechargePlanRepository rechargePlanRepository;

    @Autowired
    private OperatorRepository operatorRepository;

    @Override
    @Caching(evict = {
            @CacheEvict(value = "plans",             allEntries = true),
            @CacheEvict(value = "plans-by-operator", allEntries = true)
    })
    public RechargePlanDto addPlan(RechargePlanRequest request) {
        Operator operator = operatorRepository.findById(request.getOperatorId())
                .orElseThrow(() -> new RuntimeException(
                        "Operator not found with id: " + request.getOperatorId()));

        if (!"ACTIVE".equalsIgnoreCase(operator.getStatus())) {
            throw new RuntimeException(
                "Cannot add a plan to an INACTIVE operator. Activate the operator first.");
        }

        RechargePlan plan = new RechargePlan();
        plan.setPlanName(request.getPlanName());
        plan.setPrice(request.getPrice());
        plan.setValidity(request.getValidity());
        plan.setData(request.getData());
        plan.setCalls(request.getCalls());
        plan.setSms(request.getSms());
        plan.setDescription(request.getDescription());
        plan.setCategory(request.getCategory().toUpperCase());
        plan.setStatus(request.getStatus().toUpperCase());
        plan.setOperator(operator);

        rechargePlanRepository.save(plan);
        log.info("Plan added: {} for operator: {} — plans cache evicted",
                plan.getPlanName(), operator.getName());
        return mapToDto(plan);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "plan-by-id",        key = "#id"),
            @CacheEvict(value = "plans",             allEntries = true),
            @CacheEvict(value = "plans-by-operator", allEntries = true)
    })
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
        plan.setCategory(request.getCategory().toUpperCase());
        plan.setStatus(request.getStatus().toUpperCase());
        plan.setOperator(operator);

        rechargePlanRepository.save(plan);
        log.info("Plan updated: id={} — plan cache evicted", id);
        return mapToDto(plan);
    }

    /**
     * PATCH — changes only the status column.
     * Example: admin deactivates a plan mid-month without deleting it.
     */
    @Override
    @Caching(evict = {
            @CacheEvict(value = "plan-by-id",        key = "#id"),
            @CacheEvict(value = "plans",             allEntries = true),
            @CacheEvict(value = "plans-by-operator", allEntries = true)
    })
    public RechargePlanDto updatePlanStatus(Long id, String status) {
        RechargePlan plan = rechargePlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plan not found with id: " + id));

        String normalised = status.toUpperCase();
        if (!normalised.equals("ACTIVE") && !normalised.equals("INACTIVE")) {
            throw new RuntimeException("Invalid status. Allowed values: ACTIVE, INACTIVE");
        }

        if (plan.getStatus().equals(normalised)) {
            throw new RuntimeException(
                "Plan is already " + normalised + ". No change made.");
        }

        plan.setStatus(normalised);
        rechargePlanRepository.save(plan);
        log.info("Plan id={} status changed to {} — cache evicted", id, normalised);
        return mapToDto(plan);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "plan-by-id",        key = "#id"),
            @CacheEvict(value = "plans",             allEntries = true),
            @CacheEvict(value = "plans-by-operator", allEntries = true)
    })
    public void deletePlan(Long id) {
        RechargePlan plan = rechargePlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plan not found with id: " + id));
        rechargePlanRepository.delete(plan);
        log.info("Plan deleted: id={} — plan cache evicted", id);
    }

    @Override
    @Cacheable(value = "plan-by-id", key = "#id")
    public RechargePlanDto getPlanById(Long id) {
        log.info("Cache MISS — loading plan from DB: id={}", id);
        RechargePlan plan = rechargePlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plan not found with id: " + id));
        return mapToDto(plan);
    }

    @Override
    @Cacheable(value = "plans")
    public List<RechargePlanDto> getAllPlans() {
        log.info("Cache MISS — loading all plans from DB");
        return rechargePlanRepository.findAll()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "plans-by-operator", key = "#operatorId")
    public List<RechargePlanDto> getPlansByOperator(Long operatorId) {
        log.info("Cache MISS — loading plans for operatorId={}", operatorId);
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
        return rechargePlanRepository.findByCategory(category.toUpperCase())
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