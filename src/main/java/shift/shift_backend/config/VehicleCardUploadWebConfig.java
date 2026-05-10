package shift.shift_backend.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties({VehicleCardUploadProperties.class, VehicleCardBundledCarsProperties.class})
public class VehicleCardUploadWebConfig implements WebMvcConfigurer {

	private final String uploadResourceLocation;

	public VehicleCardUploadWebConfig(VehicleCardUploadProperties properties) throws IOException {
		Path dir = Path.of(properties.getDir()).toAbsolutePath().normalize();
		Files.createDirectories(dir);
		String location = dir.toUri().toString();
		this.uploadResourceLocation = location.endsWith("/") ? location : location + "/";
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/uploads/vehicle-cards/**")
				.addResourceLocations(uploadResourceLocation);
	}
}
