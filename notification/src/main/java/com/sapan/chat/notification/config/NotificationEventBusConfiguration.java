package com.sapan.chat.notification.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NotificationEventBusConfiguration {

	@Bean
	FanoutExchange messageEventsExchange() {
		return new FanoutExchange("chat.message.events", true, false);
	}

	@Bean
	FanoutExchange messageEventsDeadLetterExchange() {
		return new FanoutExchange("chat.message.events.dlx", true, false);
	}

	@Bean
	Queue notificationMessageEventsQueue() {
		return QueueBuilder.durable("chat.notification.message-events")
				.withArgument("x-dead-letter-exchange", "chat.message.events.dlx")
				.build();
	}

	@Bean
	Queue notificationDeadLetterQueue() {
		return QueueBuilder.durable("chat.message.events.dlq").build();
	}

	@Bean
	Binding notificationMessageEventsBinding(
			Queue notificationMessageEventsQueue,
			FanoutExchange messageEventsExchange) {
		return BindingBuilder.bind(notificationMessageEventsQueue).to(messageEventsExchange);
	}

	@Bean
	Binding notificationDeadLetterBinding(
			Queue notificationDeadLetterQueue,
			FanoutExchange messageEventsDeadLetterExchange) {
		return BindingBuilder.bind(notificationDeadLetterQueue).to(messageEventsDeadLetterExchange);
	}
}
