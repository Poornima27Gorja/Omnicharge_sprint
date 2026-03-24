package com.omnicharge.operator_service.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "recharge_plans")
public class RechargePlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String planName;

    @Column(nullable = false)
    private Double price;

    @Column(nullable = false)
    private String validity;

    @Column
    private String data;

    @Column
    private String calls;

    @Column
    private String sms;

    @Column
    private String description;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String status;

    // Many plans belong to one operator
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operator_id", nullable = false)
    private Operator operator;

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

    public Operator getOperator() { return operator; }
    public void setOperator(Operator operator) { this.operator = operator; }
}
