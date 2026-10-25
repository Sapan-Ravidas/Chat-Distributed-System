package com.sapan.chat.message.dto;

import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateConversationRequest(
		@NotBlank
		@Size(max = 64)
		@Pattern(regexp = "^[a-zA-Z0-9._-]+$")
		String creatorUsername,
		@Size(max = 100)
		String name,
		@NotEmpty
		@Size(max = 99)
		Set<@NotBlank @Size(max = 64) @Pattern(regexp = "^[a-zA-Z0-9._-]+$") String> memberUsernames) {
}
