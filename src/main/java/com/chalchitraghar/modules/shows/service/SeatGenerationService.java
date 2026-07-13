package com.chalchitraghar.modules.shows.service;

/** Service for generating seats for shows based on hall seat templates. */
public interface SeatGenerationService {

    void generateSeatsForShow(Long showId);
}
