package ru.practicum.ewm.main.server.service;

import ru.practicum.ewm.main.application.event.*;
import ru.practicum.ewm.main.server.model.EventGetAllByAdminParameters;
import ru.practicum.ewm.main.server.model.EventGetAllParameters;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;

public interface EventService {

    EventFullDto create(NewEventDto newEventDto, long userId);

    EventFullDto getByIdByUser(long eventId, long userId);

    Collection<EventShortDto> getAllByUser(long userId, int from, int size);

    EventFullDto updateByUser(EventUpdateUserRequest eventUpdateUserRequest, long eventId, long userId);

    Collection<EventShortDto> getAll(EventGetAllParameters parameters);

    Collection<EventFullDto> getAllByAdmin(EventGetAllByAdminParameters parameters);

    EventFullDto getByIdByPublic(long eventId, HttpServletRequest httpRequest);

    EventFullDto updateByAdmin(EventUpdateAdminRequest eventUpdateAdminRequest, long eventId);

    RequestStatusUpdateResult updateRequestStatus(RequestStatusUpdateRequest dto, Long eventId, Long userId);
}
