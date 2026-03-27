package com.omnicharge.operator_service.dto;

import jakarta.validation.constraints.*;

public class RechargePlanRequest {

	@NotBlank(message = "Plan name is required")
	@Size(min = 2, max = 100, message = "Plan name must be between 2 and 100 characters")
	private String planName;

	@NotNull(message = "Price is required")
	@DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
	@DecimalMax(value = "10000.0", message = "Price cannot exceed Rs.10,000")
	private Double price;

	@NotBlank(message = "Validity is required (e.g. 28 Days, 84 Days)")
	private String validity;

	private String data;
	private String calls;
	private String sms;

	@Size(max = 500, message = "Description cannot exceed 500 characters")
	private String description;

	@NotBlank(message = "Category is required")
	@Pattern(regexp = "^(TOPUP|DATA|COMBO|SMS|ROAMING|INTERNATIONAL|SPECIAL)$", message = "Category must be one of: TOPUP, DATA, COMBO, SMS, ROAMING, INTERNATIONAL, SPECIAL")
	private String category;

	@NotBlank(message = "Status is required")
	@Pattern(regexp = "^(ACTIVE|INACTIVE)$", message = "Status must be either ACTIVE or INACTIVE")
	private String status;

	@NotNull(message = "Operator ID is required")
	@Positive(message = "Operator ID must be a positive number")
	private Long operatorId;

	public String getPlanName() {
		return planName;
	}

	public void setPlanName(String planName) {
		this.planName = planName;
	}

	public Double getPrice() {
		return price;
	}

	public void setPrice(Double price) {
		this.price = price;
	}

	public String getValidity() {
		return validity;
	}

	public void setValidity(String validity) {
		this.validity = validity;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public String getCalls() {
		return calls;
	}

	public void setCalls(String calls) {
		this.calls = calls;
	}

	public String getSms() {
		return sms;
	}

	public void setSms(String sms) {
		this.sms = sms;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Long getOperatorId() {
		return operatorId;
	}

	public void setOperatorId(Long operatorId) {
		this.operatorId = operatorId;
	}
}