package com.sapan.chat.message.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "chat_messages",
		indexes = @Index(name = "idx_messages_conversation_sequence", columnList = "conversation_id,sequence_number"),
		uniqueConstraints = @UniqueConstraint(
				name = "uk_message_client_id",
				columnNames = {"conversation_id", "sender_username", "client_message_id"}))
public class MessageEntity {

	@Id
	@GeneratedValue
	private UUID id;

	@Column(name = "conversation_id", nullable = false)
	private UUID conversationId;

	@Column(name = "sequence_number", nullable = false)
	private long sequenceNumber;

	@Column(name = "sender_username", nullable = false, length = 64)
	private String senderUsername;

	@Column(name = "client_message_id", nullable = false, length = 64)
	private String clientMessageId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private MessageType type;

	@Column(length = 5000)
	private String text;

	@Column(name = "image_key", length = 36)
	private String imageKey;

	@Column(name = "original_media_type", length = 64)
	private String originalMediaType;

	@Column(name = "original_size")
	private long originalSize;

	@Column(name = "stored_size")
	private long storedSize;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected MessageEntity() {
	}

	public MessageEntity(UUID conversationId, long sequenceNumber, String senderUsername,
			String clientMessageId, String text) {
		this.conversationId = conversationId;
		this.sequenceNumber = sequenceNumber;
		this.senderUsername = senderUsername;
		this.clientMessageId = clientMessageId;
		this.type = MessageType.TEXT;
		this.text = text;
		this.createdAt = Instant.now();
	}

	public MessageEntity(UUID conversationId, long sequenceNumber, String senderUsername,
			String clientMessageId, String caption, String imageKey, String originalMediaType,
			long originalSize, long storedSize) {
		this.conversationId = conversationId;
		this.sequenceNumber = sequenceNumber;
		this.senderUsername = senderUsername;
		this.clientMessageId = clientMessageId;
		this.type = MessageType.IMAGE;
		this.text = caption;
		this.imageKey = imageKey;
		this.originalMediaType = originalMediaType;
		this.originalSize = originalSize;
		this.storedSize = storedSize;
		this.createdAt = Instant.now();
	}

	public UUID getId() {
		return id;
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

	public String getClientMessageId() {
		return clientMessageId;
	}

	public MessageType getType() {
		return type;
	}

	public String getText() {
		return text;
	}

	public String getImageKey() {
		return imageKey;
	}

	public String getOriginalMediaType() {
		return originalMediaType;
	}

	public long getOriginalSize() {
		return originalSize;
	}

	public long getStoredSize() {
		return storedSize;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
