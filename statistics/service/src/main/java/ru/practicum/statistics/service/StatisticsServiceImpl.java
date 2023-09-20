package ru.practicum.statistics.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.practicum.statistics.dto.RequestHitDto;
import ru.practicum.statistics.dto.ResponseHitDto;
import ru.practicum.statistics.mapper.HitMapper;
import ru.practicum.statistics.repository.StatisticsRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticsServiceImpl implements StatisticsService {
    private final StatisticsRepository repository;

    @Override
    @Transactional
    public RequestHitDto createHit(RequestHitDto hitDto) {
        return  HitMapper.toRequestHitDto(repository.save(HitMapper.toHitEntityFromRequestHitDto(hitDto)));
    }

    @Override
    public List<ResponseHitDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {
        if (start.isAfter(end)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End cannot be early then start");
        }

        return repository.getStats(start, end, uris, unique);
    }
}
