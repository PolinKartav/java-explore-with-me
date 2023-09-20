package ru.practicum.util.constant;

import java.time.format.DateTimeFormatter;
import org.springframework.data.domain.Sort;

public class Constants {
    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);
    public static final Sort SORT_BY_ID_ASC = Sort.by(Sort.Direction.ASC, "id");
}
