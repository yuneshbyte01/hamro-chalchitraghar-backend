package com.chalchitraghar.service.show;

/**
 * Service for generating seats for shows based on hall seat templates.
 */
public interface SeatGenerationService {

    void generateSeatsForShow(Long showId);
}
