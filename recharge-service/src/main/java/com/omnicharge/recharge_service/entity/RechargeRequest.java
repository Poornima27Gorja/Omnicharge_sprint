package com.omnicharge.recharge_service.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "recharge_requests")
public class RechargeRequest {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String username;

	@Column(nullable = false)
	private String mobileNumber;

	@Column(nullable = false)
	private Long operatorId;

	@Column(nullable = false)
	private String operatorName;

	@Column(nullable = false)
	private Long planId;

	@Column(nullable = false)
	private String planName;

	@Column(nullable = false)
	private Double amount;

	@Column(nullable = false)
	private String validity;

	@Column(nullable = false)
	private String dataInfo;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private RechargeStatus status;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	@Column
	private LocalDateTime updatedAt;

	@Column
	private String failureReason;

	// Set timestamps before saving
	@PrePersist
	public void prePersist() {
		this.createdAt = LocalDateTime.now();
		this.status = RechargeStatus.PENDING;
	}

	@PreUpdate
	public void preUpdate() {
		this.updatedAt = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getMobileNumber() {
		return mobileNumber;
	}

	public void setMobileNumber(String mobileNumber) {
		this.mobileNumber = mobileNumber;
	}

	public Long getOperatorId() {
		return operatorId;
	}

	public void setOperatorId(Long operatorId) {
		this.operatorId = operatorId;
	}

	public String getOperatorName() {
		return operatorName;
	}

	public void setOperatorName(String operatorName) {
		this.operatorName = operatorName;
	}

	public Long getPlanId() {
		return planId;
	}

	public void setPlanId(Long planId) {
		this.planId = planId;
	}

	public String getPlanName() {
		return planName;
	}

	public void setPlanName(String planName) {
		this.planName = planName;
	}

	public Double getAmount() {
		return amount;
	}

	public void setAmount(Double amount) {
		this.amount = amount;
	}

	public String getValidity() {
		return validity;
	}

	public void setValidity(String validity) {
		this.validity = validity;
	}

	public String getDataInfo() {
		return dataInfo;
	}

	public void setDataInfo(String dataInfo) {
		this.dataInfo = dataInfo;
	}

	public RechargeStatus getStatus() {
		return status;
	}

	public void setStatus(RechargeStatus status) {
		this.status = status;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}

	public String getFailureReason() {
		return failureReason;
	}

	public void setFailureReason(String failureReason) {
		this.failureReason = failureReason;
	}
}
