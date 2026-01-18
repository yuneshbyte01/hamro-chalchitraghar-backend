package com.chalchitraghar.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

import com.chalchitraghar.dto.hall.HallResponseDto;
import com.chalchitraghar.service.HallService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/halls")
@RequiredArgsConstructor
public class HallController {

    private final HallService hallService;

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Working....");
    }

    @GetMapping
    public ResponseEntity<List<HallResponseDto>> getAllHalls() {
        List<HallResponseDto> halls = hallService.getAllHalls();
        return ResponseEntity.ok(halls);
    }

    @GetMapping("/{id}")
    public ResponseEntity<HallResponseDto> getHallById(@PathVariable Long id) {
        HallResponseDto hall = hallService.getHallById(id);
        return ResponseEntity.ok(hall);
    }

    @GetMapping("/active")
    public ResponseEntity<List<HallResponseDto>> getActiveHalls() {
        List<HallResponseDto> halls = hallService.getActiveHalls();
        return ResponseEntity.ok(halls);
    }

}
