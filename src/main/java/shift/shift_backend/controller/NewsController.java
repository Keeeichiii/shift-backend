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
import shift.shift_backend.dto.news.CreateNewsRequest;
import shift.shift_backend.dto.news.NewsDto;
import shift.shift_backend.dto.news.UpdateNewsRequest;
import shift.shift_backend.service.NewsService;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsService newsService;

    @GetMapping("/public")
    public List<NewsDto> getPublishedNews() {
        return newsService.getPublishedNews();
    }

    @GetMapping
    public List<NewsDto> getAllNews() {
        return newsService.getAllNews();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NewsDto create(@Valid @RequestBody CreateNewsRequest request) {
        return newsService.create(request);
    }

    @PutMapping("/{id}")
    public NewsDto update(@PathVariable Long id, @Valid @RequestBody UpdateNewsRequest request) {
        return newsService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        newsService.delete(id);
    }
}
