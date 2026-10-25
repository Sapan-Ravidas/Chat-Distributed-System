package com.sapan.chat.notification;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.sapan.chat.notification.dto.MessageCreatedEvent;
import com.sapan.chat.notification.dto.NotificationResponse;
import com.sapan.chat.notification.repository.MessageNotificationRepository;
import com.sapan.chat.notification.service.MessageNotificationService;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:notification-test;DB_CLOSE_DELAY=-1",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.rabbitmq.listener.simple.auto-startup=false",
		"eureka.client.enabled=false"
})
class NotificationApplicationTests {

	@Autowired
	private MessageNotificationService notificationService;

	@Autowired
	private MessageNotificationRepository notificationRepository;

	@BeforeEach
	void clearNotifications() {
		notificationRepository.deleteAll();
	}

	@Test
	void contextLoads() {
	}

	@Test
	void messageEventsCreateIdempotentRecipientNotifications() {
		UUID eventId = UUID.randomUUID();
		MessageCreatedEvent event = new MessageCreatedEvent(
				eventId,
				UUID.randomUUID(),
				UUID.randomUUID(),
				1,
				"alice",
				Set.of("bob", "carol"),
				"TEXT",
				"Hello group",
				null,
				Instant.now());

		notificationService.handleMessageCreated(event);
		notificationService.handleMessageCreated(event);

		assertThat(notificationRepository.count()).isEqualTo(2);
		NotificationResponse notification = notificationService.getForUser("BOB").get(0);
		assertThat(notification.preview()).isEqualTo("Hello group");
		assertThat(notification.readAt()).isNull();

		NotificationResponse read = notificationService.markRead(notification.id(), "bob");
		assertThat(read.readAt()).isNotNull();
	}
}
