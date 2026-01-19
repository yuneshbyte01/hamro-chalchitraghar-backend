package com.chalchitraghar.service;

import org.springframework.stereotype.Service;

import com.chalchitraghar.dto.hall.HallRequest;
import com.chalchitraghar.dto.hall.HallResponse;
import com.chalchitraghar.exception.ResourceNotFoundException;
import com.chalchitraghar.mapper.HallMapper;
import com.chalchitraghar.model.Hall;
import com.chalchitraghar.model.enums.Status;
import com.chalchitraghar.repository.HallRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class HallService {

    private final HallRepository hallRepository;
    private final HallMapper hallMapper;

    public HallResponse addHall(HallRequest dto) {

        if (hallRepository.existsByName(dto.getName())) {
            throw new IllegalArgumentException("Hall with this name already exists");
        }

        Hall hall = hallMapper.toEntity(dto);
        Hall saved = hallRepository.save(hall);
        return hallMapper.toResponseDto(saved);
    }
    
    public HallResponse updateHall(Long id, HallRequest dto) {
        Hall hall = hallRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hall", id));
        hallMapper.updateEntityFromDto(hall, dto);
        Hall updated = hallRepository.save(hall);
        return hallMapper.toResponseDto(updated);
    }

    public void deleteHall(Long id) {
        Hall hall = hallRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hall", id));
        hall.setStatus(Status.INACTIVE);
        hallRepository.save(hall);
    }

    @Transactional(readOnly = true)
    public List<HallResponse> getAllHalls() {
        return hallRepository.findAll()
                .stream()
                .map(hallMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public HallResponse getHallById(Long id) {
        Hall hall = hallRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hall", id));
        return hallMapper.toResponseDto(hall);
    }

    @Transactional(readOnly = true)
    public List<HallResponse> getActiveHalls() {
        return hallRepository.findAllByStatus(Status.ACTIVE)
                .stream()
                .map(hallMapper::toResponseDto)
                .toList();
    }
}
