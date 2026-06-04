package shift.shift_backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import shift.shift_backend.config.VehicleCardBundledCarsRootResolver;
import shift.shift_backend.config.VehicleCardUploadProperties;

class VehicleCardImageUploadServiceTest {

    @TempDir
    private Path tempDir;

    @Test
    void storesAllowedImageUnderCategoryUploadDirectory() throws Exception {
        VehicleCardUploadProperties properties = new VehicleCardUploadProperties();
        properties.setDir(tempDir.resolve("uploads").toString());
        VehicleCardBundledCarsRootResolver resolver = org.mockito.Mockito.mock(VehicleCardBundledCarsRootResolver.class);
        when(resolver.resolveWritableRoot()).thenReturn(Optional.empty());

        VehicleCardImageUploadService service = new VehicleCardImageUploadService(properties, resolver);
        MockMultipartFile file = new MockMultipartFile("file", "car.png", "image/png", new byte[]{1, 2, 3});

        String path = service.store(file, "long_booking");

        assertThat(path).startsWith("/uploads/vehicle-cards/долгое бронирование/");
        assertThat(path).endsWith(".png");
        Path stored = tempDir.resolve(path.replace("/uploads/vehicle-cards/", "uploads/")).normalize();
        assertThat(Files.exists(stored)).isTrue();
        assertThat(Files.readAllBytes(stored)).containsExactly(1, 2, 3);
    }

    @Test
    void rejectsUnsupportedContentType() throws Exception {
        VehicleCardUploadProperties properties = new VehicleCardUploadProperties();
        properties.setDir(tempDir.resolve("uploads").toString());
        VehicleCardBundledCarsRootResolver resolver = org.mockito.Mockito.mock(VehicleCardBundledCarsRootResolver.class);
        when(resolver.resolveWritableRoot()).thenReturn(Optional.empty());
        VehicleCardImageUploadService service = new VehicleCardImageUploadService(properties, resolver);
        MockMultipartFile file = new MockMultipartFile("file", "note.txt", "text/plain", "x".getBytes());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.store(file));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }
}
