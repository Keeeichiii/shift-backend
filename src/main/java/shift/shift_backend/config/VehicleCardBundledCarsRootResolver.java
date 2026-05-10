package shift.shift_backend.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class VehicleCardBundledCarsRootResolver {

	private final VehicleCardBundledCarsProperties properties;

	public VehicleCardBundledCarsRootResolver(VehicleCardBundledCarsProperties properties) {
		this.properties = properties;
	}

	public Optional<Path> resolveReadableBundledRoot() {
		String configured = properties.getRoot();
		if (configured != null && !configured.isBlank()) {
			Path path = Path.of(configured.trim()).toAbsolutePath().normalize();
			if (Files.isDirectory(path)) {
				return Optional.of(path);
			}
			return Optional.empty();
		}
		Path def = Path.of(System.getProperty("user.dir", "."))
				.resolve("src/main/resources/static/images/cars")
				.toAbsolutePath()
				.normalize();
		if (Files.isDirectory(def)) {
			return Optional.of(def);
		}
		return Optional.empty();
	}

	/**
	 * Каталог {@code static/images/cars}, куда можно писать файлы карточек (локальный проект).
	 */
	public Optional<Path> resolveWritableRoot() {
		return resolveReadableBundledRoot()
				.filter(path -> Files.isWritable(path));
	}

	/**
	 * Для мгновенной отдачи из classpath при {@code spring-boot:run} без полной пересборки.
	 */
	public Optional<Path> targetClassesMirrorRoot() {
		Path classes = Path.of(System.getProperty("user.dir", "."))
				.resolve("target/classes")
				.toAbsolutePath()
				.normalize();
		if (!Files.isDirectory(classes)) {
			return Optional.empty();
		}
		Path root = classes.resolve("static/images/cars").normalize();
		return Optional.of(root);
	}
}
