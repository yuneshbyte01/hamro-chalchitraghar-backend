package com.chalchitraghar.modules.halls;

import com.chalchitraghar.shared.GenericEntity;

import com.chalchitraghar.modules.halls.Status;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Represents a cinema hall with seating capacity and layout configuration.
 */
@Entity
@Table(name = "halls", indexes = {
    @jakarta.persistence.Index(name = "idx_hall_name", columnList = "name"), // Index for name lookup
    @jakarta.persistence.Index(name = "idx_hall_status", columnList = "status") // Index for status lookup
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Hall extends GenericEntity {

    /**
     * Unique name identifier for the hall.
     */
    @Column(nullable = false, unique = true)
    @NotBlank(message = "Name is required")
    private String name;

    /**
     * Maximum seating capacity of the hall.
     */
    @Column(nullable = false)
    @PositiveOrZero(message = "Capacity must be at least 1")
    private Integer capacity;

    /**
     * Reference to the layout configuration for the hall.
     */
    @Column(nullable = false)
    @NotBlank(message = "Layout reference is required")
    private String layoutRef;

    /**
     * Operational status of the hall. Automatically set to ACTIVE on creation.
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @NotNull(message = "Status is required")
    private Status status;

    /**
     * Lifecycle callback invoked before entity persistence.
     * Sets default status to ACTIVE.
     */
    @PrePersist
    @Override
    protected void onCreate() {
        super.onCreate();
        this.status = Status.ACTIVE;
    }
}
