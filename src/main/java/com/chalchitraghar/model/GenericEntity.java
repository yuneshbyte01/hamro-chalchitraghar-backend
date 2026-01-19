package com.chalchitraghar.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Base entity class containing common fields and lifecycle methods.
 * All entity classes should extend this class to inherit:
 * - id (Primary key)
 * - createdAt (Timestamp of creation)
 * - updatedAt (Timestamp of last update)
 * - Automatic timestamp management via @PrePersist and @PreUpdate
 */
@MappedSuperclass
@Getter
@Setter
public abstract class GenericEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    @NotNull(message = "Created at is required")
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @NotNull(message = "Updated at is required")
    private LocalDateTime updatedAt;

    /**
     * Called before entity is persisted (inserted).
     * Sets createdAt and updatedAt timestamps.
     * Subclasses can override this method to add additional initialization logic.
     */
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * Called before entity is updated.
     * Updates the updatedAt timestamp.
     * Subclasses can override this method to add additional update logic.
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
