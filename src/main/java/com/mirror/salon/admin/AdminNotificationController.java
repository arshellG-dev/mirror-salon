package com.mirror.salon.admin;

import com.mirror.salon.users.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminNotificationController {

    private final NotificationRepository notificationRepository;

    @GetMapping("/admin/notifications")
    public String list(Model model) {
        model.addAttribute("notifications", notificationRepository.findAllByOrderByScheduledAtDesc());
        return "admin/notifications";
    }
}
