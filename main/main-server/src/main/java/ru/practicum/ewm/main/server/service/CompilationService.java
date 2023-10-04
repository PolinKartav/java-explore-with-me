package ru.practicum.ewm.main.server.service;

import ru.practicum.ewm.main.application.compilation.CompilationDto;
import ru.practicum.ewm.main.application.compilation.NewCompilationDto;
import ru.practicum.ewm.main.application.compilation.UpdateCompilationRequest;

import java.util.Collection;

public interface CompilationService {

    CompilationDto create(NewCompilationDto dto);

    void delete(Long compId);

    CompilationDto update(UpdateCompilationRequest dto, Long compId);

    Collection<CompilationDto> getAll(Boolean pinned, int from, int size);

    CompilationDto getById(Long compId);
}
