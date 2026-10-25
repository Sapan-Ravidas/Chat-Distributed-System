package com.sapan.chat.message.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "message_outbox")
public class OutboxEventEntity {

	@Id
	@GeneratedValue
	private UUID id;

	@Column(nullable = false, length = 64)
	private String eventType;

	@Lob
	@Column(nullable = false)
	private String payload;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private OutboxStatus status;

	@Column(name = "attempt_count", nullable = false)
	private int attemptCount;

	@Column(name = "next_attempt_at", nullable = false)
	private Instant nextAttemptAt;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "published_at")
	private Instant publishedAt;

	@Column(name = "last_error", length = 1000)
	private String lastError;

	protected OutboxEventEntity() {
	}

	public OutboxEventEntity(String eventType, String payload) {
		this.eventType = eventType;
		this.payload = payload;
		this.status = OutboxStatus.PENDING;
		this.attemptCount = 0;
		this.createdAt = Instant.now();
		this.nextAttemptAt = this.createdAt;
	}

	public UUID getId() {
		return id;
	}

	public String getEventType() {
		return eventType;
	}

	public String getPayload() {
		return payload;
	}

	public OutboxStatus getStatus() {
		return status;
	}

	public int getAttemptCount() {
		return attemptCount;
	}

	public Instant getNextAttemptAt() {
		return nextAttemptAt;
	}

	public void markPublished() {
		status = OutboxStatus.PUBLISHED;
		publishedAt = Instant.now();
		lastError = null;
	}

	public void recordFailure(String error) {
		attemptCount++;
		lastError = error.length() > 1000 ? error.substring(0, 1000) : error;
		if (attemptCount >= 10) {
			status = OutboxStatus.FAILED;
		} else {
			status = OutboxStatus.PENDING;
			nextAttemptAt = Instant.now().plusSeconds(Math.min(300, 1L << Math.min(attemptCount, 8)));
		}
	}
}
