# Chat Distributed System

This starter contains four business services, a Eureka service registry, and a Spring Cloud Gateway. The message service persists conversations and messages, supports STOMP/WebSocket delivery and compressed image uploads, and publishes asynchronous message-created events through RabbitMQ to the notification service.

| Application | Port | Purpose |
| --- | ---: | --- |
| `service-discovery` | 8761 | Registers service instances; dashboard at `http://localhost:8761` |
| `api-gateway` | 8080 | Routes client requests to registered services |
| `auth` | 8081 | User and authentication APIs |
| `connection` | 8082 | Connection APIs |
| `message` | 8083 | Message APIs |
| `notification` | 8084 | Notification APIs |
| RabbitMQ | 5672 | Async message event bus; management UI at `http://localhost:15672` |

## Run locally

Start RabbitMQ and then each application in a separate terminal, in this order:

1. `docker compose up -d rabbitmq`
2. `service-discovery`
3. `auth`, `connection`, `message`, and `notification`
4. `api-gateway`

From the repository root, run each with the existing Maven wrapper:

```sh
./connection/mvnw -f service-discovery/pom.xml spring-boot:run
./connection/mvnw -f auth/pom.xml spring-boot:run
./connection/mvnw -f connection/pom.xml spring-boot:run
./connection/mvnw -f message/pom.xml spring-boot:run
./connection/mvnw -f notification/pom.xml spring-boot:run
./connection/mvnw -f api-gateway/pom.xml spring-boot:run
```

The gateway uses service names from Eureka and exposes these routes:

| Gateway path | Service |
| --- | --- |
| `/api/users/**` | `auth` |
| `/api/connections/**` | `connection` |
| `/api/conversations/**` and `/api/messages/**` | `message` |
| `/ws` (WebSocket) | `message` |
| `/api/notifications/**` | `notification` |

All services use `localhost` for local discovery by default.

## Connection service API

Connection relationships are stored in the connection service's local H2 file database (`connection/data/connections`). Usernames are normalized to lowercase and must contain 1-64 letters, numbers, periods, underscores, or hyphens.

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/api/connections/requests` | Send a request with `{"requesterUsername":"alice","recipientUsername":"bob"}` |
| `GET` | `/api/connections/{username}` | List accepted connections |
| `GET` | `/api/connections/requests/incoming?username=bob` | List pending incoming requests |
| `GET` | `/api/connections/requests/outgoing?username=alice` | List pending outgoing requests |
| `PATCH` | `/api/connections/requests/{requestId}` | Accept or decline with `{"username":"bob","action":"ACCEPT"}` |
| `DELETE` | `/api/connections/requests/{requestId}?username=alice` | Cancel a pending request as its requester |
| `DELETE` | `/api/connections/{username}/{otherUsername}` | Remove an accepted connection |

Requests move through `PENDING`, `ACCEPTED`, `DECLINED`, and `CANCELLED`. Only the recipient can accept or decline; only the requester can cancel. Duplicate pending requests in either direction are rejected, and cancelled or declined pairs can request again.

**Authentication note:** The current auth service does not yet issue validated identity tokens. The connection endpoints therefore accept usernames as input for this starter and are not an authorization boundary. Before exposing them to untrusted clients, derive the acting username from a validated token rather than trusting the request body or query parameter.

The local H2 database is suitable for development only. Use a shared production database such as PostgreSQL before running multiple connection-service instances.

## Messaging service

Conversation participants and message history are stored in the message service's local H2 file database (`message/data/messages`). A conversation supports 2-100 members and can be used for direct or group chat. Usernames are normalized to lowercase. For this starter, the caller supplies the username; this is not authentication.

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/api/conversations` | Create a chat: `{"creatorUsername":"alice","name":"Team","memberUsernames":["bob","carol"]}` |
| `GET` | `/api/conversations?username=alice` | List conversations for a member |
| `POST` | `/api/messages/conversations/{conversationId}` | Send text: `{"senderUsername":"alice","text":"Hello","clientMessageId":"unique-retry-key"}` |
| `GET` | `/api/messages/conversations/{conversationId}?username=alice` | Read history; optional `beforeSequence` and `limit` (1-100) |
| `POST` | `/api/messages/conversations/{conversationId}/images` | Upload multipart fields `senderUsername`, `clientMessageId`, optional `caption`, and `image` |
| `GET` | `/api/messages/{messageId}/image?username=alice` | Download an image as a conversation member |

Text can also be sent over STOMP WebSocket at `/ws`. Connect via the gateway at `ws://localhost:8080/ws` (or directly at port 8083) with a STOMP `CONNECT` native header named `username`, subscribe to `/topic/conversations/{conversationId}`, and send a JSON body to `/app/conversations/{conversationId}/messages`, for example:

```json
{"senderUsername":"alice","text":"Hello","clientMessageId":"client-generated-unique-id"}
```

Subscribe to `/user/queue/acks` for the sender's acknowledgement and `/user/queue/errors` for controller validation or membership errors. Socket subscriptions and sends are restricted to conversation members and the sender name must match the CONNECT username. The CONNECT username is self-asserted, not authenticated; this membership check is not a security boundary until token validation is added. Image content is uploaded through the multipart HTTP endpoint; the resulting message and image URL are broadcast to WebSocket subscribers.

Messages are assigned a monotonically increasing sequence number while holding a database lock on the conversation. Clients should order and paginate by `sequenceNumber`, not client timestamps or timestamps from different service instances. `createdAt` is server-generated display metadata. Each client should create a unique `clientMessageId` per sender/conversation and reuse it on retries; the service uses it for idempotency.

JPEG and PNG uploads up to 10 MB are decoded, resized to a maximum 1920-pixel edge, converted to JPEG at 82% quality, and stored under `message/data/images`. The response reports original and compressed sizes. This local directory is for development; multiple instances need shared object storage.

### Async event flow and reliability

The message service writes the message and a `MessageCreated` event to its database in the same transaction (transactional outbox). A publisher sends pending outbox events to the durable `chat.message.events` RabbitMQ fanout exchange and marks them published only after a broker publisher confirmation. Failed publishes back off and retry up to 10 times; exhausted events remain visible in the message database with `FAILED` status for inspection.

The notification service consumes the event asynchronously from its own durable queue and stores one idempotent notification per recipient. Fetch notifications with `GET /api/notifications?username=bob`; mark one read with `PATCH /api/notifications/{id}/read?username=bob`. Listener processing retries three times, then routes rejected events to the shared `chat.message.events.dlq`. Message-service instances each bind an ephemeral queue so RabbitMQ broadcasts new events to each instance's local WebSocket clients. Delivery is at-least-once: consumers and WebSocket clients should deduplicate using event/message IDs.

Inspect RabbitMQ at `http://localhost:15672` with the local-only starter credentials `chat` / `chat-local-dev`. Stop the broker with `docker compose down`; keep its named volume to retain local broker data. Change these development credentials before deploying outside a local machine.

**Production boundaries:** H2 and local image files are single-instance development storage. Before running multiple instances, use a shared database with migrations and shared object storage. The current auth service does not issue validated identity tokens, so message, connection, image, and notification APIs accept caller-supplied usernames. Add token validation and derive the acting username from the authenticated principal before exposing these APIs to untrusted clients. Configure WebSocket allowed origins for the frontend when it is deployed outside localhost.
