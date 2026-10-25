package com.sapan.chat.notification.exception;

import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class NotificationExceptionHandler {

	@ExceptionHandler(NotificationException.class)
	ProblemDetail handleNotificationException(NotificationException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.getStatus(), exception.getMessage());
		problem.setTitle(exception.getStatus().getReasonPhrase());
		return problem;
	}
}
