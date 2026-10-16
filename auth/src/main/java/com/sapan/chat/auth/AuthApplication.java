package com.sapan.chat.auth;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import com.sapan.chat.auth.utils.Utils;

@SpringBootApplication
public class AuthApplication {

	public static void main(String[] args) {
		System.setProperty(Utils.KEY_CLOUD_ENV, "production-asia-south-west");
		SpringApplication.run(AuthApplication.class, args);
	}

	@Bean 
	CommandLineRunner runner(Environment environment) {
		return new CommandLineRunner() {
			@Override
			public void run(String... args) throws Exception {
				System.out.println("App Bootstrap status: " + environment.getProperty(Utils.APP_BOOTSTRAP_STATUS));
				System.out.println("App Runtime Target: " + environment.getProperty(Utils.APP_RUNTIME_TARGET));	
			}
		};
	}
}
