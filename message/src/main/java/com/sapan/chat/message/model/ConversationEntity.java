package com.sapan.chat.message.model;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "conversations")
public class ConversationEntity {

	@Id
	@GeneratedValue
	private UUID id;

	@Column(length = 100)
	private String name;

	@Column(name = "created_by", nullable = false, length = 64)
	private String createdBy;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "conversation_members", joinColumns = @JoinColumn(name = "conversation_id"))
	@Column(name = "username", nullable = false, length = 64)
	private Set<String> memberUsernames = new HashSet<>();

	@Column(name = "last_sequence", nullable = false)
	private long lastSequence;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected ConversationEntity() {
	}

	public ConversationEntity(String name, String createdBy, Set<String> memberUsernames) {
		this.name = name;
		this.createdBy = createdBy;
		this.memberUsernames = new HashSet<>(memberUsernames);
		this.memberUsernames.add(createdBy);
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

	public String getName() {
		return name;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public Set<String> getMemberUsernames() {
		return Set.copyOf(memberUsernames);
	}

	public long getLastSequence() {
		return lastSequence;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public long nextSequence() {
		updatedAt = Instant.now();
		return ++lastSequence;
	}

	public boolean hasMember(String username) {
		return memberUsernames.contains(username);
	}
}
