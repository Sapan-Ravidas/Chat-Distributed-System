package com.sapan.chat.message.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sapan.chat.message.config.MessageEventBusConfiguration;
import com.sapan.chat.message.model.OutboxEventEntity;
import com.sapan.chat.message.model.OutboxStatus;
import com.sapan.chat.message.repository.OutboxRepository;

@Component
public class MessageOutboxPublisher {

	private static final Logger logger = LoggerFactory.getLogger(MessageOutboxPublisher.class);
	private static final int MAX_FAILURES = 10;

	private final OutboxRepository outboxRepository;
	private final RabbitTemplate rabbitTemplate;

	public MessageOutboxPublisher(OutboxRepository outboxRepository, RabbitTemplate rabbitTemplate) {
		this.outboxRepository = outboxRepository;
		this.rabbitTemplate = rabbitTemplate;
	}

	@Scheduled(fixedDelayString = "${chat.outbox.poll-interval:1000}")
	public void publishPendingEvents() {
		List<OutboxEventEntity> events = outboxRepository
				.findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
						OutboxStatus.PENDING, Instant.now());
		for (OutboxEventEntity event : events) {
			publish(event);
		}
	}

	void publish(OutboxEventEntity event) {
		try {
			CorrelationData correlationData = new CorrelationData(event.getId().toString());
			Message message = MessageBuilder
					.withBody(event.getPayload().getBytes(StandardCharsets.UTF_8))
					.setContentType("application/json")
					.setMessageId(event.getId().toString())
					.setType(event.getEventType())
					.build();
			rabbitTemplate.send(
					MessageEventBusConfiguration.MESSAGE_EVENTS_EXCHANGE,
					"",
					message,
					correlationData);
			CorrelationData.Confirm confirm = correlationData.getFuture().get(5, TimeUnit.SECONDS);
			if (!confirm.isAck()) {
				throw new IllegalStateException("RabbitMQ rejected event: " + confirm.getReason());
			}
			if (correlationData.getReturned() != null) {
				throw new IllegalStateException("RabbitMQ returned an unroutable message.");
			}

			event.markPublished();
			outboxRepository.save(event);
		} catch (Exception exception) {
			event.recordFailure(exception.getMessage() == null
					? exception.getClass().getSimpleName()
					: exception.getMessage());
			outboxRepository.save(event);
			if (event.getAttemptCount() >= MAX_FAILURES) {
				logger.error("Message event {} failed after {} attempts and was moved to FAILED.",
						event.getId(), event.getAttemptCount(), exception);
			} else {
				logger.warn("Message event {} publish failed on attempt {}; retrying.",
						event.getId(), event.getAttemptCount(), exception);
			}
		}
	}
}
