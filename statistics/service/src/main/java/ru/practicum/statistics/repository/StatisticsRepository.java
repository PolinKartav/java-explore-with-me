package ru.practicum.statistics.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.statistics.dto.ResponseHitDto;
import ru.practicum.statistics.entity.Hit;

import java.time.LocalDateTime;
import java.util.List;

public interface StatisticsRepository extends JpaRepository<Hit, Long> {

    @Query("select new ru.practicum.statistics.dto.ResponseHitDto(" +
            "   h.app, " +
            "   h.uri, " +
            "   case when :unique = true " +
            "       then count(distinct(h.ip)) " +
            "       else count(h.ip) " +
            "   end " +
            ") " +
            "from Hit h " +
            "where h.timestamp between :start and :end" +
            "   and (coalesce(:uris, null) is null or h.uri in :uris) " +
            "group by h.app, h.uri " +
            "order by 3 desc")
    List<ResponseHitDto> getStats(LocalDateTime start,
                                  LocalDateTime end,
                                  List<String> uris,
                                  Boolean unique);
}
