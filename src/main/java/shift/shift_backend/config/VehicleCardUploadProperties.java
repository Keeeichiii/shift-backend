package shift.shift_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.vehicle-card-upload")
public class VehicleCardUploadProperties {


	private String dir = System.getProperty("user.home") + "/.shift-backend/vehicle-card-uploads";

	public String getDir() {
		return dir;
	}

	public void setDir(String dir) {
		this.dir = dir;
	}
}
