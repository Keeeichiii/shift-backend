package shift.shift_backend.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import shift.shift_backend.dto.user.AdminUserManagementRequest;
import shift.shift_backend.dto.user.AdminLicenseUpdateRequest;
import shift.shift_backend.dto.user.CreateUserRequest;
import shift.shift_backend.dto.user.UpdateUserRequest;
import shift.shift_backend.dto.user.UserDto;
import shift.shift_backend.service.UserService;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public List<UserDto> getAll() {
        return userService.getAll();
    }

    @GetMapping("/{id}")
    public UserDto getById(@PathVariable Long id) {
        return userService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto create(@Valid @RequestBody CreateUserRequest request) {
        return userService.create(request);
    }

    @PutMapping("/{id}")
    public UserDto update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return userService.update(id, request);
    }

    @PutMapping("/{id}/management")
    public UserDto manage(@PathVariable Long id, @Valid @RequestBody AdminUserManagementRequest request) {
        return userService.manageByAdmin(id, request);
    }

    @PutMapping("/{id}/license")
    public UserDto updateLicenseData(@PathVariable Long id, @Valid @RequestBody AdminLicenseUpdateRequest request) {
        return userService.updateLicenseData(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        userService.delete(id);
    }
}
