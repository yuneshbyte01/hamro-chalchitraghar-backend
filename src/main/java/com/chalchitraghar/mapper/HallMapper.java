package com.chalchitraghar.mapper;

import org.springframework.stereotype.Component;

import com.chalchitraghar.dto.hall.HallRequestDto;
import com.chalchitraghar.dto.hall.HallResponseDto;
import com.chalchitraghar.model.Hall;

@Component
public class HallMapper {

    public Hall toEntity(HallRequestDto dto) {
        if (dto == null) {
            return null;
        }

        return Hall.builder()
                .name(dto.getName())
                .capacity(dto.getCapacity())
                .layoutRef(dto.getLayoutRef())
                .isActive(dto.isActive())
                .build();
    }

    public void updateEntityFromDto(Hall hall, HallRequestDto dto) {
        if (hall == null || dto == null) {
            return;
        }

        hall.setName(dto.getName());
        hall.setCapacity(dto.getCapacity());
        hall.setLayoutRef(dto.getLayoutRef());
        hall.setActive(dto.isActive());
    }

    public HallResponseDto toResponseDto(Hall hall) {
        if (hall == null) {
            return null;
        }

        HallResponseDto dto = new HallResponseDto();
        dto.setId(hall.getId());
        dto.setName(hall.getName());
        dto.setCapacity(hall.getCapacity());
        dto.setLayoutRef(hall.getLayoutRef());
        dto.setActive(hall.isActive());
        dto.setCreatedAt(hall.getCreatedAt());
        dto.setUpdatedAt(hall.getUpdatedAt());
        return dto;
    }
}
