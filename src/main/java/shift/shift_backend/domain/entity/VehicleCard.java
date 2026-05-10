package shift.shift_backend.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "vehicle_cards")
public class VehicleCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, unique = true, length = 120)
    private String slug;

    @Column(nullable = false, length = 80)
    private String category;

    @Column(nullable = false)
    private boolean wrapped;

    @Column(name = "image_path", nullable = false, length = 300)
    private String imagePath;

    @Column(name = "price_per_minute", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerMinute;

    @Column(length = 30)
    private String badge;

    @Column(name = "short_description", length = 500)
    private String shortDescription;

    @Column(name = "detail_description", length = 1200)
    private String detailDescription;

    @Column(length = 80)
    private String transmission;

    @Column(name = "fuel_type", length = 80)
    private String fuelType;

    @Column(length = 120)
    private String engine;

    @Column(name = "conditions_text", columnDefinition = "text")
    private String conditionsText;

    @Column(name = "features_text", columnDefinition = "text")
    private String featuresText;

    @Column(name = "minute_packages_text", columnDefinition = "text")
    private String minutePackagesText;

    @Column(name = "hour_packages_text", columnDefinition = "text")
    private String hourPackagesText;

    @Column(name = "day_packages_text", columnDefinition = "text")
    private String dayPackagesText;

    @Column(nullable = false)
    private boolean published = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
