package com.omnicharge.operator_service.repository;

import com.omnicharge.operator_service.entity.RechargePlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RechargePlanRepository extends JpaRepository<RechargePlan, Long> {

    List<RechargePlan> findByOperatorId(Long operatorId);

    List<RechargePlan> findByOperatorIdAndStatus(Long operatorId, String status);

    List<RechargePlan> findByCategory(String category);

    List<RechargePlan> findByStatus(String status);
}
