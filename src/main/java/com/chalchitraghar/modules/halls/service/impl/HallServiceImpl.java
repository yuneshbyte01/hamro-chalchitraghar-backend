package com.chalchitraghar.modules.halls.service.impl;

import com.chalchitraghar.modules.audit.enums.*;
import com.chalchitraghar.modules.audit.service.AuditBusinessPublisher;
import com.chalchitraghar.modules.halls.dto.request.HallRequest;
import com.chalchitraghar.modules.halls.dto.request.HallSearchCriteria;
import com.chalchitraghar.modules.halls.dto.response.AdminHallDetailResponse;
import com.chalchitraghar.modules.halls.dto.response.AdminHallSummaryResponse;
import com.chalchitraghar.modules.halls.dto.response.PublicHallDetailResponse;
import com.chalchitraghar.modules.halls.dto.response.PublicHallSummaryResponse;
import com.chalchitraghar.modules.halls.entity.Hall;
import com.chalchitraghar.modules.halls.enums.Status;
import com.chalchitraghar.modules.halls.mapper.HallMapper;
import com.chalchitraghar.modules.halls.repository.HallRepository;
import com.chalchitraghar.modules.halls.repository.SeatTemplateRepository;
import com.chalchitraghar.modules.halls.service.HallService;
import com.chalchitraghar.modules.halls.specification.HallSpecification;
import com.chalchitraghar.modules.shows.enums.ShowStatus;
import com.chalchitraghar.modules.shows.repository.ShowRepository;
import com.chalchitraghar.shared.exception.HallConflictException;
import com.chalchitraghar.shared.exception.ResourceNotFoundException;
import com.chalchitraghar.shared.response.PageResponse;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class HallServiceImpl implements HallService {

    private static final List<String> HALL_SORT_FIELDS =
            List.of("id", "name", "capacity", "layoutRef", "status", "createdAt", "updatedAt");

    private final HallRepository hallRepository;
    private final SeatTemplateRepository seatTemplateRepository;
    private final ShowRepository showRepository;
    private final HallMapper hallMapper;
    private final AuditBusinessPublisher audit;

    @Override
    public AdminHallDetailResponse addHall(HallRequest dto) {
        normalize(dto);
        ensureUniqueName(dto.getName(), null);
        Hall hall = hallMapper.toEntity(dto);
        Hall saved = hallRepository.save(hall);
        audit.catalog(
                AuditAction.HALL_CREATED,
                "HALL",
                saved.getId(),
                saved.getName(),
                null,
                hallSnapshot(saved),
                AuditSeverity.INFO);
        return hallMapper.toAdminDetail(saved);
    }

    @Override
    public AdminHallDetailResponse updateHall(Long id, HallRequest dto) {
        Hall hall =
                hallRepository
                        .findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Hall", id));
        normalize(dto);
        ensureUniqueName(dto.getName(), id);
        validateStatusTransition(hall.getStatus(), dto.getStatus());
        ensureLayoutDependenciesAllowUpdate(hall, dto);
        if (hall.getStatus() == Status.ACTIVE && dto.getStatus() == Status.INACTIVE) {
            ensureNoFutureActiveShows(hall.getId());
        }
        Map<String, ?> before = hallSnapshot(hall);
        hallMapper.updateEntityFromDto(hall, dto);
        Hall saved = hallRepository.save(hall);
        audit.catalog(
                AuditAction.HALL_UPDATED,
                "HALL",
                saved.getId(),
                saved.getName(),
                before,
                hallSnapshot(saved),
                AuditSeverity.INFO);
        return hallMapper.toAdminDetail(saved);
    }

    @Override
    public void deleteHall(Long id) {
        Hall hall =
                hallRepository
                        .findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Hall", id));
        if (hall.getStatus() == Status.ACTIVE) {
            ensureNoFutureActiveShows(hall.getId());
        }
        Map<String, ?> before = hallSnapshot(hall);
        hall.setStatus(Status.INACTIVE);
        hallRepository.save(hall);
        audit.catalog(
                AuditAction.HALL_UPDATED,
                "HALL",
                hall.getId(),
                hall.getName(),
                before,
                hallSnapshot(hall),
                AuditSeverity.WARNING);
    }

    private Map<String, ?> hallSnapshot(Hall hall) {
        return Map.of(
                "id",
                hall.getId(),
                "name",
                hall.getName(),
                "capacity",
                hall.getCapacity(),
                "status",
                hall.getStatus().name());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PublicHallSummaryResponse> getPublicHalls(
            HallSearchCriteria criteria, int page, int size, String sortBy, String sortDir) {
        Page<Hall> halls = searchPublicHalls(criteria, page, size, sortBy, sortDir);
        List<PublicHallSummaryResponse> content =
                halls.getContent().stream().map(hallMapper::toPublicSummary).toList();
        return PageResponse.from(halls, content);
    }

    @Override
    @Transactional(readOnly = true)
    public PublicHallDetailResponse getPublicHallById(Long id) {
        Hall hall =
                hallRepository
                        .findById(id)
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
    public PageResponse<AdminHallSummaryResponse> getAdminHalls(
            HallSearchCriteria criteria, int page, int size, String sortBy, String sortDir) {
        Page<Hall> halls = searchAdminHalls(criteria, page, size, sortBy, sortDir);
        List<AdminHallSummaryResponse> content =
                halls.getContent().stream().map(hallMapper::toAdminSummary).toList();
        return PageResponse.from(halls, content);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminHallDetailResponse getAdminHallById(Long id) {
        Hall hall =
                hallRepository
                        .findById(id)
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

    private Page<Hall> searchPublicHalls(
            HallSearchCriteria criteria, int page, int size, String sortBy, String sortDir) {
        validatePageRequest(page, size, sortBy);
        Sort.Direction direction = parseSortDirection(sortDir);
        var pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        return hallRepository.findAll(HallSpecification.publicSearch(criteria), pageable);
    }

    private Page<Hall> searchAdminHalls(
            HallSearchCriteria criteria, int page, int size, String sortBy, String sortDir) {
        validatePageRequest(page, size, sortBy);
        Sort.Direction direction = parseSortDirection(sortDir);
        Status status =
                parseEnum(Status.class, criteria == null ? null : criteria.status(), "status");
        var pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        return hallRepository.findAll(HallSpecification.adminSearch(criteria, status), pageable);
    }

    private void validatePageRequest(int page, int size, String sortBy) {
        if (page < 0) {
            throw new IllegalArgumentException("Page must be zero or greater");
        }
        if (size < 1) {
            throw new IllegalArgumentException("Size must be at least 1");
        }
        if (!HALL_SORT_FIELDS.contains(sortBy)) {
            throw new IllegalArgumentException(
                    "Invalid sortBy. Allowed values: " + String.join(", ", HALL_SORT_FIELDS));
        }
    }

    private Sort.Direction parseSortDirection(String sortDir) {
        if ("asc".equalsIgnoreCase(sortDir)) {
            return Sort.Direction.ASC;
        }
        if ("desc".equalsIgnoreCase(sortDir)) {
            return Sort.Direction.DESC;
        }
        throw new IllegalArgumentException("Invalid sortDir. Allowed values: asc, desc");
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumType, String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Arrays.stream(enumType.getEnumConstants())
                .filter(enumValue -> enumValue.name().equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "Invalid "
                                                + fieldName
                                                + ". Allowed values: "
                                                + allowedEnumValues(enumType)));
    }

    private <E extends Enum<E>> String allowedEnumValues(Class<E> enumType) {
        return String.join(
                ", ", Arrays.stream(enumType.getEnumConstants()).map(Enum::name).toList());
    }

    private void normalize(HallRequest dto) {
        dto.setName(trim(dto.getName()));
        dto.setLayoutRef(trim(dto.getLayoutRef()));
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private void ensureUniqueName(String name, Long currentHallId) {
        boolean exists =
                currentHallId == null
                        ? hallRepository.existsByNameIgnoreCase(name)
                        : hallRepository.existsByNameIgnoreCaseAndIdNot(name, currentHallId);
        if (exists) {
            throw new HallConflictException("Hall with this name already exists");
        }
    }

    private void validateStatusTransition(Status currentStatus, Status nextStatus) {
        if (currentStatus == nextStatus) {
            return;
        }
        boolean allowed =
                (currentStatus == Status.ACTIVE && nextStatus == Status.INACTIVE)
                        || (currentStatus == Status.INACTIVE && nextStatus == Status.ACTIVE);
        if (!allowed) {
            throw new HallConflictException(
                    "Invalid hall status transition from " + currentStatus + " to " + nextStatus);
        }
    }

    private void ensureLayoutDependenciesAllowUpdate(Hall hall, HallRequest dto) {
        if (!seatTemplateRepository.existsByHallId(hall.getId())) {
            return;
        }
        if (!hall.getCapacity().equals(dto.getCapacity())) {
            throw new HallConflictException(
                    "Cannot change hall capacity after seat layout has been generated");
        }
        if (!hall.getLayoutRef().equals(dto.getLayoutRef())) {
            throw new HallConflictException(
                    "Cannot change hall layout reference after seat layout has been generated");
        }
    }

    private void ensureNoFutureActiveShows(Long hallId) {
        boolean hasFutureActiveShows =
                showRepository.existsFutureActiveShowForHall(
                        hallId,
                        List.of(ShowStatus.SCHEDULED, ShowStatus.RUNNING),
                        LocalDate.now(),
                        LocalTime.now());
        if (hasFutureActiveShows) {
            throw new HallConflictException(
                    "Cannot inactivate hall while future active shows exist");
        }
    }
}
