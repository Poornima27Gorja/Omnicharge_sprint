package com.omnicharge.payment_service.messaging;

import com.omnicharge.payment_service.dto.PaymentResultMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PaymentResultPublisher {

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Value("${rabbitmq.notification.queue}")
	private String notificationQueue;

	// Publish payment result to notification queue after processing
	public void publishPaymentResult(PaymentResultMessage message) {
		rabbitTemplate.convertAndSend(notificationQueue, message);
	}
}