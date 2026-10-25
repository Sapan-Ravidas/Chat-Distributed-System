package com.sapan.chat.message.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record WebSocketMessageRequest(
		@NotBlank
		@Size(max = 64)
		@Pattern(regexp = "^[a-zA-Z0-9._-]+$")
		String senderUsername,
		@NotBlank
		@Size(max = 5000)
		String text,
		@NotBlank
		@Size(max = 64)
		String clientMessageId) {
}
