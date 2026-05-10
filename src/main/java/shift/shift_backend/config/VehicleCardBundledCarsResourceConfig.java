package shift.shift_backend.config;

import java.nio.file.Path;
import java.util.Optional;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Раздаёт {@code /images/cars/**} с диска (том в Docker или {@code src/main/resources/…} локально),
 * чтобы пути из БД совпадали с файлами вне только встроенного в JAR classpath.
 */
@Configuration
public class VehicleCardBundledCarsResourceConfig implements WebMvcConfigurer {

	private final VehicleCardBundledCarsRootResolver bundledCarsRootResolver;

	public VehicleCardBundledCarsResourceConfig(VehicleCardBundledCarsRootResolver bundledCarsRootResolver) {
		this.bundledCarsRootResolver = bundledCarsRootResolver;
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		Optional<Path> root = bundledCarsRootResolver.resolveReadableBundledRoot();
		if (root.isEmpty()) {
			return;
		}
		String location = root.get().toUri().toString();
		if (!location.endsWith("/")) {
			location = location + "/";
		}
		registry.addResourceHandler("/images/cars/**")
				.addResourceLocations(location);
	}
}
