package com.sapan.chat.connection.dto;

import com.sapan.chat.connection.model.ConnectionAction;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RespondToConnectionRequest(
		@NotBlank
		@Size(max = 64)
		@Pattern(regexp = "^[a-zA-Z0-9._-]+$")
		String username,
		@NotNull
		ConnectionAction action) {
}
