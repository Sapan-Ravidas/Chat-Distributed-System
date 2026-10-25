package com.sapan.chat.message.dto;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import com.sapan.chat.message.model.ConversationEntity;

public record ConversationResponse(
		UUID id,
		String name,
		String createdBy,
		Set<String> memberUsernames,
		long lastSequence,
		Instant createdAt,
		Instant updatedAt) {

	public static ConversationResponse from(ConversationEntity conversation) {
		return new ConversationResponse(
				conversation.getId(),
				conversation.getName(),
				conversation.getCreatedBy(),
				conversation.getMemberUsernames(),
				conversation.getLastSequence(),
				conversation.getCreatedAt(),
				conversation.getUpdatedAt());
	}
}
