package shift.shift_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import shift.shift_backend.config.DatabaseUrlBootstrap;

@SpringBootApplication
public class ShiftBackendApplication {

	public static void main(String[] args) {
		DatabaseUrlBootstrap.applyFromEnvironment();

		SpringApplication.run(ShiftBackendApplication.class, args);
	}

}