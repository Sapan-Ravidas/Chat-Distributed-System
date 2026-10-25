package com.sapan.chat.connection.service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sapan.chat.connection.dto.ConnectionResponse;
import com.sapan.chat.connection.exception.ConnectionException;
import com.sapan.chat.connection.model.ConnectionAction;
import com.sapan.chat.connection.model.ConnectionEntity;
import com.sapan.chat.connection.model.ConnectionStatus;
import com.sapan.chat.connection.repository.ConnectionRepository;

@Service
public class ConnectionService {

	private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]{1,64}$");

	private final ConnectionRepository repository;

	public ConnectionService(ConnectionRepository repository) {
		this.repository = repository;
	}

	@Transactional
	public ConnectionResponse sendRequest(String requester, String recipient) {
		String normalizedRequester = normalizeUsername(requester);
		String normalizedRecipient = normalizeUsername(recipient);

		if (normalizedRequester.equals(normalizedRecipient)) {
			throw new ConnectionException(HttpStatus.BAD_REQUEST, "You cannot connect with yourself.");
		}

		String pairKey = createPairKey(normalizedRequester, normalizedRecipient);
		ConnectionEntity connection = repository.findByPairKey(pairKey).orElse(null);

		if (connection != null) {
			if (connection.getStatus() == ConnectionStatus.ACCEPTED) {
				throw new ConnectionException(HttpStatus.CONFLICT, "These users are already connected.");
			}
			if (connection.getStatus() == ConnectionStatus.PENDING) {
				throw new ConnectionException(HttpStatus.CONFLICT,
						"A connection request for this pair is already pending.");
			}
			connection.restartRequest(normalizedRequester, normalizedRecipient);
			return ConnectionResponse.from(repository.save(connection));
		}

		connection = new ConnectionEntity(pairKey, normalizedRequester, normalizedRecipient);
		try {
			return ConnectionResponse.from(repository.saveAndFlush(connection));
		} catch (DataIntegrityViolationException exception) {
			throw new ConnectionException(HttpStatus.CONFLICT,
					"A connection request for this pair is already pending.");
		}
	}

	@Transactional(readOnly = true)
	public List<ConnectionResponse> getConnections(String username) {
		String normalizedUsername = normalizeUsername(username);
		return repository.findAllForUserWithStatus(normalizedUsername, ConnectionStatus.ACCEPTED)
				.stream()
				.map(ConnectionResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<ConnectionResponse> getIncomingRequests(String username) {
		String normalizedUsername = normalizeUsername(username);
		return repository.findByRecipientUsernameAndStatusOrderByCreatedAtDesc(
						normalizedUsername, ConnectionStatus.PENDING)
				.stream()
				.map(ConnectionResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<ConnectionResponse> getOutgoingRequests(String username) {
		String normalizedUsername = normalizeUsername(username);
		return repository.findByRequesterUsernameAndStatusOrderByCreatedAtDesc(
						normalizedUsername, ConnectionStatus.PENDING)
				.stream()
				.map(ConnectionResponse::from)
				.toList();
	}

	@Transactional
	public ConnectionResponse respond(UUID requestId, String username, ConnectionAction action) {
		String normalizedUsername = normalizeUsername(username);
		ConnectionEntity connection = findById(requestId);

		if (!connection.getRecipientUsername().equals(normalizedUsername)) {
			throw new ConnectionException(HttpStatus.FORBIDDEN,
					"Only the recipient can respond to this connection request.");
		}
		if (connection.getStatus() != ConnectionStatus.PENDING) {
			throw new ConnectionException(HttpStatus.CONFLICT, "This connection request is no longer pending.");
		}

		connection.setStatus(action == ConnectionAction.ACCEPT
				? ConnectionStatus.ACCEPTED
				: ConnectionStatus.DECLINED);
		return ConnectionResponse.from(repository.save(connection));
	}

	@Transactional
	public void cancelRequest(UUID requestId, String username) {
		String normalizedUsername = normalizeUsername(username);
		ConnectionEntity connection = findById(requestId);

		if (!connection.getRequesterUsername().equals(normalizedUsername)) {
			throw new ConnectionException(HttpStatus.FORBIDDEN,
					"Only the requester can cancel this connection request.");
		}
		if (connection.getStatus() != ConnectionStatus.PENDING) {
			throw new ConnectionException(HttpStatus.CONFLICT, "This connection request is no longer pending.");
		}

		connection.setStatus(ConnectionStatus.CANCELLED);
		repository.save(connection);
	}

	@Transactional
	public void removeConnection(String username, String otherUsername) {
		String normalizedUsername = normalizeUsername(username);
		String normalizedOtherUsername = normalizeUsername(otherUsername);
		if (normalizedUsername.equals(normalizedOtherUsername)) {
			throw new ConnectionException(HttpStatus.BAD_REQUEST, "A user cannot remove a connection with themselves.");
		}

		ConnectionEntity connection = repository.findByPairKey(
						createPairKey(normalizedUsername, normalizedOtherUsername))
				.orElseThrow(() -> new ConnectionException(HttpStatus.NOT_FOUND, "Connection not found."));

		if (connection.getStatus() != ConnectionStatus.ACCEPTED) {
			throw new ConnectionException(HttpStatus.CONFLICT, "These users are not connected.");
		}
		repository.delete(connection);
	}

	private ConnectionEntity findById(UUID requestId) {
		return repository.findById(requestId)
				.orElseThrow(() -> new ConnectionException(HttpStatus.NOT_FOUND, "Connection request not found."));
	}

	private String normalizeUsername(String username) {
		if (username == null || !USERNAME_PATTERN.matcher(username.trim()).matches()) {
			throw new ConnectionException(HttpStatus.BAD_REQUEST,
					"Username must be 1-64 characters using letters, numbers, '.', '_' or '-'.");
		}
		return username.trim().toLowerCase(Locale.ROOT);
	}

	private String createPairKey(String firstUsername, String secondUsername) {
		return List.of(firstUsername, secondUsername).stream()
				.sorted(Comparator.naturalOrder())
				.reduce((first, second) -> first + ":" + second)
				.orElseThrow();
	}
}
