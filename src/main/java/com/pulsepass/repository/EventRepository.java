package com.pulsepass.repository;

import com.pulsepass.domain.Event;
import com.pulsepass.domain.EventStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {


    @EntityGraph(attributePaths = "venue")
    Optional<Event> findByEventCode(String eventCode);


    List<Event> findByStatusOrderByEventDateAsc(EventStatus status);


    List<Event> findByVenueCode(String venueCode);

    // ------------------------------------------------------------------
    // JPQL (consultas con JOIN, filtros y DISTINCT)
    // ------------------------------------------------------------------


    @Query("""
            select distinct e
            from Event e
            join e.artists a
            where a.stageName = :stageName
            order by e.eventDate asc
            """)
    List<Event> findByArtistStageName(@Param("stageName") String stageName);


    @Query("""
            select distinct e
            from Event e
            join e.venue v
            join e.artists a
            where v.city = :city
              and a.stageName = :stageName
            order by e.eventDate asc
            """)
    List<Event> findByCityAndArtistStageName(@Param("city") String city,
                                             @Param("stageName") String stageName);


    @Query("""
            select distinct e
            from Event e
            join e.venue v
            join e.artists a
            where e.status = com.pulsepass.domain.EventStatus.PUBLISHED
              and e.eventDate > :after
              and lower(v.city) = lower(:city)
              and lower(a.stageName) like lower(concat('%', :artistText, '%'))
            order by e.eventDate asc
            """)
    List<Event> findRecommended(@Param("after") LocalDateTime after,
                                @Param("city") String city,
                                @Param("artistText") String artistText);
}
