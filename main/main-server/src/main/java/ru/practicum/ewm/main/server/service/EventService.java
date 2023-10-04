package ru.practicum.ewm.main.server.service;

import ru.practicum.ewm.main.application.event.EventFullDto;
import ru.practicum.ewm.main.application.event.EventShortDto;
import ru.practicum.ewm.main.application.event.EventUpdateAdminRequest;
import ru.practicum.ewm.main.application.event.EventUpdateUserRequest;
import ru.practicum.ewm.main.application.event.NewEventDto;
import ru.practicum.ewm.main.application.event.RequestStatusUpdateRequest;
import ru.practicum.ewm.main.application.event.RequestStatusUpdateResult;
import ru.practicum.ewm.main.application.request.ParticipationRequestDto;
import ru.practicum.ewm.main.server.model.EventGetAllByAdminParameters;
import ru.practicum.ewm.main.server.model.EventGetAllParameters;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.List;

public interface EventService {

    EventFullDto create(NewEventDto newEventDto, long userId);

    EventFullDto getByIdByUser(long eventId, long userId);

    Collection<EventShortDto> getAllByUser(long userId, int from, int size);

    EventFullDto updateByUser(EventUpdateUserRequest eventUpdateUserRequest, long eventId, long userId);

    Collection<EventShortDto> getAll(EventGetAllParameters parameters);

    Collection<EventFullDto> getAllByAdmin(EventGetAllByAdminParameters parameters);

    EventFullDto getByIdByPublic(long eventId, HttpServletRequest httpRequest);

    EventFullDto updateByAdmin(EventUpdateAdminRequest eventUpdateAdminRequest, long eventId);

    List<ParticipationRequestDto> getRequests(Long eventId, Long userId);

    RequestStatusUpdateResult updateRequestStatus(RequestStatusUpdateRequest dto, Long eventId, Long userId);
}
