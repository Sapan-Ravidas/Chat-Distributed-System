package com.sapan.chat.connection.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "connections", indexes = {
		@Index(name = "idx_connections_recipient_status", columnList = "recipient_username,status"),
		@Index(name = "idx_connections_requester_status", columnList = "requester_username,status")
})
public class ConnectionEntity {

	@Id
	@GeneratedValue
	private UUID id;

	@Column(name = "pair_key", nullable = false, unique = true, length = 129)
	private String pairKey;

	@Column(name = "requester_username", nullable = false, length = 64)
	private String requesterUsername;

	@Column(name = "recipient_username", nullable = false, length = 64)
	private String recipientUsername;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private ConnectionStatus status;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected ConnectionEntity() {
	}

	public ConnectionEntity(String pairKey, String requesterUsername, String recipientUsername) {
		this.pairKey = pairKey;
		this.requesterUsername = requesterUsername;
		this.recipientUsername = recipientUsername;
		this.status = ConnectionStatus.PENDING;
	}

	@PrePersist
	void onCreate() {
		Instant now = Instant.now();
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public String getRequesterUsername() {
		return requesterUsername;
	}

	public String getRecipientUsername() {
		return recipientUsername;
	}

	public ConnectionStatus getStatus() {
		return status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void restartRequest(String requesterUsername, String recipientUsername) {
		this.requesterUsername = requesterUsername;
		this.recipientUsername = recipientUsername;
		this.status = ConnectionStatus.PENDING;
		this.createdAt = Instant.now();
		this.updatedAt = this.createdAt;
	}

	public void setStatus(ConnectionStatus status) {
		this.status = status;
		this.updatedAt = Instant.now();
	}
}
