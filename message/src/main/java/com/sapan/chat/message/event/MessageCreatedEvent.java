package com.sapan.chat.message.event;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import com.sapan.chat.message.model.MessageType;

public record MessageCreatedEvent(
		UUID eventId,
		UUID messageId,
		UUID conversationId,
		long sequenceNumber,
		String senderUsername,
		Set<String> recipients,
		MessageType type,
		String text,
		String imageUrl,
		Instant createdAt) {
}
