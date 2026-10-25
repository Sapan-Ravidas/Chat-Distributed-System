package com.sapan.chat.message.controller;

import java.util.List;
import java.util.UUID;

import java.net.URI;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.sapan.chat.message.dto.MessageResponse;
import com.sapan.chat.message.dto.SendMessageRequest;
import com.sapan.chat.message.model.MessageEntity;
import com.sapan.chat.message.service.MessageService;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

	private final MessageService messageService;

	public MessageController(MessageService messageService) {
		this.messageService = messageService;
	}

	@PostMapping("/conversations/{conversationId}")
	public ResponseEntity<MessageResponse> sendText(
			@PathVariable UUID conversationId,
			@Valid @RequestBody SendMessageRequest request) {
		MessageResponse response = messageService.sendText(
				conversationId,
				request.senderUsername(),
				request.text(),
				request.clientMessageId());
		return ResponseEntity.created(URI.create(
				"/api/messages/" + response.id())).body(response);
	}

	@GetMapping("/conversations/{conversationId}")
	public List<MessageResponse> history(
			@PathVariable UUID conversationId,
			@RequestParam String username,
			@RequestParam(defaultValue = "9223372036854775807") long beforeSequence,
			@RequestParam(defaultValue = "50") @Min(1) @Max(100) int limit) {
		return messageService.getMessages(conversationId, username, beforeSequence, limit);
	}

	@PostMapping(value = "/conversations/{conversationId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public MessageResponse uploadImage(
			@PathVariable UUID conversationId,
			@RequestParam String senderUsername,
			@RequestParam String clientMessageId,
			@RequestParam(required = false) String caption,
			@RequestParam MultipartFile image) {
		return messageService.sendImage(conversationId, senderUsername, caption, clientMessageId, image);
	}

	@GetMapping("/{messageId}/image")
	public ResponseEntity<byte[]> downloadImage(
			@PathVariable UUID messageId,
			@RequestParam String username) {
		MessageEntity imageMessage = messageService.getImageMessage(messageId, username);
		return ResponseEntity.ok()
				.contentType(MediaType.IMAGE_JPEG)
				.cacheControl(CacheControl.noCache())
				.header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + messageId + ".jpg\"")
				.body(messageService.readImage(imageMessage.getImageKey()));
	}
}
