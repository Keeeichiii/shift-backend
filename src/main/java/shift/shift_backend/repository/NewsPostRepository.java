package shift.shift_backend.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import shift.shift_backend.domain.entity.NewsPost;

public interface NewsPostRepository extends JpaRepository<NewsPost, Long> {
    List<NewsPost> findAllByPublishedTrueOrderByUpdatedAtDesc();

    Optional<NewsPost> findBySlug(String slug);
}
