package com.sapan.chat.message.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sapan.chat.message.model.ConversationEntity;

import jakarta.persistence.LockModeType;

public interface ConversationRepository extends JpaRepository<ConversationEntity, UUID> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select conversation from ConversationEntity conversation where conversation.id = :id")
	Optional<ConversationEntity> findByIdForUpdate(@Param("id") UUID id);

	List<ConversationEntity> findDistinctByMemberUsernamesContainingOrderByUpdatedAtDesc(String username);
}
