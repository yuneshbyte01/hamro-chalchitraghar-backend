package com.chalchitraghar.controller.admin;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chalchitraghar.dto.show.ShowRequest;
import com.chalchitraghar.dto.show.ShowResponse;
import com.chalchitraghar.service.ShowService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for admin show management endpoints.
 * Requires ADMIN role for all operations.
 */
@RestController("adminShowController")
@RequestMapping("/api/admin/shows")
@RequiredArgsConstructor
public class ShowController {

    private final ShowService showService;

    /**
     * Retrieves all shows.
     *
     * @return list of all show responses
     */
    @GetMapping
    public ResponseEntity<List<ShowResponse>> getAllShows() {
        List<ShowResponse> shows = showService.getAllShows();
        return ResponseEntity.ok(shows);
    }

    /**
     * Retrieves a show by ID.
     *
     * @param id the show ID
     * @return show response
     */
    @GetMapping("/{id}")
    public ResponseEntity<ShowResponse> getShowById(@PathVariable Long id) {
        ShowResponse show = showService.getShowById(id);
        return ResponseEntity.ok(show);
    }

    @PostMapping
    public ResponseEntity<ShowResponse> createShow(@Valid @RequestBody ShowRequest dto) {
        ShowResponse createdShow = showService.addShow(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdShow);
    }

    /**
     * Updates an existing show.
     *
     * @param id the show ID
     * @param dto show request containing updated details
     * @return updated show response
     */
    @PutMapping("/{id}")
    public ResponseEntity<ShowResponse> updateShow(@PathVariable Long id, @Valid @RequestBody ShowRequest dto) {
        ShowResponse updatedShow = showService.updateShow(id, dto);
        return ResponseEntity.ok(updatedShow);
    }

    /**
     * Soft deletes a show by setting its status to CANCELLED.
     *
     * @param id the show ID
     * @return no content response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteShow(@PathVariable Long id) {
        showService.deleteShow(id);
        return ResponseEntity.noContent().build();
    }
}
