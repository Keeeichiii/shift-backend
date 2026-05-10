package shift.shift_backend.config;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Railway / Heroku передают {@code DATABASE_URL} как {@code postgresql://user:pass@host:port/db}.
 * Spring DataSource ожидает JDBC URL и отдельные учётные данные — преобразуем, если задан только
 * {@code DATABASE_URL} и нет {@code SPRING_DATASOURCE_URL}.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

	private static final String PROP_SPRING_DATASOURCE_URL = "SPRING_DATASOURCE_URL";
	private static final String PROP_DATABASE_URL = "DATABASE_URL";

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		String springDsUrl = environment.getProperty(PROP_SPRING_DATASOURCE_URL);
		if (springDsUrl != null && !springDsUrl.isBlank()) {
			return;
		}
		String databaseUrl = environment.getProperty(PROP_DATABASE_URL);
		if (databaseUrl == null || databaseUrl.isBlank()) {
			return;
		}
		if (!databaseUrl.startsWith("postgres")) {
			return;
		}
		try {
			String forUri = databaseUrl.replaceFirst("^postgres(ql)?://", "http://");
			URI uri = URI.create(forUri);
			String userInfo = uri.getUserInfo();
			if (userInfo == null || userInfo.isBlank()) {
				return;
			}
			int colon = userInfo.indexOf(':');
			String user = URLDecoder.decode(
					colon > 0 ? userInfo.substring(0, colon) : userInfo,
					StandardCharsets.UTF_8);
			String password = "";
			if (colon >= 0 && colon < userInfo.length() - 1) {
				password = URLDecoder.decode(userInfo.substring(colon + 1), StandardCharsets.UTF_8);
			}
			String host = uri.getHost();
			if (host == null || host.isBlank()) {
				return;
			}
			int port = uri.getPort() > 0 ? uri.getPort() : 5432;
			String path = uri.getPath();
			if (path != null && path.startsWith("/")) {
				path = path.substring(1);
			}
			if (path == null || path.isBlank()) {
				return;
			}
			StringBuilder jdbc = new StringBuilder();
			jdbc.append("jdbc:postgresql://").append(host).append(':').append(port).append('/').append(path);
			String query = uri.getQuery();
			if (query != null && !query.isBlank()) {
				jdbc.append('?').append(query);
			}
			Map<String, Object> map = new HashMap<>();
			map.put("spring.datasource.url", jdbc.toString());
			map.put("spring.datasource.username", user);
			map.put("spring.datasource.password", password);
			environment.getPropertySources().addFirst(new MapPropertySource("databaseUrlDerived", map));
		} catch (IllegalArgumentException ignored) {
			// оставляем дефолты из application.properties
		}
	}
}
