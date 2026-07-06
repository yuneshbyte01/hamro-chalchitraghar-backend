package com.chalchitraghar.modules.halls.service.impl;

import com.chalchitraghar.modules.halls.service.HallService;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chalchitraghar.modules.halls.dto.request.HallRequest;
import com.chalchitraghar.modules.halls.dto.response.AdminHallDetailResponse;
import com.chalchitraghar.modules.halls.dto.response.AdminHallSummaryResponse;
import com.chalchitraghar.modules.halls.dto.response.PublicHallDetailResponse;
import com.chalchitraghar.modules.halls.dto.response.PublicHallSummaryResponse;
import com.chalchitraghar.shared.exception.ResourceNotFoundException;
import com.chalchitraghar.modules.halls.mapper.HallMapper;
import com.chalchitraghar.modules.halls.entity.Hall;
import com.chalchitraghar.modules.halls.enums.Status;
import com.chalchitraghar.modules.halls.repository.HallRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class HallServiceImpl implements HallService {

    private final HallRepository hallRepository;
    private final HallMapper hallMapper;

    @Override
    public AdminHallDetailResponse addHall(HallRequest dto) {
        if (hallRepository.existsByName(dto.getName())) {
            throw new IllegalArgumentException("Hall with this name already exists");
        }
        Hall hall = hallMapper.toEntity(dto);
        return hallMapper.toAdminDetail(hallRepository.save(hall));
    }

    @Override
    public AdminHallDetailResponse updateHall(Long id, HallRequest dto) {
        Hall hall = hallRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hall", id));
        hallMapper.updateEntityFromDto(hall, dto);
        return hallMapper.toAdminDetail(hallRepository.save(hall));
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
    public List<PublicHallSummaryResponse> getPublicHalls() {
        return hallRepository.findAllByStatus(Status.ACTIVE).stream()
                .map(hallMapper::toPublicSummary)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PublicHallDetailResponse getPublicHallById(Long id) {
        Hall hall = hallRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hall", id));
        if (hall.getStatus() != Status.ACTIVE) {
            throw new ResourceNotFoundException("Hall", id);
        }
        return hallMapper.toPublicDetail(hall);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PublicHallSummaryResponse> getPublicActiveHalls() {
        return hallRepository.findAllByStatus(Status.ACTIVE).stream()
                .map(hallMapper::toPublicSummary)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminHallSummaryResponse> getAdminHalls() {
        return hallRepository.findAll().stream()
                .map(hallMapper::toAdminSummary)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AdminHallDetailResponse getAdminHallById(Long id) {
        Hall hall = hallRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hall", id));
        return hallMapper.toAdminDetail(hall);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminHallSummaryResponse> getAdminActiveHalls() {
        return hallRepository.findAllByStatus(Status.ACTIVE).stream()
                .map(hallMapper::toAdminSummary)
                .toList();
    }
}
