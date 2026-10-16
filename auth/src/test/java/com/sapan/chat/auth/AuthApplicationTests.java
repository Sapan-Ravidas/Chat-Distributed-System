package com.sapan.chat.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "CLOUD_ENV=test")
class AuthApplicationTests {

	@Autowired
	private Environment environment;

	@Test
	void contextLoads() {
	}

	@Test
	void initializerAddsDynamicProperties() {
		assertThat(environment.getProperty("app.bootstrap.status")).isEqualTo("VERIFIED");
		assertThat(environment.getProperty("app.runtime.target")).isEqualTo("test");
	}

}
