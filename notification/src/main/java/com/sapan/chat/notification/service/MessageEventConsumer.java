package com.sapan.chat.notification.service;

import java.io.IOException;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sapan.chat.notification.dto.MessageCreatedEvent;

@Component
public class MessageEventConsumer {

	private final ObjectMapper objectMapper;
	private final MessageNotificationService notificationService;

	public MessageEventConsumer(ObjectMapper objectMapper, MessageNotificationService notificationService) {
		this.objectMapper = objectMapper;
		this.notificationService = notificationService;
	}

	@RabbitListener(queues = "chat.notification.message-events")
	public void createNotifications(Message message) throws IOException {
		MessageCreatedEvent event = objectMapper.readValue(message.getBody(), MessageCreatedEvent.class);
		notificationService.handleMessageCreated(event);
	}
}
