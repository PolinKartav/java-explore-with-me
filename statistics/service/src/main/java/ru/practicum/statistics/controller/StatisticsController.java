package ru.practicum.statistics.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.statistics.dto.RequestHitDto;
import ru.practicum.statistics.dto.ResponseHitDto;
import ru.practicum.statistics.service.StatisticsService;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static ru.practicum.util.constant.Constants.DATE_TIME_FORMAT;

@RestController
@RequiredArgsConstructor
@Validated
public class StatisticsController {
    private final StatisticsService service;

    @PostMapping("/hit")
    @ResponseStatus(HttpStatus.CREATED)
    public RequestHitDto createHit(@RequestBody @Valid RequestHitDto hitDto) {
        return service.createHit(hitDto);
    }

    @GetMapping("/stats")
    public Collection<ResponseHitDto> getStats(@RequestParam @DateTimeFormat(fallbackPatterns = DATE_TIME_FORMAT) LocalDateTime start,
                                               @RequestParam @DateTimeFormat(fallbackPatterns = DATE_TIME_FORMAT) LocalDateTime end,
                                               @RequestParam(required = false) List<String> uris,
                                               @RequestParam(defaultValue = "false") boolean unique) {
        return service.getStats(start, end, uris, unique);
    }
}
