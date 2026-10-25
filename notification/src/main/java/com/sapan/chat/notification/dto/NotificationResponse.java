package com.sapan.chat.notification.dto;

import java.time.Instant;
import java.util.UUID;

import com.sapan.chat.notification.model.MessageNotificationEntity;

public record NotificationResponse(
		UUID id,
		UUID messageId,
		UUID conversationId,
		long sequenceNumber,
		String senderUsername,
		String preview,
		Instant createdAt,
		Instant readAt) {

	public static NotificationResponse from(MessageNotificationEntity notification) {
		return new NotificationResponse(
				notification.getId(),
				notification.getMessageId(),
				notification.getConversationId(),
				notification.getSequenceNumber(),
				notification.getSenderUsername(),
				notification.getPreview(),
				notification.getCreatedAt(),
				notification.getReadAt());
	}
}
