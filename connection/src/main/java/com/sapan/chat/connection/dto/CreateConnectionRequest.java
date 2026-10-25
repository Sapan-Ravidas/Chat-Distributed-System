package com.sapan.chat.connection.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateConnectionRequest(
		@NotBlank
		@Size(max = 64)
		@Pattern(regexp = "^[a-zA-Z0-9._-]+$")
		String requesterUsername,
		@NotBlank
		@Size(max = 64)
		@Pattern(regexp = "^[a-zA-Z0-9._-]+$")
		String recipientUsername) {
}
