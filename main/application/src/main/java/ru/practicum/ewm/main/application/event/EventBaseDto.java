package ru.practicum.ewm.main.application.event;

import lombok.*;
import ru.practicum.ewm.main.application.category.CategoryDto;
import ru.practicum.ewm.main.application.user.UserShortDto;

import java.time.LocalDateTime;

@Getter
@Setter
public class EventBaseDto {

    private Long id;
    private String title;
    private String annotation;
    private CategoryDto category;
    private UserShortDto initiator;
    private Boolean paid;
    private Long confirmedRequests;
    private Long views;
    private LocalDateTime eventDate;
}
