package com.omnicharge.operator_service.repository;

import com.omnicharge.operator_service.entity.Operator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OperatorRepository extends JpaRepository<Operator, Long> {

    Optional<Operator> findByName(String name);

    boolean existsByName(String name);

    List<Operator> findByStatus(String status);

    List<Operator> findByType(String type);
}
