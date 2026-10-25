package com.sapan.chat.message.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.sapan.chat.message.model.MessageEntity;

public interface MessageRepository extends JpaRepository<MessageEntity, UUID> {

	Optional<MessageEntity> findByConversationIdAndSenderUsernameAndClientMessageId(
			UUID conversationId, String senderUsername, String clientMessageId);

	List<MessageEntity> findByConversationIdAndSequenceNumberLessThanOrderBySequenceNumberDesc(
			UUID conversationId, long beforeSequence, Pageable pageable);
}
