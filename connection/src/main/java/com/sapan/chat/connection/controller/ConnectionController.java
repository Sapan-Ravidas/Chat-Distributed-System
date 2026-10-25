package com.sapan.chat.connection.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sapan.chat.connection.dto.ConnectionResponse;
import com.sapan.chat.connection.dto.CreateConnectionRequest;
import com.sapan.chat.connection.dto.RespondToConnectionRequest;
import com.sapan.chat.connection.service.ConnectionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/connections")
public class ConnectionController {

	private final ConnectionService connectionService;

	public ConnectionController(ConnectionService connectionService) {
		this.connectionService = connectionService;
	}

	@PostMapping("/requests")
	public ResponseEntity<ConnectionResponse> sendRequest(@Valid @RequestBody CreateConnectionRequest request) {
		ConnectionResponse response = connectionService.sendRequest(
				request.requesterUsername(), request.recipientUsername());
		return ResponseEntity.created(URI.create("/api/connections/requests/" + response.id())).body(response);
	}

	@GetMapping("/{username}")
	public List<ConnectionResponse> getConnections(@PathVariable String username) {
		return connectionService.getConnections(username);
	}

	@GetMapping("/requests/incoming")
	public List<ConnectionResponse> getIncomingRequests(@RequestParam String username) {
		return connectionService.getIncomingRequests(username);
	}

	@GetMapping("/requests/outgoing")
	public List<ConnectionResponse> getOutgoingRequests(@RequestParam String username) {
		return connectionService.getOutgoingRequests(username);
	}

	@PatchMapping("/requests/{requestId}")
	public ConnectionResponse respond(
			@PathVariable UUID requestId,
			@Valid @RequestBody RespondToConnectionRequest request) {
		return connectionService.respond(requestId, request.username(), request.action());
	}

	@DeleteMapping("/requests/{requestId}")
	public ResponseEntity<Void> cancelRequest(
			@PathVariable UUID requestId,
			@RequestParam String username) {
		connectionService.cancelRequest(requestId, username);
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/{username}/{otherUsername}")
	public ResponseEntity<Void> removeConnection(
			@PathVariable String username,
			@PathVariable String otherUsername) {
		connectionService.removeConnection(username, otherUsername);
		return ResponseEntity.noContent().build();
	}
}
