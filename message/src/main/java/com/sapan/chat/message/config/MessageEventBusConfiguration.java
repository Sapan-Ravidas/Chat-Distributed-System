package com.sapan.chat.message.config;

import java.util.UUID;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessageEventBusConfiguration {

	public static final String MESSAGE_EVENTS_EXCHANGE = "chat.message.events";
	public static final String MESSAGE_EVENTS_DEAD_LETTER_EXCHANGE = "chat.message.events.dlx";

	@Bean
	FanoutExchange messageEventsExchange() {
		return new FanoutExchange(MESSAGE_EVENTS_EXCHANGE, true, false);
	}

	@Bean
	FanoutExchange messageEventsDeadLetterExchange() {
		return new FanoutExchange(MESSAGE_EVENTS_DEAD_LETTER_EXCHANGE, true, false);
	}

	@Bean
	Queue messageEventsQueue() {
		return QueueBuilder.nonDurable("message-service-" + UUID.randomUUID())
				.autoDelete()
				.withArgument("x-dead-letter-exchange", MESSAGE_EVENTS_DEAD_LETTER_EXCHANGE)
				.build();
	}

	@Bean
	Queue messageEventsDeadLetterQueue() {
		return QueueBuilder.durable("chat.message.events.dlq").build();
	}

	@Bean
	Binding messageEventsBinding(Queue messageEventsQueue, FanoutExchange messageEventsExchange) {
		return BindingBuilder.bind(messageEventsQueue).to(messageEventsExchange);
	}

	@Bean
	Binding messageEventsDeadLetterBinding(
			Queue messageEventsDeadLetterQueue,
			FanoutExchange messageEventsDeadLetterExchange) {
		return BindingBuilder.bind(messageEventsDeadLetterQueue).to(messageEventsDeadLetterExchange);
	}
}
