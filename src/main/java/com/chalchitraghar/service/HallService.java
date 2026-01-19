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

/**
 * Service for hall management operations.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class HallService {

    private final HallRepository hallRepository;
    private final HallMapper hallMapper;

    /**
     * Creates a new hall.
     *
     * @param dto hall request containing hall details
     * @return created hall response
     * @throws IllegalArgumentException if a hall with the same name already exists
     */
    public HallResponse addHall(HallRequest dto) {

        if (hallRepository.existsByName(dto.getName())) {
            throw new IllegalArgumentException("Hall with this name already exists");
        }

        Hall hall = hallMapper.toEntity(dto);
        Hall saved = hallRepository.save(hall);
        return hallMapper.toResponseDto(saved);
    }
    
    /**
     * Updates an existing hall.
     *
     * @param id the hall ID
     * @param dto hall request containing updated details
     * @return updated hall response
     * @throws ResourceNotFoundException if hall is not found
     */
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

    /**
     * Retrieves all halls.
     *
     * @return list of all hall responses
     */
    @Transactional(readOnly = true)
    public List<HallResponse> getAllHalls() {
        return hallRepository.findAll()
                .stream()
                .map(hallMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a hall by ID.
     *
     * @param id the hall ID
     * @return hall response
     * @throws ResourceNotFoundException if hall is not found
     */
    @Transactional(readOnly = true)
    public HallResponse getHallById(Long id) {
        Hall hall = hallRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hall", id));
        return hallMapper.toResponseDto(hall);
    }

    /**
     * Retrieves all active halls.
     *
     * @return list of active hall responses
     */
    @Transactional(readOnly = true)
    public List<HallResponse> getActiveHalls() {
        return hallRepository.findAllByStatus(Status.ACTIVE)
                .stream()
                .map(hallMapper::toResponseDto)
                .toList();
    }
}
