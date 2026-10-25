package com.sapan.chat.message.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sapan.chat.message.dto.ConversationResponse;
import com.sapan.chat.message.dto.CreateConversationRequest;
import com.sapan.chat.message.service.MessageService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

	private final MessageService messageService;

	public ConversationController(MessageService messageService) {
		this.messageService = messageService;
	}

	@PostMapping
	public ConversationResponse create(@Valid @RequestBody CreateConversationRequest request) {
		return messageService.createConversation(
				request.creatorUsername(), request.name(), request.memberUsernames());
	}

	@GetMapping
	public List<ConversationResponse> list(@RequestParam String username) {
		return messageService.getConversations(username);
	}
}
