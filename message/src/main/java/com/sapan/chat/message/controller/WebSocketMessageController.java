package com.sapan.chat.message.controller;

import java.security.Principal;
import java.util.UUID;
import java.util.Locale;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import org.springframework.http.HttpStatus;

import com.sapan.chat.message.dto.MessageResponse;
import com.sapan.chat.message.dto.WebSocketMessageRequest;
import com.sapan.chat.message.exception.MessageException;
import com.sapan.chat.message.service.MessageService;

import jakarta.validation.Valid;

@Controller
public class WebSocketMessageController {

	private final MessageService messageService;

	public WebSocketMessageController(MessageService messageService) {
		this.messageService = messageService;
	}

	@MessageMapping("/conversations/{conversationId}/messages")
	@SendToUser(value = "/queue/acks", broadcast = false)
	public MessageResponse sendText(
			@DestinationVariable UUID conversationId,
			@Valid @Payload WebSocketMessageRequest request,
			Principal principal) {
		if (principal == null
				|| !principal.getName().equals(request.senderUsername().trim().toLowerCase(Locale.ROOT))) {
			throw new MessageException(HttpStatus.FORBIDDEN, "The sender must match the connected socket username.");
		}
		return messageService.sendText(
				conversationId,
				request.senderUsername(),
				request.text(),
				request.clientMessageId());
	}

	@MessageExceptionHandler
	@SendToUser(value = "/queue/errors", broadcast = false)
	public SocketError handleMessageException(MessageException exception) {
		return new SocketError(exception.getStatus().value(), exception.getMessage());
	}

	public record SocketError(int status, String detail) {
	}
}
