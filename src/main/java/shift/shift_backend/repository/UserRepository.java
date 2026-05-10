package shift.shift_backend.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import shift.shift_backend.domain.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByPhone(String phone);
}
