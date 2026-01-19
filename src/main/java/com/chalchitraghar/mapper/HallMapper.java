package com.chalchitraghar.mapper;

import org.springframework.stereotype.Component;

import com.chalchitraghar.dto.hall.HallRequest;
import com.chalchitraghar.dto.hall.HallResponse;
import com.chalchitraghar.model.Hall;

@Component
public class HallMapper {

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

    public void updateEntityFromDto(Hall hall, HallRequest dto) {
        if (hall == null || dto == null) {
            return;
        }

        hall.setName(dto.getName());
        hall.setCapacity(dto.getCapacity());
        hall.setLayoutRef(dto.getLayoutRef());
        hall.setStatus(dto.getStatus());
    }

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
