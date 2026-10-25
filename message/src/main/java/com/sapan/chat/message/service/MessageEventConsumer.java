package com.sapan.chat.message.service;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sapan.chat.message.event.MessageCreatedEvent;

@Component
public class MessageEventConsumer {

	private static final Logger logger = LoggerFactory.getLogger(MessageEventConsumer.class);

	private final ObjectMapper objectMapper;
	private final SimpMessagingTemplate messagingTemplate;

	public MessageEventConsumer(ObjectMapper objectMapper, SimpMessagingTemplate messagingTemplate) {
		this.objectMapper = objectMapper;
		this.messagingTemplate = messagingTemplate;
	}

	@RabbitListener(queues = "#{messageEventsQueue.name}")
	public void broadcastToConnectedClients(Message message) throws IOException {
		MessageCreatedEvent event = objectMapper.readValue(message.getBody(), MessageCreatedEvent.class);
		messagingTemplate.convertAndSend(
				"/topic/conversations/" + event.conversationId(),
				event);
		logger.debug("Broadcast message {} for conversation {}", event.messageId(), event.conversationId());
	}
}
