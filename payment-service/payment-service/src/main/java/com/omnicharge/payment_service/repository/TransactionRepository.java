package com.omnicharge.payment_service.repository;

import com.omnicharge.payment_service.entity.Transaction;
import com.omnicharge.payment_service.entity.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByTransactionId(String transactionId);

    Optional<Transaction> findByRechargeId(Long rechargeId);

    List<Transaction> findByUsernameOrderByCreatedAtDesc(String username);

    List<Transaction> findAllByOrderByCreatedAtDesc();

    List<Transaction> findByStatus(TransactionStatus status);

    List<Transaction> findByMobileNumber(String mobileNumber);
}
