package shift.shift_backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import shift.shift_backend.domain.entity.NewsPost;
import shift.shift_backend.dto.news.CreateNewsRequest;
import shift.shift_backend.dto.news.UpdateNewsRequest;
import shift.shift_backend.repository.NewsPostRepository;

@ExtendWith(MockitoExtension.class)
class NewsServiceTest {

	@Mock
	private NewsPostRepository newsPostRepository;

	@InjectMocks
	private NewsService newsService;

	@Test
	void createThrowsWhenSlugAlreadyExists() {
		when(newsPostRepository.findBySlug("my-slug")).thenReturn(Optional.of(new NewsPost()));

		CreateNewsRequest req = new CreateNewsRequest("T", "my-slug", "summary", "body", true);
		ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> newsService.create(req));
		assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

		verify(newsPostRepository, never()).save(any());
	}

	@Test
	void updateThrowsWhenSlugBelongsToAnotherPost() {
		NewsPost self = new NewsPost();
		self.setId(1L);
		NewsPost other = new NewsPost();
		other.setId(2L);

		when(newsPostRepository.findById(1L)).thenReturn(Optional.of(self));
		when(newsPostRepository.findBySlug("taken")).thenReturn(Optional.of(other));

		UpdateNewsRequest req = new UpdateNewsRequest("T", "taken", "s", "c", true);
		ResponseStatusException ex2 = assertThrows(ResponseStatusException.class, () -> newsService.update(1L, req));
		assertThat(ex2.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
	}
}
