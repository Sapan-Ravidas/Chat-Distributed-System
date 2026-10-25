package com.sapan.chat.notification.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "message_notifications",
		indexes = @Index(name = "idx_notifications_recipient_created", columnList = "recipient_username,created_at"),
		uniqueConstraints = @UniqueConstraint(
				name = "uk_notification_event_recipient",
				columnNames = {"event_id", "recipient_username"}))
public class MessageNotificationEntity {

	@Id
	@GeneratedValue
	private UUID id;

	@Column(name = "event_id", nullable = false)
	private UUID eventId;

	@Column(name = "message_id", nullable = false)
	private UUID messageId;

	@Column(name = "conversation_id", nullable = false)
	private UUID conversationId;

	@Column(name = "sequence_number", nullable = false)
	private long sequenceNumber;

	@Column(name = "sender_username", nullable = false, length = 64)
	private String senderUsername;

	@Column(name = "recipient_username", nullable = false, length = 64)
	private String recipientUsername;

	@Column(name = "preview", length = 160)
	private String preview;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "read_at")
	private Instant readAt;

	protected MessageNotificationEntity() {
	}

	public MessageNotificationEntity(
			UUID eventId,
			UUID messageId,
			UUID conversationId,
			long sequenceNumber,
			String senderUsername,
			String recipientUsername,
			String preview,
			Instant createdAt) {
		this.eventId = eventId;
		this.messageId = messageId;
		this.conversationId = conversationId;
		this.sequenceNumber = sequenceNumber;
		this.senderUsername = senderUsername;
		this.recipientUsername = recipientUsername;
		this.preview = preview;
		this.createdAt = createdAt;
	}

	public UUID getId() {
		return id;
	}

	public UUID getEventId() {
		return eventId;
	}

	public UUID getMessageId() {
		return messageId;
	}

	public UUID getConversationId() {
		return conversationId;
	}

	public long getSequenceNumber() {
		return sequenceNumber;
	}

	public String getSenderUsername() {
		return senderUsername;
	}

	public String getRecipientUsername() {
		return recipientUsername;
	}

	public String getPreview() {
		return preview;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getReadAt() {
		return readAt;
	}

	public void markRead() {
		if (readAt == null) {
			readAt = Instant.now();
		}
	}
}
