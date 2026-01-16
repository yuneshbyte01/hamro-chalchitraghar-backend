package com.chalchitraghar.model;

import java.time.LocalDateTime;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

@Entity
@Table(name = "halls")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Hall {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    @NotBlank(message = "Name is required")
    private String name;

    @Column(nullable = false)
    @PositiveOrZero(message = "Capacity must be at least 1")
    private Integer capacity;

    @Column(nullable = false)
    @NotBlank(message = "Layout reference is required")
    private String layoutRef;

    @Column(nullable = false)
    @NotNull(message = "Active status is required")
    private boolean isActive;

    @Column(nullable = false)
    @NotNull(message = "Created at is required")
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @NotNull(message = "Updated at is required")
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.isActive = true;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
