package shift.shift_backend.service;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Подпапки в {@code static/images/cars/…} (строчные кириллические имена, как в репозитории).
 */
public final class VehicleCardCategoryUploadDirs {

	private static final Map<String, String> BY_CATEGORY = Map.ofEntries(
			Map.entry("standard", "стандарт"),
			Map.entry("crossover", "кроссовер"),
			Map.entry("premium", "премиум"),
			Map.entry("minivan", "минивэн 7 мест"),
			Map.entry("exclusive", "эксклюзив"),
			Map.entry("electric", "электро"),
			Map.entry("cabriolet", "кабриолет"),
			Map.entry("offroad", "внедорожник"),
			Map.entry("cargo", "грузовой"),
			Map.entry("long_booking", "долгое бронирование")
	);

	private VehicleCardCategoryUploadDirs() {
	}

	public static Optional<String> subdirForCategory(String categoryCode) {
		if (categoryCode == null || categoryCode.isBlank()) {
			return Optional.empty();
		}
		String key = categoryCode.trim().toLowerCase(Locale.ROOT);
		return Optional.ofNullable(BY_CATEGORY.get(key));
	}
}
