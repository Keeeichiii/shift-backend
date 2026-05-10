package shift.shift_backend.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import shift.shift_backend.domain.entity.Transaction;
import shift.shift_backend.domain.enums.TransactionStatus;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findAllByUserIdAndStatus(Long userId, TransactionStatus status);
}
