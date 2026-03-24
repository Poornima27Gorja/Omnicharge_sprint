package com.omnicharge.recharge_service.service;

import com.omnicharge.recharge_service.dto.*;
import com.omnicharge.recharge_service.entity.RechargeRequest;
import com.omnicharge.recharge_service.entity.RechargeStatus;
import com.omnicharge.recharge_service.feign.OperatorFeignClient;
import com.omnicharge.recharge_service.messaging.RechargeEventPublisher;
import com.omnicharge.recharge_service.repository.RechargeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RechargeServiceImpl implements RechargeService {

    @Autowired
    private RechargeRepository rechargeRepository;

    @Autowired
    private OperatorFeignClient operatorFeignClient;

    @Autowired
    private RechargeEventPublisher rechargeEventPublisher;

    @Override
    public RechargeRequestDto initiateRecharge(String username,
                                               String authToken,
                                               InitiateRechargeRequest request) {

        if (request.getMobileNumber() == null
                || !request.getMobileNumber().matches("^[6-9]\\d{9}$")) {
            throw new RuntimeException(
                    "Invalid mobile number. Must be a valid 10 digit Indian mobile number");
        }

        OperatorResponse operator;
        try {
            operator = operatorFeignClient.getOperatorById(
                    request.getOperatorId(), authToken);
        } catch (Exception ex) {
            throw new RuntimeException(
                    "Operator not found with id: " + request.getOperatorId());
        }

        if (!"ACTIVE".equalsIgnoreCase(operator.getStatus())) {
            throw new RuntimeException(
                    "Operator " + operator.getName() + " is currently inactive");
        }

        PlanResponse plan;
        try {
            plan = operatorFeignClient.getPlanById(request.getPlanId(), authToken);
        } catch (Exception ex) {
            throw new RuntimeException(
                    "Plan not found with id: " + request.getPlanId());
        }

        if (!"ACTIVE".equalsIgnoreCase(plan.getStatus())) {
            throw new RuntimeException(
                    "Plan " + plan.getPlanName() + " is currently inactive");
        }

        if (!plan.getOperatorId().equals(request.getOperatorId())) {
            throw new RuntimeException(
                    "Plan does not belong to the selected operator");
        }

        RechargeRequest rechargeRequest = new RechargeRequest();
        rechargeRequest.setUsername(username);
        rechargeRequest.setMobileNumber(request.getMobileNumber());
        rechargeRequest.setOperatorId(operator.getId());
        rechargeRequest.setOperatorName(operator.getName());
        rechargeRequest.setPlanId(plan.getId());
        rechargeRequest.setPlanName(plan.getPlanName());
        rechargeRequest.setAmount(plan.getPrice());
        rechargeRequest.setValidity(plan.getValidity());
        rechargeRequest.setDataInfo(plan.getData());
        rechargeRequest.setStatus(RechargeStatus.PENDING);

        RechargeRequest saved = rechargeRepository.save(rechargeRequest);

        RechargeEventMessage event = buildEventMessage(saved);
        rechargeEventPublisher.publishRechargeEvent(event);

        return mapToDto(saved);
    }

    // Called by payment-service via Feign after payment is processed
    @Override
    public RechargeRequestDto updateRechargeStatus(Long rechargeId,
                                                    RechargeStatusUpdateRequest request) {
        RechargeRequest recharge = rechargeRepository.findById(rechargeId)
                .orElseThrow(() -> new RuntimeException(
                        "Recharge not found with id: " + rechargeId));

        RechargeStatus newStatus = RechargeStatus.valueOf(request.getStatus().toUpperCase());
        recharge.setStatus(newStatus);

        if (request.getFailureReason() != null) {
            recharge.setFailureReason(request.getFailureReason());
        }

        rechargeRepository.save(recharge);
        return mapToDto(recharge);
    }

    @Override
    public List<RechargeRequestDto> getMyRechargeHistory(String username) {
        return rechargeRepository.findByUsernameOrderByCreatedAtDesc(username)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public RechargeRequestDto getRechargeById(Long id,
                                               String username,
                                               boolean isAdmin) {
        RechargeRequest recharge = rechargeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Recharge not found with id: " + id));

        if (!isAdmin && !recharge.getUsername().equals(username)) {
            throw new RuntimeException(
                    "Access denied. You can only view your own recharge");
        }

        return mapToDto(recharge);
    }

    @Override
    public List<RechargeRequestDto> getAllRecharges() {
        return rechargeRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<RechargeRequestDto> getRechargesByStatus(String status) {
        try {
            RechargeStatus rechargeStatus = RechargeStatus.valueOf(status.toUpperCase());
            return rechargeRepository.findByStatus(rechargeStatus)
                    .stream()
                    .map(this::mapToDto)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException(
                    "Invalid status. Valid values: PENDING, SUCCESS, FAILED");
        }
    }

    @Override
    public List<RechargeRequestDto> getRechargesByMobile(String mobileNumber) {
        return rechargeRepository.findByMobileNumber(mobileNumber)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private RechargeEventMessage buildEventMessage(RechargeRequest recharge) {
        RechargeEventMessage event = new RechargeEventMessage();
        event.setRechargeId(recharge.getId());
        event.setUsername(recharge.getUsername());
        event.setMobileNumber(recharge.getMobileNumber());
        event.setOperatorName(recharge.getOperatorName());
        event.setPlanName(recharge.getPlanName());
        event.setAmount(recharge.getAmount());
        event.setValidity(recharge.getValidity());
        event.setDataInfo(recharge.getDataInfo());
        event.setStatus(recharge.getStatus().name());
        event.setCreatedAt(recharge.getCreatedAt());
        return event;
    }

    private RechargeRequestDto mapToDto(RechargeRequest recharge) {
        RechargeRequestDto dto = new RechargeRequestDto();
        dto.setId(recharge.getId());
        dto.setUsername(recharge.getUsername());
        dto.setMobileNumber(recharge.getMobileNumber());
        dto.setOperatorId(recharge.getOperatorId());
        dto.setOperatorName(recharge.getOperatorName());
        dto.setPlanId(recharge.getPlanId());
        dto.setPlanName(recharge.getPlanName());
        dto.setAmount(recharge.getAmount());
        dto.setValidity(recharge.getValidity());
        dto.setDataInfo(recharge.getDataInfo());
        dto.setStatus(recharge.getStatus().name());
        dto.setCreatedAt(recharge.getCreatedAt());
        dto.setUpdatedAt(recharge.getUpdatedAt());
        dto.setFailureReason(recharge.getFailureReason());
        return dto;
    }
}