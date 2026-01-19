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

@RestController("adminShowController")
@RequestMapping("/api/admin/shows")
@RequiredArgsConstructor
public class ShowController {

    private final ShowService showService;

    @GetMapping
    public ResponseEntity<List<ShowResponse>> getAllShows() {
        List<ShowResponse> shows = showService.getAllShows();
        return ResponseEntity.ok(shows);
    }

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

    @PutMapping("/{id}")
    public ResponseEntity<ShowResponse> updateShow(@PathVariable Long id, @Valid @RequestBody ShowRequest dto) {
        ShowResponse updatedShow = showService.updateShow(id, dto);
        return ResponseEntity.ok(updatedShow);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteShow(@PathVariable Long id) {
        showService.deleteShow(id);
        return ResponseEntity.noContent().build();
    }
}
