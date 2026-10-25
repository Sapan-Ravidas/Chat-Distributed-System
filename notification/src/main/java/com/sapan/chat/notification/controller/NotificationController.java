package com.sapan.chat.notification.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sapan.chat.notification.dto.NotificationResponse;
import com.sapan.chat.notification.service.MessageNotificationService;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

	private final MessageNotificationService notificationService;

	public NotificationController(MessageNotificationService notificationService) {
		this.notificationService = notificationService;
	}

	@GetMapping
	public List<NotificationResponse> getForUser(@RequestParam String username) {
		return notificationService.getForUser(username);
	}

	@PatchMapping("/{id}/read")
	public NotificationResponse markRead(@PathVariable UUID id, @RequestParam String username) {
		return notificationService.markRead(id, username);
	}
}
