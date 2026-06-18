package shift.shift_backend.config;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;


public final class DatabaseUrlBootstrap {

	private DatabaseUrlBootstrap() {
	}

	public static void applyFromEnvironment() {
		if (System.getenv("SPRING_DATASOURCE_URL") != null && !System.getenv("SPRING_DATASOURCE_URL").isBlank()) {
			return;
		}
		if (System.getProperty("spring.datasource.url") != null && !System.getProperty("spring.datasource.url").isBlank()) {
			return;
		}
		String databaseUrl = System.getenv("DATABASE_URL");
		if (databaseUrl == null || databaseUrl.isBlank() || !databaseUrl.startsWith("postgres")) {
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
			System.setProperty("spring.datasource.url", jdbc.toString());
			System.setProperty("spring.datasource.username", user);
			System.setProperty("spring.datasource.password", password);
		} catch (IllegalArgumentException ignored) {
		}
	}
}
