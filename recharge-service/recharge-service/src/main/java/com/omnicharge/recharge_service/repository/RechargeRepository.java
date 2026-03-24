package com.omnicharge.recharge_service.repository;

import com.omnicharge.recharge_service.entity.RechargeRequest;
import com.omnicharge.recharge_service.entity.RechargeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RechargeRepository extends JpaRepository<RechargeRequest, Long> {

    // Get all recharges for a specific user
    List<RechargeRequest> findByUsernameOrderByCreatedAtDesc(String username);

    // Get all recharges for admin view
    List<RechargeRequest> findAllByOrderByCreatedAtDesc();

    // Get recharges by status
    List<RechargeRequest> findByStatus(RechargeStatus status);

    // Get recharges by username and status
    List<RechargeRequest> findByUsernameAndStatus(String username, RechargeStatus status);

    // Get recharges by mobile number
    List<RechargeRequest> findByMobileNumber(String mobileNumber);
}
