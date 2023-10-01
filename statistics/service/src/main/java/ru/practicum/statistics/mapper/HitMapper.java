package ru.practicum.statistics.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.statistics.dto.RequestHitDto;
import ru.practicum.statistics.entity.Hit;

@UtilityClass
public class HitMapper {
    public Hit toHitEntityFromRequestHitDto(RequestHitDto hitDto) {
        return Hit.builder()
                .app(hitDto.getApp())
                .uri(hitDto.getUri())
                .ip(hitDto.getIp())
                .timestamp(hitDto.getTimestamp())
                .build();
    }

    public RequestHitDto toRequestHitDto(Hit entity) {
        return RequestHitDto.builder()
                .app(entity.getApp())
                .uri(entity.getUri())
                .ip(entity.getIp())
                .timestamp(entity.getTimestamp())
                .build();
    }
}
