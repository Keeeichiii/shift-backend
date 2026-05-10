package shift.shift_backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import shift.shift_backend.dto.panel.AdminPanelDto;
import shift.shift_backend.dto.panel.ModeratorPanelDto;
import shift.shift_backend.dto.panel.PanelUserReviewDto;
import shift.shift_backend.service.PanelService;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PanelController {

    private final PanelService panelService;

    @GetMapping("/admin/panel")
    public AdminPanelDto adminPanel() {
        return panelService.getAdminPanel();
    }

    @GetMapping("/moderator/panel")
    public ModeratorPanelDto moderatorPanel() {
        return panelService.getModeratorPanel();
    }

    @PostMapping("/moderator/users/{id}/approve")
    public PanelUserReviewDto approveUser(@PathVariable Long id) {
        return panelService.approveUser(id);
    }

    @PostMapping("/moderator/users/{id}/reject")
    public PanelUserReviewDto rejectUser(@PathVariable Long id) {
        return panelService.rejectUser(id);
    }
}
