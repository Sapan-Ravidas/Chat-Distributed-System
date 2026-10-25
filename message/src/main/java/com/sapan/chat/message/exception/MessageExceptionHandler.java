package com.sapan.chat.message.exception;

import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class MessageExceptionHandler {

	@ExceptionHandler(MessageException.class)
	ProblemDetail handleMessageException(MessageException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.getStatus(), exception.getMessage());
		problem.setTitle(exception.getStatus().getReasonPhrase());
		return problem;
	}
}
