package com.omnicharge.operator_service.dto;

public class RechargePlanDto {

    private Long id;
    private String planName;
    private Double price;
    private String validity;
    private String data;
    private String calls;
    private String sms;
    private String description;
    private String category;
    private String status;
    private Long operatorId;
    private String operatorName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public String getValidity() { return validity; }
    public void setValidity(String validity) { this.validity = validity; }

    public String getData() { return data; }
    public void setData(String data) { this.data = data; }

    public String getCalls() { return calls; }
    public void setCalls(String calls) { this.calls = calls; }

    public String getSms() { return sms; }
    public void setSms(String sms) { this.sms = sms; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }

    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }
}
