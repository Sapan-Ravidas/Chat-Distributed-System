package com.sapan.chat.connection.dto;

import java.time.Instant;
import java.util.UUID;

import com.sapan.chat.connection.model.ConnectionEntity;
import com.sapan.chat.connection.model.ConnectionStatus;

public record ConnectionResponse(
		UUID id,
		String requesterUsername,
		String recipientUsername,
		ConnectionStatus status,
		Instant createdAt,
		Instant updatedAt) {

	public static ConnectionResponse from(ConnectionEntity connection) {
		return new ConnectionResponse(
				connection.getId(),
				connection.getRequesterUsername(),
				connection.getRecipientUsername(),
				connection.getStatus(),
				connection.getCreatedAt(),
				connection.getUpdatedAt());
	}
}
