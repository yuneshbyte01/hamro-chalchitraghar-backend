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

import com.chalchitraghar.dto.show.ShowRequestDto;
import com.chalchitraghar.dto.show.ShowResponseDto;
import com.chalchitraghar.service.ShowService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController("adminShowController")
@RequestMapping("/api/admin/shows")
@RequiredArgsConstructor
public class ShowController {

    private final ShowService showService;

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Working....");
    }

    @GetMapping
    public ResponseEntity<List<ShowResponseDto>> getAllShows() {
        List<ShowResponseDto> shows = showService.getAllShows();
        return ResponseEntity.ok(shows);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShowResponseDto> getShowById(@PathVariable Long id) {
        ShowResponseDto show = showService.getShowById(id);
        return ResponseEntity.ok(show);
    }

    @PostMapping
    public ResponseEntity<ShowResponseDto> createShow(@Valid @RequestBody ShowRequestDto dto) {
        ShowResponseDto createdShow = showService.addShow(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdShow);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ShowResponseDto> updateShow(@PathVariable Long id, @Valid @RequestBody ShowRequestDto dto) {
        ShowResponseDto updatedShow = showService.updateShow(id, dto);
        return ResponseEntity.ok(updatedShow);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteShow(@PathVariable Long id) {
        showService.deleteShow(id);
        return ResponseEntity.noContent().build();
    }
}
