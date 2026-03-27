package com.omnicharge.operator_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class OperatorRequest {

	@NotBlank(message = "Operator name is required")
	@Size(min = 2, max = 100, message = "Operator name must be between 2 and 100 characters")
	private String name;

	@NotBlank(message = "Operator type is required")
	@Pattern(regexp = "^(MOBILE|BROADBAND|DTH|LANDLINE)$", message = "Type must be one of: MOBILE, BROADBAND, DTH, LANDLINE")
	private String type;

	@NotBlank(message = "Status is required")
	@Pattern(regexp = "^(ACTIVE|INACTIVE)$", message = "Status must be either ACTIVE or INACTIVE")
	private String status;

	private String logoUrl;

	@Size(max = 500, message = "Description cannot exceed 500 characters")
	private String description;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getLogoUrl() {
		return logoUrl;
	}

	public void setLogoUrl(String logoUrl) {
		this.logoUrl = logoUrl;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}