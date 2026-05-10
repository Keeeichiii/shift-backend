package shift.shift_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ShiftBackendApplication {

	public static void main(String[] args) {
		String envPort = System.getenv("SERVER_PORT");
		if (envPort != null && !envPort.isBlank()) {
			System.err.println("[shift-backend] SERVER_PORT=" + envPort
					+ " задаётся в окружении и перекрывает server.port из application.properties.");
		}
		String sysPort = System.getProperty("server.port");
		if (sysPort != null && !sysPort.isBlank()) {
			System.err.println("[shift-backend] -Dserver.port=" + sysPort
					+ " задаётся в JVM и перекрывает server.port из application.properties.");
		}
		SpringApplication.run(ShiftBackendApplication.class, args);
	}

}
