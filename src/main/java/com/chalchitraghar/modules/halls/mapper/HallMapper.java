package com.chalchitraghar.modules.halls.mapper;

import org.springframework.stereotype.Component;

import com.chalchitraghar.modules.halls.dto.request.HallRequest;
import com.chalchitraghar.modules.halls.dto.response.AdminHallDetailResponse;
import com.chalchitraghar.modules.halls.dto.response.AdminHallSummaryResponse;
import com.chalchitraghar.modules.halls.dto.response.PublicHallDetailResponse;
import com.chalchitraghar.modules.halls.dto.response.PublicHallSummaryResponse;
import com.chalchitraghar.modules.halls.entity.Hall;

/**
 * Mapper for converting between Hall entity and DTOs.
 */
@Component
public class HallMapper {

    /**
     * Converts a HallRequest DTO to a Hall entity.
     *
     * @param dto the request DTO to convert
     * @return the Hall entity, or null if dto is null
     */
    public Hall toEntity(HallRequest dto) {
        if (dto == null) {
            return null;
        }

        return Hall.builder()
                .name(dto.getName())
                .capacity(dto.getCapacity())
                .layoutRef(dto.getLayoutRef())
                .status(dto.getStatus())
                .build();
    }

    /**
     * Updates an existing Hall entity with values from a HallRequest DTO.
     *
     * @param hall the entity to update
     * @param dto the request DTO containing new values
     */
    public void updateEntityFromDto(Hall hall, HallRequest dto) {
        if (hall == null || dto == null) {
            return;
        }

        hall.setName(dto.getName());
        hall.setCapacity(dto.getCapacity());
        hall.setLayoutRef(dto.getLayoutRef());
        hall.setStatus(dto.getStatus());
    }

    public PublicHallSummaryResponse toPublicSummary(Hall hall) {
        if (hall == null) {
            return null;
        }

        PublicHallSummaryResponse dto = new PublicHallSummaryResponse();
        dto.setId(hall.getId());
        dto.setName(hall.getName());
        dto.setCapacity(hall.getCapacity());
        return dto;
    }

    public PublicHallDetailResponse toPublicDetail(Hall hall) {
        if (hall == null) {
            return null;
        }

        PublicHallDetailResponse dto = new PublicHallDetailResponse();
        dto.setId(hall.getId());
        dto.setName(hall.getName());
        dto.setCapacity(hall.getCapacity());
        dto.setStatus(hall.getStatus());
        return dto;
    }

    public AdminHallSummaryResponse toAdminSummary(Hall hall) {
        if (hall == null) {
            return null;
        }

        AdminHallSummaryResponse dto = new AdminHallSummaryResponse();
        dto.setId(hall.getId());
        dto.setName(hall.getName());
        dto.setCapacity(hall.getCapacity());
        dto.setLayoutRef(hall.getLayoutRef());
        dto.setStatus(hall.getStatus());
        return dto;
    }

    public AdminHallDetailResponse toAdminDetail(Hall hall) {
        if (hall == null) {
            return null;
        }

        AdminHallDetailResponse dto = new AdminHallDetailResponse();
        dto.setId(hall.getId());
        dto.setName(hall.getName());
        dto.setCapacity(hall.getCapacity());
        dto.setLayoutRef(hall.getLayoutRef());
        dto.setStatus(hall.getStatus());
        dto.setCreatedAt(hall.getCreatedAt());
        dto.setUpdatedAt(hall.getUpdatedAt());
        return dto;
    }
}
