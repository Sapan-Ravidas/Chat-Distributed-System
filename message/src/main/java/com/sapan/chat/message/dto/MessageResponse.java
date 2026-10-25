package com.sapan.chat.message.dto;

import java.time.Instant;
import java.util.UUID;

import com.sapan.chat.message.model.MessageEntity;
import com.sapan.chat.message.model.MessageType;

public record MessageResponse(
		UUID id,
		UUID conversationId,
		long sequenceNumber,
		String senderUsername,
		String clientMessageId,
		MessageType type,
		String text,
		String imageUrl,
		String originalMediaType,
		long originalSize,
		long storedSize,
		Instant createdAt) {

	public static MessageResponse from(MessageEntity message) {
		String imageUrl = message.getImageKey() == null
				? null
				: "/api/messages/" + message.getId() + "/image";
		return new MessageResponse(
				message.getId(),
				message.getConversationId(),
				message.getSequenceNumber(),
				message.getSenderUsername(),
				message.getClientMessageId(),
				message.getType(),
				message.getText(),
				imageUrl,
				message.getOriginalMediaType(),
				message.getOriginalSize(),
				message.getStoredSize(),
				message.getCreatedAt());
	}
}
