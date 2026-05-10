package shift.shift_backend.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import shift.shift_backend.config.VehicleCardBundledCarsRootResolver;
import shift.shift_backend.config.VehicleCardUploadProperties;

@Service
public class VehicleCardImageUploadService {

	private static final Set<String> ALLOWED_TYPES = Set.of(
			"image/jpeg",
			"image/png",
			"image/webp",
			"image/gif"
	);

	private final Path uploadDir;
	private final VehicleCardBundledCarsRootResolver bundledCarsRootResolver;

	public VehicleCardImageUploadService(
			VehicleCardUploadProperties properties,
			VehicleCardBundledCarsRootResolver bundledCarsRootResolver
	) throws IOException {
		this.uploadDir = Path.of(properties.getDir()).toAbsolutePath().normalize();
		Files.createDirectories(uploadDir);
		this.bundledCarsRootResolver = bundledCarsRootResolver;
	}

	public String store(MultipartFile file) {
		return store(file, null);
	}

	/**
	 * Сохраняет в {@code src/main/resources/static/images/cars/<категория>/}, если корень доступен
	 * (локальная разработка); иначе в {@code app.vehicle-card-upload.dir} (Docker / JAR).
	 */
	public String store(MultipartFile file, String categoryCode) {
		if (file == null || file.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Файл не передан.");
		}
		String contentType = file.getContentType();
		if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
			throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
					"Допустимы только изображения JPEG, PNG, WebP или GIF.");
		}
		String extension = extensionForContentType(contentType.toLowerCase(Locale.ROOT));
		String storedName = UUID.randomUUID() + extension;
		String subdir = VehicleCardCategoryUploadDirs.subdirForCategory(categoryCode).orElse(null);

		Optional<Path> bundledRoot = bundledCarsRootResolver.resolveWritableRoot();
		if (bundledRoot.isPresent()) {
			return storeUnderBundledRoot(bundledRoot.get(), subdir, storedName, file);
		}
		return storeUnderUploadDir(subdir, storedName, file);
	}

	private String storeUnderBundledRoot(Path carsRoot, String subdir, String storedName, MultipartFile file) {
		Path destDir = subdir != null ? carsRoot.resolve(subdir).normalize() : carsRoot;
		if (!destDir.startsWith(carsRoot)) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Некорректный путь сохранения.");
		}
		try {
			Files.createDirectories(destDir);
		} catch (IOException e) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Не удалось создать каталог изображений.");
		}
		Path target = destDir.resolve(storedName).normalize();
		if (!target.startsWith(carsRoot)) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Некорректный путь сохранения.");
		}
		copyStreamToFile(file, target);
		mirrorToTargetClasses(carsRoot, target);
		if (subdir == null) {
			return "/images/cars/" + storedName;
		}
		return "/images/cars/" + subdir + "/" + storedName;
	}

	private void mirrorToTargetClasses(Path carsRoot, Path writtenFile) {
		Optional<Path> mirrorRoot = bundledCarsRootResolver.targetClassesMirrorRoot();
		if (mirrorRoot.isEmpty() || !writtenFile.startsWith(carsRoot)) {
			return;
		}
		try {
			Path relative = carsRoot.relativize(writtenFile).normalize();
			if (relative.isAbsolute() || relative.startsWith("..")) {
				return;
			}
			Path mirrorFile = mirrorRoot.get().resolve(relative).normalize();
			if (!mirrorFile.startsWith(mirrorRoot.get())) {
				return;
			}
			Files.createDirectories(mirrorFile.getParent());
			Files.copy(writtenFile, mirrorFile, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException ignored) {
			// зеркало не обязательно для работы; основной файл уже в src/main/resources
		}
	}

	private String storeUnderUploadDir(String subdir, String storedName, MultipartFile file) {
		Path destDir = uploadDir;
		if (subdir != null) {
			destDir = uploadDir.resolve(subdir).normalize();
			if (!destDir.startsWith(uploadDir)) {
				throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Некорректный путь сохранения.");
			}
			try {
				Files.createDirectories(destDir);
			} catch (IOException e) {
				throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Не удалось создать каталог загрузки.");
			}
		}
		Path target = destDir.resolve(storedName).normalize();
		if (!target.startsWith(uploadDir)) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Некорректный путь сохранения.");
		}
		copyStreamToFile(file, target);
		if (subdir == null) {
			return "/uploads/vehicle-cards/" + storedName;
		}
		return "/uploads/vehicle-cards/" + subdir + "/" + storedName;
	}

	private static void copyStreamToFile(MultipartFile file, Path target) {
		try (InputStream in = file.getInputStream()) {
			Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Не удалось сохранить файл.");
		}
	}

	private static String extensionForContentType(String contentType) {
		return switch (contentType) {
			case "image/jpeg" -> ".jpg";
			case "image/png" -> ".png";
			case "image/webp" -> ".webp";
			case "image/gif" -> ".gif";
			default -> ".bin";
		};
	}
}
