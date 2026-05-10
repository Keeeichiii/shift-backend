package shift.shift_backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserRoleWriteService {

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public void assignRole(Long userId, Long roleId) {
        jdbcTemplate.update("""
                insert into users_roles(user_id, role_id)
                values (?, ?)
                on conflict do nothing
                """, userId, roleId);
    }
}
