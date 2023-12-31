package ru.practicum.ewm.main.application.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.practicum.ewm.main.application.category.CategoryDto;
import ru.practicum.ewm.main.application.user.UserShortDto;
import ru.practicum.util.constant.Constants;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class EventShortDto extends EventBaseDto {
    private Long id;
    private String title;
    private String annotation;
    private CategoryDto category;
    private UserShortDto initiator;
    private Boolean paid;
    private Long confirmedRequests;
    private Long views;

    @JsonFormat(pattern = Constants.DATE_TIME_FORMAT)
    private LocalDateTime eventDate;
}
