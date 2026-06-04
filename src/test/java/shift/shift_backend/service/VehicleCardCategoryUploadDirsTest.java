package shift.shift_backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class VehicleCardCategoryUploadDirsTest {

    @Test
    void resolvesKnownCategoryIgnoringCaseAndSpaces() {
        assertThat(VehicleCardCategoryUploadDirs.subdirForCategory("  LONG_BOOKING  "))
                .contains("долгое бронирование");
    }

    @Test
    void returnsEmptyForUnknownOrBlankCategory() {
        assertThat(VehicleCardCategoryUploadDirs.subdirForCategory("unknown")).isEmpty();
        assertThat(VehicleCardCategoryUploadDirs.subdirForCategory(" ")).isEmpty();
        assertThat(VehicleCardCategoryUploadDirs.subdirForCategory(null)).isEmpty();
    }
}
