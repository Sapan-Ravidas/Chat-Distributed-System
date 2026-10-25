package com.sapan.chat.connection.exception;

import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ConnectionExceptionHandler {

	@ExceptionHandler(ConnectionException.class)
	ProblemDetail handleConnectionException(ConnectionException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.getStatus(), exception.getMessage());
		problem.setTitle(exception.getStatus().getReasonPhrase());
		return problem;
	}
}
