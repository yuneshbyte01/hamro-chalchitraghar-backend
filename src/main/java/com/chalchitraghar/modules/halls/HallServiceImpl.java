package com.chalchitraghar.modules.halls;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chalchitraghar.modules.halls.HallRequest;
import com.chalchitraghar.modules.halls.HallResponse;
import com.chalchitraghar.shared.exception.ResourceNotFoundException;
import com.chalchitraghar.modules.halls.HallMapper;
import com.chalchitraghar.modules.halls.Hall;
import com.chalchitraghar.modules.halls.Status;
import com.chalchitraghar.modules.halls.HallRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class HallServiceImpl implements HallService {

    private final HallRepository hallRepository;
    private final HallMapper hallMapper;

    @Override
    public HallResponse addHall(HallRequest dto) {
        if (hallRepository.existsByName(dto.getName())) {
            throw new IllegalArgumentException("Hall with this name already exists");
        }
        Hall hall = hallMapper.toEntity(dto);
        return hallMapper.toResponseDto(hallRepository.save(hall));
    }

    @Override
    public HallResponse updateHall(Long id, HallRequest dto) {
        Hall hall = hallRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hall", id));
        hallMapper.updateEntityFromDto(hall, dto);
        return hallMapper.toResponseDto(hallRepository.save(hall));
    }

    @Override
    public void deleteHall(Long id) {
        Hall hall = hallRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hall", id));
        hall.setStatus(Status.INACTIVE);
        hallRepository.save(hall);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HallResponse> getAllHalls() {
        return hallRepository.findAll().stream().map(hallMapper::toResponseDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public HallResponse getHallById(Long id) {
        Hall hall = hallRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hall", id));
        return hallMapper.toResponseDto(hall);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HallResponse> getActiveHalls() {
        return hallRepository.findAllByStatus(Status.ACTIVE).stream().map(hallMapper::toResponseDto).toList();
    }
}
