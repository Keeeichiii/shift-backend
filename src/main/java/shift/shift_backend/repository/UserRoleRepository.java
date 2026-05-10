package shift.shift_backend.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import shift.shift_backend.domain.entity.Role;

public interface UserRoleRepository extends JpaRepository<Role, Integer> {

    @Query(value = """
            select r.name
            from roles r
            join users_roles ur on ur.role_id = r.id
            where ur.user_id = :userId
            """, nativeQuery = true)
    List<String> findRoleNamesByUserId(Long userId);
}
