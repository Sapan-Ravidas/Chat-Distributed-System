package com.sapan.chat.notification.service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sapan.chat.notification.dto.MessageCreatedEvent;
import com.sapan.chat.notification.dto.NotificationResponse;
import com.sapan.chat.notification.exception.NotificationException;
import com.sapan.chat.notification.model.MessageNotificationEntity;
import com.sapan.chat.notification.repository.MessageNotificationRepository;

@Service
public class MessageNotificationService {

	private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]{1,64}$");

	private final MessageNotificationRepository repository;

	public MessageNotificationService(MessageNotificationRepository repository) {
		this.repository = repository;
	}

	@Transactional
	public void handleMessageCreated(MessageCreatedEvent event) {
		for (String recipient : event.recipients()) {
			String normalizedRecipient = normalizeUsername(recipient);
			if (!repository.existsByEventIdAndRecipientUsername(event.eventId(), normalizedRecipient)) {
				repository.save(new MessageNotificationEntity(
						event.eventId(),
						event.messageId(),
						event.conversationId(),
						event.sequenceNumber(),
						normalizeUsername(event.senderUsername()),
						normalizedRecipient,
						createPreview(event),
						event.createdAt()));
			}
		}
	}

	@Transactional(readOnly = true)
	public List<NotificationResponse> getForUser(String username) {
		return repository.findTop100ByRecipientUsernameOrderByCreatedAtDesc(normalizeUsername(username))
				.stream()
				.map(NotificationResponse::from)
				.toList();
	}

	@Transactional
	public NotificationResponse markRead(UUID id, String username) {
		MessageNotificationEntity notification = repository.findByIdAndRecipientUsername(
						id, normalizeUsername(username))
				.orElseThrow(() -> new NotificationException(HttpStatus.NOT_FOUND, "Notification not found."));
		notification.markRead();
		return NotificationResponse.from(notification);
	}

	private String createPreview(MessageCreatedEvent event) {
		if ("IMAGE".equals(event.type())) {
			return event.text() == null || event.text().isBlank() ? "Image" : event.text().trim();
		}
		String text = event.text() == null ? "" : event.text().trim();
		return text.length() > 160 ? text.substring(0, 160) : text;
	}

	private String normalizeUsername(String username) {
		if (username == null || !USERNAME_PATTERN.matcher(username.trim()).matches()) {
			throw new NotificationException(HttpStatus.BAD_REQUEST, "Event contains an invalid username.");
		}
		return username.trim().toLowerCase(Locale.ROOT);
	}
}
