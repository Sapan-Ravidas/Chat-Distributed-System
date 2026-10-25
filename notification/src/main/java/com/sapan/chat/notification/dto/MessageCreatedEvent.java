package com.sapan.chat.notification.dto;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record MessageCreatedEvent(
		UUID eventId,
		UUID messageId,
		UUID conversationId,
		long sequenceNumber,
		String senderUsername,
		Set<String> recipients,
		String type,
		String text,
		String imageUrl,
		Instant createdAt) {
}
