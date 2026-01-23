package com.chalchitraghar.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chalchitraghar.model.Booking;

/**
 * Repository interface for Booking entity persistence operations.
 */
public interface BookingRepository extends JpaRepository<Booking, Long> {
}