package shift.shift_backend.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import shift.shift_backend.domain.entity.Credential;

public interface CredentialRepository extends JpaRepository<Credential, Long> {
    Optional<Credential> findByUserId(Long userId);

    Optional<Credential> findByEmail(String email);
}
