package com.pulsepass.repository;

import com.pulsepass.domain.Ticket;
import com.pulsepass.domain.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    Optional<Ticket> findByTicketCode(String ticketCode);

    List<Ticket> findByUserEmail(String email);

    List<Ticket> findByUserEmailIgnoreCaseOrderByPurchaseDateDesc(String email);

    List<Ticket> findByUserEmailAndStatus(String email, TicketStatus status);

    List<Ticket> findByEventEventCodeAndStatus(String eventCode, TicketStatus status);

    @Query("""
            select count(t)
            from Ticket t
            join t.event e
            where e.eventCode = :eventCode
              and t.status = com.pulsepass.domain.TicketStatus.PAID
            """)
    long countPaidByEventCode(@Param("eventCode") String eventCode);


    @Query("""
            select t
            from Ticket t
            join fetch t.event e
            where e.eventDate > :after
            order by e.eventDate asc, t.ticketCode asc
            """)
    List<Ticket> findByEventDateAfter(@Param("after") LocalDateTime after);
}
