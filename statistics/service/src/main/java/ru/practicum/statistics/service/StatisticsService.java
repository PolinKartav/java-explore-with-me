package ru.practicum.statistics.service;

import ru.practicum.statistics.dto.RequestHitDto;
import ru.practicum.statistics.dto.ResponseHitDto;

import java.time.LocalDateTime;
import java.util.List;

public interface StatisticsService {

    RequestHitDto createHit(RequestHitDto hitDto);

    List<ResponseHitDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique);
}
