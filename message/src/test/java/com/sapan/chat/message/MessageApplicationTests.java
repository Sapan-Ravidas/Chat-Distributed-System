package com.sapan.chat.message;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import com.sapan.chat.message.dto.ConversationResponse;
import com.sapan.chat.message.dto.MessageResponse;
import com.sapan.chat.message.exception.MessageException;
import com.sapan.chat.message.model.OutboxStatus;
import com.sapan.chat.message.model.OutboxEventEntity;
import com.sapan.chat.message.repository.ConversationRepository;
import com.sapan.chat.message.repository.MessageRepository;
import com.sapan.chat.message.repository.OutboxRepository;
import com.sapan.chat.message.service.MessageService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:message-test;DB_CLOSE_DELAY=-1",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.rabbitmq.listener.simple.auto-startup=false",
		"eureka.client.enabled=false",
		"chat.media.directory=target/test-images",
		"chat.outbox.poll-interval=3600000"
})
@AutoConfigureMockMvc
class MessageApplicationTests {

	@Autowired
	private MessageService messageService;

	@Autowired
	private ConversationRepository conversationRepository;

	@Autowired
	private MessageRepository messageRepository;

	@Autowired
	private OutboxRepository outboxRepository;

	@Autowired
	private MockMvc mockMvc;

	@BeforeEach
	void clearDatabase() {
		outboxRepository.deleteAll();
		messageRepository.deleteAll();
		conversationRepository.deleteAll();
	}

	@Test
	void contextLoads() {
	}

	@Test
	void createsGroupAndOrdersMessagesByServerSequence() {
		ConversationResponse conversation = messageService.createConversation(
				"Alice", "Weekend plans", Set.of("Bob", "Carol"));

		MessageResponse first = messageService.sendText(
				conversation.id(), "alice", "first", "client-1");
		MessageResponse second = messageService.sendText(
				conversation.id(), "alice", "second", "client-2");

		assertThat(second.sequenceNumber()).isGreaterThan(first.sequenceNumber());
		assertThat(messageService.getMessages(conversation.id(), "bob", Long.MAX_VALUE, 10))
				.extracting(MessageResponse::sequenceNumber)
				.containsExactly(1L, 2L);
		assertThat(outboxRepository.count()).isEqualTo(2);
	}

	@Test
	void idempotentRetryReturnsTheOriginalMessage() {
		UUID conversationId = createConversation();

		MessageResponse first = messageService.sendText(conversationId, "alice", "hello", "retry-key");
		MessageResponse retry = messageService.sendText(conversationId, "alice", "changed text", "retry-key");

		assertThat(retry.id()).isEqualTo(first.id());
		assertThat(retry.text()).isEqualTo("hello");
		assertThat(messageRepository.count()).isEqualTo(1);
		assertThat(outboxRepository.count()).isEqualTo(1);
	}

	@Test
	void outboxRetriesWithBackoffAndMarksExhaustedEventsFailed() {
		OutboxEventEntity event = new OutboxEventEntity("MessageCreated", "{}");
		var firstRetryAt = event.getNextAttemptAt();

		for (int attempt = 1; attempt <= 9; attempt++) {
			event.recordFailure("broker unavailable");
		}
		assertThat(event.getAttemptCount()).isEqualTo(9);
		assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
		assertThat(event.getNextAttemptAt()).isAfter(firstRetryAt);

		event.recordFailure("broker unavailable");
		assertThat(event.getAttemptCount()).isEqualTo(10);
		assertThat(event.getStatus()).isEqualTo(OutboxStatus.FAILED);
	}

	@Test
	void rejectsNonMembersAndSelfOnlyConversations() {
		UUID conversationId = createConversation();

		assertThatThrownBy(() -> messageService.sendText(conversationId, "mallory", "hello", "key"))
				.isInstanceOf(MessageException.class);
		assertThatThrownBy(() -> messageService.getMessages(conversationId, "mallory", Long.MAX_VALUE, 10))
				.isInstanceOf(MessageException.class);
		assertThatThrownBy(() -> messageService.createConversation("alice", null, Set.of()))
				.isInstanceOf(MessageException.class);
	}

	@Test
	void multipartImagesAreCompressedAndAvailableOnlyToMembers() throws Exception {
		UUID conversationId = createConversation();
		BufferedImage source = new BufferedImage(512, 512, BufferedImage.TYPE_INT_RGB);
		Random random = new Random(42);
		for (int y = 0; y < source.getHeight(); y++) {
			for (int x = 0; x < source.getWidth(); x++) {
				source.setRGB(x, y, random.nextInt(1 << 24));
			}
		}
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		ImageIO.write(source, "png", bytes);
		MockMultipartFile image = new MockMultipartFile(
				"image", "photo.png", MediaType.IMAGE_PNG_VALUE, bytes.toByteArray());

		MessageResponse response = messageService.sendImage(
				conversationId, "alice", "caption", "image-key", image);

		assertThat(response.type().name()).isEqualTo("IMAGE");
		assertThat(response.originalMediaType()).isEqualTo(MediaType.IMAGE_PNG_VALUE);
		assertThat(response.storedSize()).isLessThan(response.originalSize());
		assertThat(messageService.getImageMessage(response.id(), "bob").getImageKey()).isNotBlank();
		assertThat(messageService.readImage(
				messageService.getImageMessage(response.id(), "bob").getImageKey()))
				.hasSize((int) response.storedSize());
		assertThatThrownBy(() -> messageService.getImageMessage(response.id(), "mallory"))
				.isInstanceOf(MessageException.class);
	}

	@Test
	void multipartImageEndpointAcceptsAndServesCompressedImage() throws Exception {
		UUID conversationId = createConversation();
		BufferedImage source = new BufferedImage(32, 32, BufferedImage.TYPE_INT_RGB);
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		ImageIO.write(source, "png", bytes);
		MockMultipartFile image = new MockMultipartFile(
				"image", "photo.png", MediaType.IMAGE_PNG_VALUE, bytes.toByteArray());

		String response = mockMvc.perform(multipart(
								"/api/messages/conversations/{conversationId}/images", conversationId)
						.file(image)
						.param("senderUsername", "alice")
						.param("clientMessageId", "multipart-image-1")
						.param("caption", "photo"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.type").value("IMAGE"))
				.andReturn()
				.getResponse()
				.getContentAsString();
		UUID messageId = UUID.fromString(
				com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
						.readTree(response).get("id").asText());

		mockMvc.perform(get("/api/messages/{messageId}/image", messageId)
						.param("username", "bob"))
				.andExpect(status().isOk())
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
						.content().contentType(MediaType.IMAGE_JPEG));
	}

	@Test
	void httpApiCreatesConversationsAndServesHistory() throws Exception {
		String response = mockMvc.perform(post("/api/conversations")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"creatorUsername":"alice","name":"DM","memberUsernames":["bob"]}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.memberUsernames").isArray())
				.andReturn()
				.getResponse()
				.getContentAsString();
		UUID conversationId = UUID.fromString(
				com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
						.readTree(response).get("id").asText());

		mockMvc.perform(post("/api/messages/conversations/{id}", conversationId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"senderUsername":"alice","text":"hello socket chat","clientMessageId":"http-1"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.sequenceNumber").value(1));

		mockMvc.perform(get("/api/messages/conversations/{id}", conversationId)
						.param("username", "bob"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].text").value("hello socket chat"));
	}

	private UUID createConversation() {
		return messageService.createConversation("alice", null, Set.of("bob")).id();
	}
}
