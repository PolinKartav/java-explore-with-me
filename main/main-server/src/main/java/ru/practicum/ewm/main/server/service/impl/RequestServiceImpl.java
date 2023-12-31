package ru.practicum.ewm.main.server.service.impl;


import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.main.application.event.EventStatus;
import ru.practicum.ewm.main.application.request.ParticipationRequestDto;
import ru.practicum.ewm.main.application.request.RequestStatus;
import ru.practicum.ewm.main.server.entity.*;
import ru.practicum.ewm.main.server.mapper.RequestMapper;
import ru.practicum.ewm.main.server.repository.EventRepository;
import ru.practicum.ewm.main.server.repository.RequestRepository;
import ru.practicum.ewm.main.server.repository.UserRepository;
import ru.practicum.ewm.main.server.service.RequestService;
import ru.practicum.ewm.main.util.exception.AlreadyExistedException;
import ru.practicum.ewm.main.util.exception.NotFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static ru.practicum.util.constant.Constants.SORT_BY_ID_ASC;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequestServiceImpl implements RequestService {
    private final RequestRepository requestRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final RequestMapper mapper;

    @Override
    @Transactional
    public ParticipationRequestDto create(long eventId, long userId) {
        User user = getUserById(userId);
        Event event = getEventById(eventId);

        if (!event.getState().equals(EventStatus.PUBLISHED)) {
            throw new AlreadyExistedException("Событие с таким id = " + eventId + " не является публичным");
        }
        if (event.getInitiator().getId().equals(userId)) {
            throw new AlreadyExistedException("Событие с таким id = " + eventId + " уже существует");
        }
        if (event.getParticipantLimit() > 0 && event.getParticipantLimit() - getConfirmedRequests(eventId) <= 0) {
            throw new AlreadyExistedException("Количество заявок на участие в мероприятии было ограничено.");
        }

        BooleanBuilder builder = new BooleanBuilder();
        builder.and(QRequest.request.event.id.eq(eventId));
        builder.and(QRequest.request.requester.id.eq(userId));
        if (requestRepository.count(builder) > 0) {
            throw new AlreadyExistedException("Запрос уже существует");
        }

        Request request = makeRequest(event, user);

        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            request.setStatus(RequestStatus.CONFIRMED);
        }

        eventRepository.save(event);

        return mapper.toParticipationRequestDto(requestRepository.save(request));
    }

    @Override
    public List<ParticipationRequestDto> getAll(long userId) {
        User user = getUserById(userId);

        BooleanBuilder builder = new BooleanBuilder();
        builder.and(QRequest.request.requester.eq(user));

        return StreamSupport.stream(requestRepository.findAll(builder, SORT_BY_ID_ASC).spliterator(), false)
                .map(mapper::toParticipationRequestDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancel(long requestId, long userId) {
        getUserById(userId);
        Request request = getRequestById(requestId);

        if (request.getRequester().getId() != userId) {
            throw new NotFoundException("Запроса с таким id = " + requestId + " не существует");
        }

        request.setStatus(RequestStatus.CANCELED);

        return mapper.toParticipationRequestDto(requestRepository.save(request));
    }

    @Override
    public List<ParticipationRequestDto> getRequests(Long eventId, Long userId) {
        getEventById(eventId);

        BooleanBuilder builder = new BooleanBuilder();
        builder.and(QRequest.request.event.id.eq(eventId));

        return StreamSupport.stream(requestRepository.findAll(builder, SORT_BY_ID_ASC).spliterator(), false)
                .map(mapper::toParticipationRequestDto)
                .collect(Collectors.toList());
    }

    private Request makeRequest(Event event, User user) {
        return Request.builder()
                .requester(user)
                .createDate(LocalDateTime.now())
                .event(event)
                .status(RequestStatus.PENDING)
                .build();
    }

    private User getUserById(long userId) {
        return userRepository.findById(userId).orElseThrow(
                () -> new NotFoundException("Пользователя с таким id = " + userId + " не существует")
        );
    }

    private Event getEventById(Long eventId) {
        return eventRepository.findById(eventId).orElseThrow(
                () -> new NotFoundException("События с таким id = " + eventId + " не существует")
        );
    }

    private Request getRequestById(Long requestId) {
        return requestRepository.findById(requestId).orElseThrow(
                () -> new NotFoundException("Запроса с таким id = " + requestId + " не существует")
        );
    }

    private Long getConfirmedRequests(Long eventId) {
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(QRequest.request.event.id.eq(eventId));
        builder.and(QRequest.request.status.eq(RequestStatus.CONFIRMED));

        return requestRepository.count(builder);
    }
}
