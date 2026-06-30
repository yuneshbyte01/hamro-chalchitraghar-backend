package com.chalchitraghar.shared;

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
 * Base entity class providing common fields and automatic timestamp management.
 * Entities extending this class inherit primary key and audit timestamp fields
 * with automatic lifecycle management.
 */
@MappedSuperclass
@Getter
@Setter
public abstract class GenericEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Timestamp when the entity was created. Set automatically on first persist
     * and cannot be modified thereafter.
     */
    @Column(nullable = false, updatable = false)
    @NotNull(message = "Created at is required")
    private LocalDateTime createdAt;

    /**
     * Timestamp of the last update. Automatically maintained by lifecycle callbacks.
     */
    @Column(nullable = false)
    @NotNull(message = "Updated at is required")
    private LocalDateTime updatedAt;

    /**
     * Lifecycle callback invoked before entity persistence.
     * Initializes both createdAt and updatedAt timestamps.
     */
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * Lifecycle callback invoked before entity update.
     * Refreshes the updatedAt timestamp.
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
