package ru.practicum.ewm.main.server.service.impl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.main.application.event.EventFullDto;
import ru.practicum.ewm.main.application.event.EventShortDto;
import ru.practicum.ewm.main.application.event.EventSort;
import ru.practicum.ewm.main.application.event.EventStatus;
import ru.practicum.ewm.main.application.event.EventUpdateAdminRequest;
import ru.practicum.ewm.main.application.event.EventUpdateRequest;
import ru.practicum.ewm.main.application.event.EventUpdateUserRequest;
import ru.practicum.ewm.main.application.event.NewEventDto;
import ru.practicum.ewm.main.application.event.RequestStatusUpdateRequest;
import ru.practicum.ewm.main.application.event.RequestStatusUpdateResult;
import ru.practicum.ewm.main.application.event.StateAction;
import ru.practicum.ewm.main.application.request.ParticipationRequestDto;
import ru.practicum.ewm.main.application.request.RequestStatus;
import ru.practicum.ewm.main.server.entity.Category;
import ru.practicum.ewm.main.server.entity.Event;
import ru.practicum.ewm.main.server.entity.QEvent;
import ru.practicum.ewm.main.server.entity.QRequest;
import ru.practicum.ewm.main.server.entity.Request;
import ru.practicum.ewm.main.server.entity.User;
import ru.practicum.ewm.main.server.mapper.EventMapper;
import ru.practicum.ewm.main.server.mapper.LocationMapper;
import ru.practicum.ewm.main.server.mapper.RequestMapper;
import ru.practicum.ewm.main.server.model.EventGetAllByAdminParameters;
import ru.practicum.ewm.main.server.model.EventGetAllParameters;
import ru.practicum.ewm.main.server.repository.CategoryRepository;
import ru.practicum.ewm.main.server.repository.EventRepository;
import ru.practicum.ewm.main.server.repository.RequestRepository;
import ru.practicum.ewm.main.server.repository.UserRepository;
import ru.practicum.ewm.main.server.service.EventService;
import ru.practicum.ewm.main.util.exception.AlreadyExistedException;
import ru.practicum.ewm.main.util.exception.NotFoundException;
import ru.practicum.statistics.client.StatisticsClient;
import ru.practicum.statistics.dto.ResponseHitDto;
import ru.practicum.util.pageable.OffsetBasedPageRequest;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static ru.practicum.util.constant.Constants.SORT_BY_ID_ASC;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final RequestRepository requestRepository;
    private final EventMapper eventMapper;
    private final LocationMapper locationMapper;
    private final RequestMapper requestMapper;
    private final StatisticsClient statisticsClient;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public EventFullDto create(NewEventDto newEventDto, long userId) {
        User user = getUserById(userId);
        Category category = getCategoryById(newEventDto.getCategory());

        Event event = eventMapper.toEvent(newEventDto, category, user);

        event.setInitiator(user);
        event.setCreatedOn(LocalDateTime.now());
        event.setConfirmedRequests(0L);
        event.setState(EventStatus.PENDING);

        return eventMapper.toEventFullDto(eventRepository.save(event));
    }

    @Override
    public EventFullDto getByIdByUser(long eventId, long userId) {
        Event event = getEventById(eventId);

        if (event.getInitiator().getId().equals(userId)) {
            return eventMapper.toEventFullDto(event);
        }

        throw new NotFoundException("События с id = " + eventId + " не существует");
    }

    @Override
    public Collection<EventShortDto> getAllByUser(long userId, int from, int size) {
        getUserById(userId);

        BooleanBuilder builder = new BooleanBuilder();
        builder.and(QEvent.event.initiator.id.eq(userId));

        Pageable pageable = new OffsetBasedPageRequest(from, size, SORT_BY_ID_ASC);

        return eventRepository.findAll(builder, pageable)
                .getContent()
                .stream()
                .map(eventMapper::toEventShortDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto updateByUser(EventUpdateUserRequest dto, long eventId, long userId) {
        getUserById(userId);

        Event event = getEventById(eventId);

        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("События с id = " + eventId + " не существует");
        }

        if (event.getState().equals(EventStatus.PUBLISHED)) {
            throw new AlreadyExistedException("Не удается обновить событие с публичным статусом");
        }

        patchEvent(event, dto);

        if (dto.getStateAction() != null
                && dto.getStateAction().equals(StateAction.CANCEL_REVIEW)) {
            event.setState(EventStatus.CANCELED);
        }
        if (dto.getStateAction() != null
                && dto.getStateAction().equals(StateAction.SEND_TO_REVIEW)) {
            event.setState(EventStatus.PENDING);
        }

        return eventMapper.toEventFullDto(eventRepository.save(event));
    }

    @Override
    public Collection<EventShortDto> getAll(EventGetAllParameters parameters) {
        String text = parameters.getText();
        List<Long> categories = parameters.getCategories();
        Boolean paid = parameters.getPaid();
        LocalDateTime start = parameters.getStart();
        LocalDateTime end = parameters.getEnd();
        Boolean onlyAvailable = parameters.getOnlyAvailable();
        EventSort eventSort = parameters.getEventSort();
        int from = parameters.getFrom();
        int size = parameters.getSize();
        HttpServletRequest httpServletRequest = parameters.getHttpServletRequest();

        BooleanBuilder builder = makeBuilder(Collections.emptyList(),
                categories,
                Collections.emptyList(),
                start,
                end,
                text,
                onlyAvailable,
                paid);

        Pageable pageable;

        if (eventSort.equals(EventSort.EVENT_DATE)) {
            pageable = new OffsetBasedPageRequest(from, size, Sort.by(Sort.Direction.ASC, "eventDate"));
        } else {
            pageable = new OffsetBasedPageRequest(from, size, Sort.by(Sort.Direction.DESC, "views"));
        }

        statisticsClient.postHit(httpServletRequest);

        Collection<EventShortDto> dtos = eventRepository.findAll(builder, pageable)
                .getContent()
                .stream()
                .map(eventMapper::toEventShortDto)
                .collect(Collectors.toList());

        if (!dtos.isEmpty()) {
            Map<Long, Long> views = getViews(dtos);

            dtos.forEach(dto -> dto.setViews(views.get(dto.getId())));
        }

        return dtos;
    }

    @Override
    public Collection<EventFullDto> getAllByAdmin(EventGetAllByAdminParameters parameters) {
        List<Long> users = parameters.getUsers();
        List<EventStatus> states = parameters.getStates();
        List<Long> categories = parameters.getCategories();
        LocalDateTime start = parameters.getStart();
        LocalDateTime end = parameters.getEnd();
        int from = parameters.getFrom();
        int size = parameters.getSize();

        BooleanBuilder builder = makeBuilder(users,
                categories,
                states,
                start,
                end,
                null,
                null,
                null);

        Pageable pageable = new OffsetBasedPageRequest(from, size, SORT_BY_ID_ASC);

        return eventRepository.findAll(builder, pageable)
                .getContent()
                .stream()
                .map(eventMapper::toEventFullDto)
                .collect(Collectors.toList());
    }

    @Override
    public EventFullDto getByIdByPublic(long eventId, HttpServletRequest httpServletRequest) {
        Event event = getEventById(eventId);

        if (event.getState() != EventStatus.PUBLISHED) {
            throw new NotFoundException("Событие с  id = " + eventId + " является публичным");
        }

        statisticsClient.postHit(httpServletRequest);

        EventFullDto dto = eventMapper.toEventFullDto(event);
        dto.setViews((long) statisticsClient.getStats(event.getCreatedOn(),
                        LocalDateTime.now(),
                        List.of("/events/" + eventId),
                        true)
                .size());

        return dto;
    }

    @Override
    @Transactional
    public EventFullDto updateByAdmin(EventUpdateAdminRequest dto, long eventId) {
        Event event = getEventById(eventId);

        if (dto.getStateAction() != null
                && dto.getStateAction().equals(StateAction.PUBLISH_EVENT)
                && !event.getState().equals(EventStatus.PENDING)) {
            throw new AlreadyExistedException("Событие с id = " + eventId + " не находится на рассмотрении");
        }

        if (dto.getStateAction() != null
                && dto.getStateAction().equals(StateAction.REJECT_EVENT)
                && event.getState().equals(EventStatus.PUBLISHED)) {
            throw new AlreadyExistedException("Событие с id = " + eventId + " является публичным");
        }

        patchEvent(event, dto);

        if (dto.getStateAction() != null
                && dto.getStateAction().equals(StateAction.PUBLISH_EVENT)) {
            event.setState(EventStatus.PUBLISHED);
        }

        if (dto.getStateAction() != null
                && dto.getStateAction().equals(StateAction.REJECT_EVENT)) {
            event.setState(EventStatus.CANCELED);
        }

        return eventMapper.toEventFullDto(eventRepository.save(event));
    }

    @Override
    public List<ParticipationRequestDto> getRequests(Long eventId, Long userId) {
        getEventById(eventId);

        BooleanBuilder builder = new BooleanBuilder();
        builder.and(QRequest.request.event.id.eq(eventId));

        return StreamSupport.stream(requestRepository.findAll(builder, SORT_BY_ID_ASC).spliterator(), false)
                .map(requestMapper::toParticipationRequestDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RequestStatusUpdateResult updateRequestStatus(RequestStatusUpdateRequest dto, Long eventId, Long userId) {
        getUserById(userId);
        Event event = getEventById(eventId);
        RequestStatus status = dto.getStatus();

        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Событие с id = " + eventId + " может быть обновлен только инициатором");
        }
        if (event.getParticipantLimit() == 0) {
            throw new AlreadyExistedException("Событие не имеет ограничения по количеству участников.");
        }
        if (!event.getRequestModeration()) {
            throw new AlreadyExistedException("Событие не модерируется.");
        }
        if (status.equals(RequestStatus.CONFIRMED) &&
                event.getParticipantLimit() - event.getConfirmedRequests() <= 0) {
            throw new AlreadyExistedException("Количество заявок на участие в мероприятии достигло предела");
        }

        BooleanBuilder builder = new BooleanBuilder();
        builder.and(QRequest.request.id.in(dto.getRequestIds()));
        builder.and(QRequest.request.event.id.eq(eventId));

        List<Request> requests = StreamSupport
                .stream(requestRepository.findAll(builder, SORT_BY_ID_ASC).spliterator(), false)
                .collect(Collectors.toList());

        switch (status) {
            case CONFIRMED:
                requests.forEach(r -> {
                    if (!r.getStatus().equals(RequestStatus.PENDING)) {
                        throw new AlreadyExistedException("Запрос с id = " + r.getId() + " не находится на рассмотрении");
                    }

                    if (event.getParticipantLimit() - event.getConfirmedRequests() <= 0) {
                        r.setStatus(RequestStatus.REJECTED);
                    } else {
                        event.setConfirmedRequests(event.getConfirmedRequests() + 1);
                        r.setStatus(status);
                    }
                });
                break;
            case REJECTED:
                requests.forEach(r -> {
                    if (!r.getStatus().equals(RequestStatus.PENDING)) {
                        throw new AlreadyExistedException("Запрос с id = " + r.getId() + " не находится на рассмотрении");
                    }
                    r.setStatus(status);
                });
        }


        eventRepository.save(event);

        return requestMapper.toRequestStatusUpdateResult(requests);
    }

    private User getUserById(long userId) {
        return userRepository.findById(userId).orElseThrow(
                () -> new NotFoundException("Пользователя с id = " + userId + " не существует")
        );
    }

    private Category getCategoryById(Long categoryId) {
        return categoryRepository.findById(categoryId).orElseThrow(
                () -> new NotFoundException("Категории с id = " + categoryId + " не существует")
        );
    }

    private Event getEventById(Long eventId) {
        return eventRepository.findById(eventId).orElseThrow(
                () -> new NotFoundException("Событие с id = " + eventId + " не существует")
        );
    }

    private Event patchEvent(Event event, EventUpdateRequest dto) {
        if (dto.getTitle() != null && !dto.getTitle().isBlank()) {
            event.setTitle(dto.getTitle());
        }
        if (dto.getAnnotation() != null && !dto.getAnnotation().isBlank()) {
            event.setAnnotation(dto.getAnnotation());
        }
        if (dto.getCategory() != null) {
            event.setCategory(getCategoryById(dto.getCategory()));
        }
        if (dto.getDescription() != null && !dto.getDescription().isBlank()) {
            event.setDescription(dto.getDescription());
        }
        if (dto.getLocation() != null && dto.getLocation().getLat() != null && dto.getLocation().getLon() != null) {
            event.setLocation(locationMapper.toLocation(dto.getLocation()));
        }
        if (dto.getPaid() != null) {
            event.setPaid(dto.getPaid());
        }
        if (dto.getRequestModeration() != null) {
            event.setRequestModeration(dto.getRequestModeration());
        }
        if (dto.getParticipantLimit() != null) {
            event.setParticipantLimit(dto.getParticipantLimit());
        }
        if (dto.getEventDate() != null) {
            event.setEventDate(dto.getEventDate());
        }

        return event;
    }

    private BooleanBuilder makeBuilder(List<Long> users,
                                       List<Long> categories,
                                       List<EventStatus> states,
                                       LocalDateTime start,
                                       LocalDateTime end,
                                       String text,
                                       Boolean onlyAvailable,
                                       Boolean paid) {
        BooleanBuilder builder = new BooleanBuilder();
        QEvent qEvent = QEvent.event;

        if (users != null && !users.isEmpty()) {
            builder.and(qEvent.initiator.id.in(users));
        }

        if (categories != null && !categories.isEmpty()) {
            builder.and(qEvent.category.id.in(categories));
        }

        if (states != null && !states.isEmpty()) {
            builder.and(qEvent.state.in(states));
        }

        if (text != null && !text.isBlank()) {
            builder.and(qEvent.annotation.likeIgnoreCase(text))
                    .or(qEvent.description.likeIgnoreCase(text));
        }

        if (onlyAvailable != null && onlyAvailable) {
            builder.and((QEvent.event.participantLimit.subtract(QEvent.event.confirmedRequests)).loe(1));
        }

        if (paid != null) {
            builder.and(QEvent.event.paid.eq(paid));
        }

        if (start != null) {
            builder.and(qEvent.eventDate.after(start));
        }

        if (end != null) {
            builder.and(qEvent.eventDate.before(end));
        }

        return builder;
    }

    private Map<Long, Long> getViews(Collection<EventShortDto> dtos) {
        List<String> uris = dtos.stream()
                .map(dto -> "/events/" + dto.getId())
                .collect(Collectors.toList());

        List<Long> ids = dtos.stream()
                .map(EventShortDto::getId)
                .collect(Collectors.toList());

        JPAQueryFactory query = new JPAQueryFactory(entityManager);
        QEvent qEvent = QEvent.event;

        Event event = query
               .selectFrom(qEvent)
                .where(qEvent.id.in(ids))
                .orderBy(qEvent.createdOn.asc())
                .fetchFirst();

        LocalDateTime startTime = event.getCreatedOn();

        List<ResponseHitDto> hits = statisticsClient.getStats(startTime,
                LocalDateTime.now(),
                uris,
                true);

        Map<Long, Long> views = new HashMap<>();

        hits.forEach(hit -> {
            String uri = hit.getUri();
            String[] split = uri.split("/");
            String id = split[2];
            Long eventId = Long.parseLong(id);
            views.put(eventId, hit.getHits());
        });

        return views;
    }
}