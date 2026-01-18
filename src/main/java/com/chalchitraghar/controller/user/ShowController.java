package com.chalchitraghar.controller.user;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.chalchitraghar.dto.show.ShowResponseDto;
import com.chalchitraghar.service.ShowService;

import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

@RestController("userShowController")
@RequestMapping("/api/shows")
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

    @GetMapping("/movie/{movieId}")
    public ResponseEntity<List<ShowResponseDto>> getShowsByMovie(@PathVariable Long movieId) {
        List<ShowResponseDto> shows = showService.getShowsByMovie(movieId);
        return ResponseEntity.ok(shows);
    }

    @GetMapping(params = {"movieId", "date"})
    public ResponseEntity<List<ShowResponseDto>> getShowsByMovieAndDate(
            @RequestParam Long movieId, 
            @RequestParam LocalDate date) {
        List<ShowResponseDto> shows = showService.getShowsByMovieAndShowDate(movieId, date);
        return ResponseEntity.ok(shows);
    }
}
