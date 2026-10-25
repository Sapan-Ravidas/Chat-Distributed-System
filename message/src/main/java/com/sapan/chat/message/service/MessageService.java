package com.sapan.chat.message.service;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sapan.chat.message.dto.ConversationResponse;
import com.sapan.chat.message.dto.MessageResponse;
import com.sapan.chat.message.event.MessageCreatedEvent;
import com.sapan.chat.message.exception.MessageException;
import com.sapan.chat.message.model.ConversationEntity;
import com.sapan.chat.message.model.MessageEntity;
import com.sapan.chat.message.model.OutboxEventEntity;
import com.sapan.chat.message.repository.ConversationRepository;
import com.sapan.chat.message.repository.MessageRepository;
import com.sapan.chat.message.repository.OutboxRepository;

@Service
public class MessageService {

	private static final int MAX_MEMBERS = 100;
	private static final int MAX_PAGE_SIZE = 100;
	private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]{1,64}$");

	private final ConversationRepository conversationRepository;
	private final MessageRepository messageRepository;
	private final OutboxRepository outboxRepository;
	private final ImageStorageService imageStorageService;
	private final ObjectMapper objectMapper;

	public MessageService(
			ConversationRepository conversationRepository,
			MessageRepository messageRepository,
			OutboxRepository outboxRepository,
			ImageStorageService imageStorageService,
			ObjectMapper objectMapper) {
		this.conversationRepository = conversationRepository;
		this.messageRepository = messageRepository;
		this.outboxRepository = outboxRepository;
		this.imageStorageService = imageStorageService;
		this.objectMapper = objectMapper;
	}

	@Transactional
	public ConversationResponse createConversation(String creator, String name, Set<String> members) {
		String normalizedCreator = normalizeUsername(creator);
		if (members == null || members.size() > MAX_MEMBERS - 1) {
			throw new MessageException(HttpStatus.BAD_REQUEST, "A conversation can have at most 100 members.");
		}

		Set<String> normalizedMembers = new HashSet<>();
		for (String member : members) {
			normalizedMembers.add(normalizeUsername(member));
		}
		normalizedMembers.add(normalizedCreator);
		if (normalizedMembers.size() < 2) {
			throw new MessageException(HttpStatus.BAD_REQUEST, "A conversation must have at least two members.");
		}

		String normalizedName = name == null || name.isBlank() ? null : name.trim();
		ConversationEntity conversation = conversationRepository.save(
				new ConversationEntity(normalizedName, normalizedCreator, normalizedMembers));
		return ConversationResponse.from(conversation);
	}

	@Transactional(readOnly = true)
	public List<ConversationResponse> getConversations(String username) {
		String normalizedUsername = normalizeUsername(username);
		return conversationRepository
				.findDistinctByMemberUsernamesContainingOrderByUpdatedAtDesc(normalizedUsername)
				.stream()
				.map(ConversationResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public boolean isConversationMember(UUID conversationId, String username) {
		String normalizedUsername = normalizeUsername(username);
		return conversationRepository.findById(conversationId)
				.map(conversation -> conversation.hasMember(normalizedUsername))
				.orElse(false);
	}

	@Transactional
	public MessageResponse sendText(UUID conversationId, String sender, String text, String clientMessageId) {
		String normalizedSender = normalizeUsername(sender);
		String normalizedClientId = validateClientMessageId(clientMessageId);
		if (text == null || text.isBlank() || text.length() > 5000) {
			throw new MessageException(HttpStatus.BAD_REQUEST, "Text must contain 1-5000 characters.");
		}

		ConversationEntity conversation = lockConversation(conversationId, normalizedSender);
		MessageEntity existing = messageRepository
				.findByConversationIdAndSenderUsernameAndClientMessageId(
						conversationId, normalizedSender, normalizedClientId)
				.orElse(null);
		if (existing != null) {
			return MessageResponse.from(existing);
		}
		MessageEntity message = messageRepository.saveAndFlush(new MessageEntity(
				conversationId,
				conversation.nextSequence(),
				normalizedSender,
				normalizedClientId,
				text.trim()));
		recordMessageCreated(conversation, message);
		return MessageResponse.from(message);
	}

	@Transactional
	public MessageResponse sendImage(
			UUID conversationId,
			String sender,
			String caption,
			String clientMessageId,
			MultipartFile image) {
		String normalizedSender = normalizeUsername(sender);
		String normalizedClientId = validateClientMessageId(clientMessageId);
		if (caption != null && caption.length() > 1000) {
			throw new MessageException(HttpStatus.BAD_REQUEST, "Image captions must be 1000 characters or shorter.");
		}

		ConversationEntity conversation = lockConversation(conversationId, normalizedSender);
		MessageEntity existing = messageRepository
				.findByConversationIdAndSenderUsernameAndClientMessageId(
						conversationId, normalizedSender, normalizedClientId)
				.orElse(null);
		if (existing != null) {
			return MessageResponse.from(existing);
		}
		ImageStorageService.StoredImage storedImage = imageStorageService.store(image);
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCompletion(int status) {
				if (status != STATUS_COMMITTED) {
					imageStorageService.delete(storedImage.imageKey());
				}
			}
		});
		MessageEntity message = messageRepository.saveAndFlush(new MessageEntity(
				conversationId,
				conversation.nextSequence(),
				normalizedSender,
				normalizedClientId,
				caption == null || caption.isBlank() ? null : caption.trim(),
				storedImage.imageKey(),
				storedImage.originalMediaType(),
				storedImage.originalSize(),
				storedImage.storedSize()));
		recordMessageCreated(conversation, message);
		return MessageResponse.from(message);
	}

	@Transactional(readOnly = true)
	public List<MessageResponse> getMessages(UUID conversationId, String username, long beforeSequence, int limit) {
		requireMember(conversationId, normalizeUsername(username));
		if (beforeSequence < 1) {
			throw new MessageException(HttpStatus.BAD_REQUEST, "beforeSequence must be greater than zero.");
		}
		if (limit < 1 || limit > MAX_PAGE_SIZE) {
			throw new MessageException(HttpStatus.BAD_REQUEST, "limit must be between 1 and 100.");
		}

		List<MessageResponse> messages = new ArrayList<>(messageRepository
				.findByConversationIdAndSequenceNumberLessThanOrderBySequenceNumberDesc(
						conversationId, beforeSequence, PageRequest.of(0, limit))
				.stream()
				.map(MessageResponse::from)
				.toList());
		Collections.reverse(messages);
		return List.copyOf(messages);
	}

	@Transactional(readOnly = true)
	public MessageEntity getImageMessage(UUID messageId, String username) {
		MessageEntity message = messageRepository.findById(messageId)
				.orElseThrow(() -> new MessageException(HttpStatus.NOT_FOUND, "Message not found."));
		requireMember(message.getConversationId(), normalizeUsername(username));
		if (message.getImageKey() == null) {
			throw new MessageException(HttpStatus.NOT_FOUND, "This message does not contain an image.");
		}
		return message;
	}

	public byte[] readImage(String imageKey) {
		try {
			return Files.readAllBytes(imageStorageService.resolve(imageKey));
		} catch (IOException exception) {
			throw new MessageException(HttpStatus.NOT_FOUND, "Image not found.");
		}
	}

	private ConversationEntity lockConversation(UUID conversationId, String sender) {
		ConversationEntity conversation = conversationRepository.findByIdForUpdate(conversationId)
				.orElseThrow(() -> new MessageException(HttpStatus.NOT_FOUND, "Conversation not found."));
		if (!conversation.hasMember(sender)) {
			throw new MessageException(HttpStatus.FORBIDDEN, "Only conversation members can send messages.");
		}
		return conversation;
	}

	private void requireMember(UUID conversationId, String username) {
		ConversationEntity conversation = conversationRepository.findById(conversationId)
				.orElseThrow(() -> new MessageException(HttpStatus.NOT_FOUND, "Conversation not found."));
		if (!conversation.hasMember(username)) {
			throw new MessageException(HttpStatus.FORBIDDEN, "Only conversation members can access messages.");
		}
	}

	private void recordMessageCreated(ConversationEntity conversation, MessageEntity message) {
		Set<String> recipients = new HashSet<>(conversation.getMemberUsernames());
		recipients.remove(message.getSenderUsername());
		MessageCreatedEvent event = new MessageCreatedEvent(
				UUID.randomUUID(),
				message.getId(),
				message.getConversationId(),
				message.getSequenceNumber(),
				message.getSenderUsername(),
				Set.copyOf(recipients),
				message.getType(),
				message.getText(),
				message.getImageKey() == null ? null : "/api/messages/" + message.getId() + "/image",
				message.getCreatedAt() == null ? Instant.now() : message.getCreatedAt());
		try {
			outboxRepository.save(new OutboxEventEntity(
					"MessageCreated",
					objectMapper.writeValueAsString(event)));
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("Failed to serialize message-created event.", exception);
		}
	}

	private String normalizeUsername(String username) {
		if (username == null || !USERNAME_PATTERN.matcher(username.trim()).matches()) {
			throw new MessageException(HttpStatus.BAD_REQUEST,
					"Username must be 1-64 characters using letters, numbers, '.', '_' or '-'.");
		}
		return username.trim().toLowerCase(Locale.ROOT);
	}

	private String validateClientMessageId(String clientMessageId) {
		if (clientMessageId == null || clientMessageId.isBlank() || clientMessageId.length() > 64) {
			throw new MessageException(HttpStatus.BAD_REQUEST,
					"clientMessageId must contain 1-64 characters and is required for safe retries.");
		}
		return clientMessageId.trim();
	}
}
