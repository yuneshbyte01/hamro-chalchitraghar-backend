package com.chalchitraghar.mapper;

import org.springframework.stereotype.Component;

import com.chalchitraghar.dto.hall.HallRequest;
import com.chalchitraghar.dto.hall.HallResponse;
import com.chalchitraghar.model.Hall;

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

    /**
     * Converts a Hall entity to a HallResponse DTO.
     *
     * @param hall the entity to convert
     * @return the response DTO, or null if hall is null
     */
    public HallResponse toResponseDto(Hall hall) {
        if (hall == null) {
            return null;
        }

        HallResponse dto = new HallResponse();
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
