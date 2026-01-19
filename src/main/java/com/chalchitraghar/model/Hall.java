package com.chalchitraghar.model;

import com.chalchitraghar.model.enums.Status;

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

@Entity
@Table(name = "halls", indexes = {
    @jakarta.persistence.Index(name = "idx_hall_name", columnList = "name"),
    @jakarta.persistence.Index(name = "idx_hall_status", columnList = "status")
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Hall extends GenericEntity {

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
    @Enumerated(EnumType.STRING)
    @NotNull(message = "Status is required")
    private Status status;

    @PrePersist
    @Override
    protected void onCreate() {
        super.onCreate();
        this.status = Status.ACTIVE;
    }
}
