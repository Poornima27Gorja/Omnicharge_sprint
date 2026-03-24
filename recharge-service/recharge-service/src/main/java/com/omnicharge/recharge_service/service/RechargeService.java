package com.omnicharge.recharge_service.service;

import com.omnicharge.recharge_service.dto.InitiateRechargeRequest;
import com.omnicharge.recharge_service.dto.RechargeRequestDto;
import com.omnicharge.recharge_service.dto.RechargeStatusUpdateRequest;

import java.util.List;

public interface RechargeService {

    RechargeRequestDto initiateRecharge(String username,
                                        String authToken,
                                        InitiateRechargeRequest request);

    RechargeRequestDto updateRechargeStatus(Long rechargeId,
                                             RechargeStatusUpdateRequest request);

    List<RechargeRequestDto> getMyRechargeHistory(String username);

    RechargeRequestDto getRechargeById(Long id, String username, boolean isAdmin);

    List<RechargeRequestDto> getAllRecharges();

    List<RechargeRequestDto> getRechargesByStatus(String status);

    List<RechargeRequestDto> getRechargesByMobile(String mobileNumber);
}