package com.omnicharge.recharge_service.messaging;

import com.omnicharge.recharge_service.dto.RechargeEventMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RechargeEventPublisher {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.payment.queue}")
    private String paymentQueue;

    // Publish directly to payment queue - no exchange needed
    public void publishRechargeEvent(RechargeEventMessage message) {
        rabbitTemplate.convertAndSend(paymentQueue, message);
    }
}