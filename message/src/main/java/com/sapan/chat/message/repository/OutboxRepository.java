package com.sapan.chat.message.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sapan.chat.message.model.OutboxEventEntity;
import com.sapan.chat.message.model.OutboxStatus;

public interface OutboxRepository extends JpaRepository<OutboxEventEntity, UUID> {

	List<OutboxEventEntity> findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
			OutboxStatus status, Instant now);
}
