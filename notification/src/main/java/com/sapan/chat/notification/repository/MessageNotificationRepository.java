package com.sapan.chat.notification.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sapan.chat.notification.model.MessageNotificationEntity;

public interface MessageNotificationRepository extends JpaRepository<MessageNotificationEntity, UUID> {

	List<MessageNotificationEntity> findTop100ByRecipientUsernameOrderByCreatedAtDesc(String recipientUsername);

	Optional<MessageNotificationEntity> findByIdAndRecipientUsername(UUID id, String recipientUsername);

	boolean existsByEventIdAndRecipientUsername(UUID eventId, String recipientUsername);
}
