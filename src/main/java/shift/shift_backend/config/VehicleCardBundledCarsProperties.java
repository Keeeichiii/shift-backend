package shift.shift_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties(prefix = "app.vehicle-card-bundled-cars")
public class VehicleCardBundledCarsProperties {


	private String root = "";

	public String getRoot() {
		return root;
	}

	public void setRoot(String root) {
		this.root = root == null ? "" : root;
	}
}
