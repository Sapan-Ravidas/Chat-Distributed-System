package com.sapan.chat.connection;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sapan.chat.connection.dto.ConnectionResponse;
import com.sapan.chat.connection.exception.ConnectionException;
import com.sapan.chat.connection.model.ConnectionAction;
import com.sapan.chat.connection.model.ConnectionStatus;
import com.sapan.chat.connection.repository.ConnectionRepository;
import com.sapan.chat.connection.service.ConnectionService;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:connection-test;DB_CLOSE_DELAY=-1",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"eureka.client.enabled=false"
})
@AutoConfigureMockMvc
class ConnectionApplicationTests {

	@Autowired
	private ConnectionService connectionService;

	@Autowired
	private ConnectionRepository connectionRepository;

	@Autowired
	private MockMvc mockMvc;

	@BeforeEach
	void clearConnections() {
		connectionRepository.deleteAll();
	}

	@Test
	void contextLoads() {
	}

	@Test
	void requestCanBeAcceptedAndRemovedByEitherParticipant() {
		ConnectionResponse request = connectionService.sendRequest("Alice", "Bob");

		assertThat(request.status()).isEqualTo(ConnectionStatus.PENDING);
		assertThat(connectionService.getOutgoingRequests("ALICE")).hasSize(1);
		assertThat(connectionService.getIncomingRequests("bob")).hasSize(1);

		ConnectionResponse accepted = connectionService.respond(
				request.id(), "BOB", ConnectionAction.ACCEPT);

		assertThat(accepted.status()).isEqualTo(ConnectionStatus.ACCEPTED);
		assertThat(connectionService.getConnections("alice"))
				.singleElement()
				.extracting(ConnectionResponse::status)
				.isEqualTo(ConnectionStatus.ACCEPTED);
		assertThat(connectionService.getConnections("bob"))
				.singleElement()
				.extracting(ConnectionResponse::status)
				.isEqualTo(ConnectionStatus.ACCEPTED);

		connectionService.removeConnection("bob", "alice");
		assertThat(connectionService.getConnections("alice")).isEmpty();
	}

	@Test
	void rejectsSelfRequestsAndDuplicateRequestsInEitherDirection() {
		assertThatThrownBy(() -> connectionService.sendRequest("alice", "ALICE"))
				.isInstanceOf(ConnectionException.class)
				.extracting("status")
				.isEqualTo(HttpStatus.BAD_REQUEST);

		connectionService.sendRequest("alice", "bob");

		assertThatThrownBy(() -> connectionService.sendRequest("bob", "alice"))
				.isInstanceOf(ConnectionException.class)
				.extracting("status")
				.isEqualTo(HttpStatus.CONFLICT);
	}

	@Test
	void onlyRecipientCanRespondAndOnlyRequesterCanCancel() {
		ConnectionResponse request = connectionService.sendRequest("alice", "bob");

		assertThatThrownBy(() -> connectionService.respond(
				request.id(), "alice", ConnectionAction.ACCEPT))
				.isInstanceOf(ConnectionException.class)
				.extracting("status")
				.isEqualTo(HttpStatus.FORBIDDEN);

		assertThatThrownBy(() -> connectionService.cancelRequest(request.id(), "bob"))
				.isInstanceOf(ConnectionException.class)
				.extracting("status")
				.isEqualTo(HttpStatus.FORBIDDEN);
	}

	@Test
	void cancelledAndDeclinedRequestsCanBeSentAgain() {
		ConnectionResponse cancelledRequest = connectionService.sendRequest("alice", "bob");
		connectionService.cancelRequest(cancelledRequest.id(), "alice");

		ConnectionResponse resentRequest = connectionService.sendRequest("alice", "bob");
		assertThat(resentRequest.status()).isEqualTo(ConnectionStatus.PENDING);

		connectionService.respond(resentRequest.id(), "bob", ConnectionAction.DECLINE);
		ConnectionResponse newRequest = connectionService.sendRequest("bob", "alice");

		assertThat(newRequest.status()).isEqualTo(ConnectionStatus.PENDING);
		assertThat(newRequest.requesterUsername()).isEqualTo("bob");
	}

	@Test
	void unknownRequestsReturnNotFound() {
		assertThatThrownBy(() -> connectionService.respond(
				UUID.randomUUID(), "alice", ConnectionAction.ACCEPT))
				.isInstanceOf(ConnectionException.class)
				.extracting("status")
				.isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void httpApiCreatesAndAcceptsConnectionRequests() throws Exception {
		mockMvc.perform(post("/api/connections/requests")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"requesterUsername":"alice","recipientUsername":"bob"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("PENDING"));

		ConnectionResponse request = connectionService.getIncomingRequests("bob").get(0);
		mockMvc.perform(patch("/api/connections/requests/{requestId}", request.id())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"bob","action":"ACCEPT"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ACCEPTED"));
	}
}
