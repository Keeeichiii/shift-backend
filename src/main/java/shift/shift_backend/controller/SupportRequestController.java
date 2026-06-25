package shift.shift_backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import shift.shift_backend.dto.support.CreateSupportRequest;
import shift.shift_backend.dto.support.SupportRequestDto;
import shift.shift_backend.service.SupportRequestService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/support-requests")
public class SupportRequestController {

    private final SupportRequestService supportRequestService;

    @PostMapping
    public SupportRequestDto create(
            Authentication authentication,
            @Valid @RequestBody CreateSupportRequest request
    ) {
        return supportRequestService.create(authentication, request);
    }

    @DeleteMapping("/{id}")
    public void deleteById(@PathVariable Long id) {
        supportRequestService.deleteById(id);
    }

    @DeleteMapping
    public void deleteAll() {
        supportRequestService.deleteAll();
    }
}
