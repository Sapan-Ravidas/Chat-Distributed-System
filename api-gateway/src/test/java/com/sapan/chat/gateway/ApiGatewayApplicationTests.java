package com.sapan.chat.gateway;

import java.net.URI;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiGatewayApplicationTests {

	@Autowired
	private RouteLocator routeLocator;

	@Test
	void contextLoads() {
	}

	@Test
	void routesRequestsToRegisteredServices() {
		Map<String, URI> routeUris = routeLocator.getRoutes()
				.collectMap(Route::getId, Route::getUri)
				.block();

		assertThat(routeUris)
				.containsEntry("auth-service", URI.create("lb://auth"))
				.containsEntry("connection-service", URI.create("lb://connection"))
				.containsEntry("message-service", URI.create("lb://message"))
				.containsEntry("notification-service", URI.create("lb://notification"));
	}
}
