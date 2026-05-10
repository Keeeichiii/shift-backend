package shift.shift_backend.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import shift.shift_backend.domain.enums.DocumentStatus;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "region_id")
    private Integer regionId;

    @Column(nullable = false, unique = true, length = 45)
    private String username;

    @Column(name = "registration_date", nullable = false)
    private LocalDate registrationDate;

    @Column(name = "first_name", nullable = false, length = 45)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 45)
    private String lastName;

    @Column(name = "personal_id_number", unique = true, length = 14)
    private String personalIdNumber;

    @Column(name = "driver_license", unique = true, length = 50)
    private String driverLicense;

    @Column(name = "license_expires_at")
    private LocalDate licenseExpiresAt;

    @Column(name = "driving_ban_until")
    private LocalDate drivingBanUntil;

    @Column(name = "license_front_image", columnDefinition = "text")
    private String licenseFrontImage;

    @Column(name = "license_back_image", columnDefinition = "text")
    private String licenseBackImage;

    @Column(name = "passport_main_image", columnDefinition = "text")
    private String passportMainImage;

    @Column(name = "license_submitted_at")
    private OffsetDateTime licenseSubmittedAt;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "doc_status", nullable = false, columnDefinition = "document_status")
    private DocumentStatus docStatus = DocumentStatus.PENDING;

    @Column(name = "profile_name", length = 45)
    private String profileName;

    @Column(name = "avatar_url", length = 255)
    private String avatarUrl;

    @Column(length = 300)
    private String bio;

    @Column(name = "last_activity")
    private OffsetDateTime lastActivity;

    @Column(unique = true, length = 30)
    private String phone;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        if (registrationDate == null) {
            registrationDate = now.toLocalDate();
        }
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
