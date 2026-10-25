package com.sapan.chat.message.config;

import java.security.Principal;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import com.sapan.chat.message.exception.MessageException;
import com.sapan.chat.message.service.MessageService;

@Component
public class WebSocketAuthorizationInterceptor implements ChannelInterceptor {

	private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]{1,64}$");
	private static final String CONVERSATION_TOPIC_PREFIX = "/topic/conversations/";
	private static final String APPLICATION_CONVERSATION_PREFIX = "/app/conversations/";

	private final MessageService messageService;

	public WebSocketAuthorizationInterceptor(MessageService messageService) {
		this.messageService = messageService;
	}

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
		if (accessor == null || accessor.getCommand() == null) {
			return message;
		}

		if (accessor.getCommand() == StompCommand.CONNECT) {
			String username = accessor.getFirstNativeHeader("username");
			if (username == null || !USERNAME_PATTERN.matcher(username.trim()).matches()) {
				throw new MessageException(HttpStatus.UNAUTHORIZED,
						"Connect with a username STOMP header to use chat sockets.");
			}
			String normalizedUsername = username.trim().toLowerCase(Locale.ROOT);
			Principal principal = () -> normalizedUsername;
			accessor.setUser(principal);
			return message;
		}

		if (accessor.getCommand() == StompCommand.SUBSCRIBE) {
			String destination = accessor.getDestination();
			if ("/user/queue/acks".equals(destination) || "/user/queue/errors".equals(destination)) {
				requireConnectedUser(accessor);
				return message;
			}
			UUID conversationId = conversationId(destination, CONVERSATION_TOPIC_PREFIX);
			String username = requireConnectedUser(accessor).getName();
			requireMember(conversationId, username);
			return message;
		}

		if (accessor.getCommand() == StompCommand.SEND) {
			UUID conversationId = conversationId(accessor.getDestination(), APPLICATION_CONVERSATION_PREFIX,
					"/messages");
			String username = requireConnectedUser(accessor).getName();
			requireMember(conversationId, username);
			return message;
		}

		return message;
	}

	private Principal requireConnectedUser(StompHeaderAccessor accessor) {
		if (accessor.getUser() == null) {
			throw new MessageException(HttpStatus.UNAUTHORIZED, "Connect with a username before using chat sockets.");
		}
		return accessor.getUser();
	}

	private UUID conversationId(String destination, String prefix) {
		return conversationId(destination, prefix, "");
	}

	private UUID conversationId(String destination, String prefix, String suffix) {
		if (destination == null || !destination.startsWith(prefix) || !destination.endsWith(suffix)) {
			throw new MessageException(HttpStatus.FORBIDDEN, "This WebSocket destination is not allowed.");
		}
		String conversationId = destination.substring(prefix.length(), destination.length() - suffix.length());
		try {
			return UUID.fromString(conversationId);
		} catch (IllegalArgumentException exception) {
			throw new MessageException(HttpStatus.BAD_REQUEST, "Invalid conversation ID.");
		}
	}

	private void requireMember(UUID conversationId, String username) {
		if (!messageService.isConversationMember(conversationId, username)) {
			throw new MessageException(HttpStatus.FORBIDDEN, "Only conversation members can use this socket.");
		}
	}
}
