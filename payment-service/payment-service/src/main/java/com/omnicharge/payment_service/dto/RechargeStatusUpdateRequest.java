package com.omnicharge.payment_service.dto;

public class RechargeStatusUpdateRequest {

	private String status;
	private String failureReason;

	public RechargeStatusUpdateRequest(String status, String failureReason) {
		this.status = status;
		this.failureReason = failureReason;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getFailureReason() {
		return failureReason;
	}

	public void setFailureReason(String failureReason) {
		this.failureReason = failureReason;
	}
}