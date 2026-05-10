package shift.shift_backend.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import shift.shift_backend.domain.entity.NewsPost;
import shift.shift_backend.dto.news.CreateNewsRequest;
import shift.shift_backend.dto.news.NewsDto;
import shift.shift_backend.dto.news.UpdateNewsRequest;
import shift.shift_backend.repository.NewsPostRepository;

@Service
@RequiredArgsConstructor
public class NewsService {

    private final NewsPostRepository newsPostRepository;

    @Transactional(readOnly = true)
    public List<NewsDto> getPublishedNews() {
        return newsPostRepository.findAllByPublishedTrueOrderByUpdatedAtDesc().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NewsDto> getAllNews() {
        return newsPostRepository.findAll().stream()
                .sorted((left, right) -> right.getUpdatedAt().compareTo(left.getUpdatedAt()))
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public NewsDto create(CreateNewsRequest request) {
        newsPostRepository.findBySlug(request.slug().trim()).ifPresent(existing -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Новость с таким slug уже существует.");
        });

        NewsPost newsPost = new NewsPost();
        apply(newsPost, request);
        return toDto(newsPostRepository.save(newsPost));
    }

    @Transactional
    public NewsDto update(Long id, UpdateNewsRequest request) {
        NewsPost newsPost = getEntityById(id);
        newsPostRepository.findBySlug(request.slug().trim()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Новость с таким slug уже существует.");
            }
        });

        apply(newsPost, request);
        return toDto(newsPostRepository.save(newsPost));
    }

    @Transactional
    public void delete(Long id) {
        newsPostRepository.delete(getEntityById(id));
    }

    private void apply(NewsPost newsPost, CreateNewsRequest request) {
        newsPost.setTitle(request.title().trim());
        newsPost.setSlug(request.slug().trim());
        newsPost.setSummary(request.summary().trim());
        newsPost.setContent(request.content().trim());
        newsPost.setPublished(request.published());
    }

    private void apply(NewsPost newsPost, UpdateNewsRequest request) {
        newsPost.setTitle(request.title().trim());
        newsPost.setSlug(request.slug().trim());
        newsPost.setSummary(request.summary().trim());
        newsPost.setContent(request.content().trim());
        newsPost.setPublished(request.published());
    }

    private NewsPost getEntityById(Long id) {
        return newsPostRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Новость не найдена."));
    }

    private NewsDto toDto(NewsPost newsPost) {
        return new NewsDto(
                newsPost.getId(),
                newsPost.getTitle(),
                newsPost.getSlug(),
                newsPost.getSummary(),
                newsPost.getContent(),
                newsPost.isPublished(),
                newsPost.getCreatedAt(),
                newsPost.getUpdatedAt()
        );
    }
}
