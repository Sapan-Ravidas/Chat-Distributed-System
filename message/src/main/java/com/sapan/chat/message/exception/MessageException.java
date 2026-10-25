package com.sapan.chat.message.exception;

import org.springframework.http.HttpStatus;

public class MessageException extends RuntimeException {

	private final HttpStatus status;

	public MessageException(HttpStatus status, String message) {
		super(message);
		this.status = status;
	}

	public HttpStatus getStatus() {
		return status;
	}
}
